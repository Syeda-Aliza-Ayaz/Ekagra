package com.ekagra.dao;

import com.ekagra.model.FocusSession;
import com.ekagra.model.SessionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SessionDao extends BaseDao implements Dao<FocusSession> {

    private static final String SELECT =
        "SELECT s.*, t.title AS task_title FROM focus_sessions s LEFT JOIN tasks t ON t.id = s.task_id ";

    /** Aggregated focus numbers for a time range. */
    public record FocusStats(int focusSeconds, int completedSessions, int distractions) {
        public int focusMinutes() {
            return focusSeconds / 60;
        }
    }

    @Override
    public Optional<FocusSession> findById(int id) {
        return queryOne(SELECT + "WHERE s.id = ?", this::map, id);
    }

    public List<FocusSession> findRecent(int userId, int limit) {
        return query(SELECT + "WHERE s.user_id = ? ORDER BY s.started_at DESC LIMIT ?", this::map, userId, limit);
    }

    public List<FocusSession> findBetween(int userId, LocalDateTime from, LocalDateTime to) {
        return query(SELECT + "WHERE s.user_id = ? AND s.started_at >= ? AND s.started_at < ? ORDER BY s.started_at",
            this::map, userId, from, to);
    }

    public FocusStats focusStats(int userId, LocalDateTime from, LocalDateTime to) {
        return queryOne("""
            SELECT COALESCE(SUM(actual_sec), 0), COALESCE(SUM(completed), 0), COALESCE(SUM(distractions), 0)
            FROM focus_sessions
            WHERE user_id = ? AND type = 'FOCUS' AND started_at >= ? AND started_at < ?""",
            rs -> new FocusStats(rs.getInt(1), rs.getInt(2), rs.getInt(3)), userId, from, to)
            .orElse(new FocusStats(0, 0, 0));
    }

    public int countCompletedFocus(int userId) {
        return (int) queryLong("SELECT COUNT(*) FROM focus_sessions WHERE user_id = ? AND type = 'FOCUS' AND completed = 1",
            userId);
    }

    /** Completed focus sessions that started before {@code hour} o'clock (e.g. 8 for "Early Bird"). */
    public int countCompletedFocusStartedBefore(int userId, int hour) {
        return (int) queryLong("SELECT COUNT(*) FROM focus_sessions WHERE user_id = ? AND type = 'FOCUS' "
            + "AND completed = 1 AND CAST(substr(started_at, 12, 2) AS INTEGER) < ?", userId, hour);
    }

    /** Completed focus sessions that ended at or after {@code hour} o'clock (e.g. 23 for "Night Owl"). */
    public int countCompletedFocusEndedFrom(int userId, int hour) {
        return (int) queryLong("SELECT COUNT(*) FROM focus_sessions WHERE user_id = ? AND type = 'FOCUS' "
            + "AND completed = 1 AND CAST(substr(ended_at, 12, 2) AS INTEGER) >= ?", userId, hour);
    }

    @Override
    public int insert(FocusSession s) {
        int id = insert("""
            INSERT INTO focus_sessions (user_id, task_id, type, started_at, ended_at, planned_min,
                                        actual_sec, completed, distractions, focus_rating)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            s.getUserId(), s.getTaskId(), s.getType(), s.getStartedAt(), s.getEndedAt(), s.getPlannedMin(),
            s.getActualSec(), s.isCompleted(), s.getDistractions(), s.getFocusRating());
        s.setId(id);
        return id;
    }

    @Override
    public void update(FocusSession s) {
        update("""
            UPDATE focus_sessions SET ended_at = ?, actual_sec = ?, completed = ?, distractions = ?, focus_rating = ?
            WHERE id = ?""",
            s.getEndedAt(), s.getActualSec(), s.isCompleted(), s.getDistractions(), s.getFocusRating(), s.getId());
    }

    public void setRating(int sessionId, int rating) {
        update("UPDATE focus_sessions SET focus_rating = ? WHERE id = ?", rating, sessionId);
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM focus_sessions WHERE id = ?", id);
    }

    private FocusSession map(ResultSet rs) throws SQLException {
        FocusSession s = new FocusSession();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setTaskId(nullableInt(rs, "task_id"));
        s.setTaskTitle(rs.getString("task_title"));
        s.setType(SessionType.valueOf(rs.getString("type")));
        s.setStartedAt(toDateTime(rs.getString("started_at")));
        s.setEndedAt(toDateTime(rs.getString("ended_at")));
        s.setPlannedMin(rs.getInt("planned_min"));
        s.setActualSec(rs.getInt("actual_sec"));
        s.setCompleted(rs.getInt("completed") == 1);
        s.setDistractions(rs.getInt("distractions"));
        s.setFocusRating(nullableInt(rs, "focus_rating"));
        return s;
    }
}
