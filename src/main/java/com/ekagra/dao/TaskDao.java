package com.ekagra.dao;

import com.ekagra.model.Priority;
import com.ekagra.model.SubTask;
import com.ekagra.model.Task;
import com.ekagra.model.TaskStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskDao extends BaseDao implements Dao<Task> {

    @Override
    public Optional<Task> findById(int id) {
        Optional<Task> task = queryOne("SELECT * FROM tasks WHERE id = ?", this::map, id);
        task.ifPresent(t -> query("SELECT * FROM subtasks WHERE task_id = ? ORDER BY id", this::mapSub, id)
            .forEach(t::addSubTask));
        return task;
    }

    /** All tasks of a user with their subtasks (2 queries, not N+1). */
    public List<Task> findByUser(int userId) {
        List<Task> tasks = query("SELECT * FROM tasks WHERE user_id = ? ORDER BY created_at DESC", this::map, userId);
        Map<Integer, Task> byId = tasks.stream().collect(Collectors.toMap(Task::getId, Function.identity()));
        query("""
            SELECT s.* FROM subtasks s JOIN tasks t ON t.id = s.task_id
            WHERE t.user_id = ? ORDER BY s.id""", this::mapSub, userId)
            .forEach(s -> {
                Task t = byId.get(s.getTaskId());
                if (t != null) {
                    t.addSubTask(s);
                }
            });
        return tasks;
    }

    @Override
    public int insert(Task t) {
        int id = insert("""
            INSERT INTO tasks (user_id, title, description, subject, priority, status, deadline,
                               estimated_min, spent_min, created_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            t.getUserId(), t.getTitle(), t.getDescription(), t.getSubject(), t.getPriority(), t.getStatus(),
            t.getDeadline(), t.getEstimatedMin(), t.getSpentMin(), t.getCreatedAt(), t.getCompletedAt());
        t.setId(id);
        saveSubtasks(t);
        return id;
    }

    @Override
    public void update(Task t) {
        update("""
            UPDATE tasks SET title = ?, description = ?, subject = ?, priority = ?, status = ?, deadline = ?,
                             estimated_min = ?, spent_min = ?, completed_at = ?
            WHERE id = ?""",
            t.getTitle(), t.getDescription(), t.getSubject(), t.getPriority(), t.getStatus(), t.getDeadline(),
            t.getEstimatedMin(), t.getSpentMin(), t.getCompletedAt(), t.getId());
        update("DELETE FROM subtasks WHERE task_id = ?", t.getId());
        saveSubtasks(t);
    }

    public void updateStatus(Task t) {
        update("UPDATE tasks SET status = ?, completed_at = ? WHERE id = ?",
            t.getStatus(), t.getCompletedAt(), t.getId());
    }

    public void setSubtaskDone(int subtaskId, boolean done) {
        update("UPDATE subtasks SET done = ? WHERE id = ?", done, subtaskId);
    }

    /** Adds focus time to a task and moves it from TODO to IN_PROGRESS. */
    public void addSpentMinutes(int taskId, int minutes) {
        update("""
            UPDATE tasks SET spent_min = spent_min + ?,
                status = CASE WHEN status = 'TODO' THEN 'IN_PROGRESS' ELSE status END
            WHERE id = ?""", minutes, taskId);
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM tasks WHERE id = ?", id);
    }

    public int countCompletedBetween(int userId, LocalDateTime from, LocalDateTime to) {
        return (int) queryLong("SELECT COUNT(*) FROM tasks WHERE user_id = ? AND status = 'DONE' "
            + "AND completed_at >= ? AND completed_at < ?", userId, from, to);
    }

    public int countCompleted(int userId) {
        return (int) queryLong("SELECT COUNT(*) FROM tasks WHERE user_id = ? AND status = 'DONE'", userId);
    }

    /** Tasks finished on or before their deadline. */
    public int countCompletedOnTime(int userId) {
        return (int) queryLong("SELECT COUNT(*) FROM tasks WHERE user_id = ? AND status = 'DONE' "
            + "AND deadline IS NOT NULL AND completed_at <= deadline", userId);
    }

    private void saveSubtasks(Task t) {
        for (SubTask s : t.getSubtasks()) {
            s.setTaskId(t.getId());
            s.setId(insert("INSERT INTO subtasks (task_id, title, done) VALUES (?, ?, ?)",
                t.getId(), s.getTitle(), s.isDone()));
        }
    }

    private Task map(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setSubject(rs.getString("subject"));
        t.setPriority(Priority.valueOf(rs.getString("priority")));
        t.setStatus(TaskStatus.valueOf(rs.getString("status")));
        t.setDeadline(toDateTime(rs.getString("deadline")));
        t.setEstimatedMin(rs.getInt("estimated_min"));
        t.setSpentMin(rs.getInt("spent_min"));
        t.setCreatedAt(toDateTime(rs.getString("created_at")));
        t.setCompletedAt(toDateTime(rs.getString("completed_at")));
        return t;
    }

    private SubTask mapSub(ResultSet rs) throws SQLException {
        SubTask s = new SubTask(rs.getString("title"), rs.getInt("done") == 1);
        s.setId(rs.getInt("id"));
        s.setTaskId(rs.getInt("task_id"));
        return s;
    }
}
