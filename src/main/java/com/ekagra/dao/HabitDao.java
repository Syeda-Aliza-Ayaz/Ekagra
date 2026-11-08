package com.ekagra.dao;

import com.ekagra.model.Habit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HabitDao extends BaseDao implements Dao<Habit> {

    @Override
    public Optional<Habit> findById(int id) {
        Optional<Habit> h = queryOne("SELECT * FROM habits WHERE id = ?", this::map, id);
        h.ifPresent(habit -> query("SELECT date FROM habit_checks WHERE habit_id = ?", rs -> toDate(rs.getString(1)), id)
            .forEach(habit::addCheck));
        return h;
    }

    /** Active habits with all their check-ins (2 queries). */
    public List<Habit> findActive(int userId) {
        List<Habit> habits = query("SELECT * FROM habits WHERE user_id = ? AND active = 1 ORDER BY created_at",
            this::map, userId);
        Map<Integer, Habit> byId = habits.stream().collect(Collectors.toMap(Habit::getId, Function.identity()));
        query("SELECT c.habit_id, c.date FROM habit_checks c JOIN habits h ON h.id = c.habit_id WHERE h.user_id = ?",
            rs -> {
                Habit h = byId.get(rs.getInt(1));
                if (h != null) {
                    h.addCheck(toDate(rs.getString(2)));
                }
                return null;
            }, userId);
        return habits;
    }

    public void check(int habitId, LocalDate day) {
        update("INSERT OR IGNORE INTO habit_checks (habit_id, date) VALUES (?, ?)", habitId, day);
    }

    public void uncheck(int habitId, LocalDate day) {
        update("DELETE FROM habit_checks WHERE habit_id = ? AND date = ?", habitId, day);
    }

    @Override
    public int insert(Habit h) {
        int id = insert("INSERT INTO habits (user_id, title, icon, active, created_at) VALUES (?, ?, ?, ?, ?)",
            h.getUserId(), h.getTitle(), h.getIcon(), h.isActive(), h.getCreatedAt());
        h.setId(id);
        return id;
    }

    @Override
    public void update(Habit h) {
        update("UPDATE habits SET title = ?, icon = ?, active = ? WHERE id = ?", h.getTitle(), h.getIcon(), h.isActive(), h.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM habits WHERE id = ?", id);
    }

    private Habit map(ResultSet rs) throws SQLException {
        Habit h = new Habit(rs.getInt("user_id"), rs.getString("title"), rs.getString("icon"));
        h.setId(rs.getInt("id"));
        h.setActive(rs.getInt("active") == 1);
        h.setCreatedAt(toDateTime(rs.getString("created_at")));
        return h;
    }
}
