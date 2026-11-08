package com.ekagra.dao;

import com.ekagra.model.Goal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GoalDao extends BaseDao implements Dao<Goal> {

    @Override
    public Optional<Goal> findById(int id) {
        return queryOne("SELECT * FROM goals WHERE id = ?", this::map, id);
    }

    public List<Goal> findActive(int userId) {
        return query("SELECT * FROM goals WHERE user_id = ? AND active = 1 ORDER BY period, created_at", this::map, userId);
    }

    public int countAll(int userId) {
        return (int) queryLong("SELECT COUNT(*) FROM goals WHERE user_id = ?", userId);
    }

    @Override
    public int insert(Goal g) {
        int id = insert("""
            INSERT INTO goals (user_id, title, type, period, target_value, active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""",
            g.getUserId(), g.getTitle(), g.getType(), g.getPeriod(), g.getTargetValue(), g.isActive(), g.getCreatedAt());
        g.setId(id);
        return id;
    }

    @Override
    public void update(Goal g) {
        update("UPDATE goals SET title = ?, type = ?, period = ?, target_value = ?, active = ? WHERE id = ?",
            g.getTitle(), g.getType(), g.getPeriod(), g.getTargetValue(), g.isActive(), g.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM goals WHERE id = ?", id);
    }

    private Goal map(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setUserId(rs.getInt("user_id"));
        g.setTitle(rs.getString("title"));
        g.setType(Goal.Type.valueOf(rs.getString("type")));
        g.setPeriod(Goal.Period.valueOf(rs.getString("period")));
        g.setTargetValue(rs.getInt("target_value"));
        g.setActive(rs.getInt("active") == 1);
        g.setCreatedAt(toDateTime(rs.getString("created_at")));
        return g;
    }
}
