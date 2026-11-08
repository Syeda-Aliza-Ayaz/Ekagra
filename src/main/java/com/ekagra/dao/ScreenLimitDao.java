package com.ekagra.dao;

import com.ekagra.model.ScreenLimit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Per-app daily limits. (The overall daily limit is a user setting.) */
public class ScreenLimitDao extends BaseDao implements Dao<ScreenLimit> {

    @Override
    public Optional<ScreenLimit> findById(int id) {
        return queryOne("SELECT * FROM screen_limits WHERE id = ?", this::map, id);
    }

    public List<ScreenLimit> findByUser(int userId) {
        return query("SELECT * FROM screen_limits WHERE user_id = ? AND app_name IS NOT NULL ORDER BY app_name",
            this::map, userId);
    }

    @Override
    public int insert(ScreenLimit l) {
        int id = insert("INSERT INTO screen_limits (user_id, app_name, limit_min) VALUES (?, ?, ?)",
            l.getUserId(), l.getAppName(), l.getLimitMin());
        l.setId(id);
        return id;
    }

    @Override
    public void update(ScreenLimit l) {
        update("UPDATE screen_limits SET app_name = ?, limit_min = ? WHERE id = ?", l.getAppName(), l.getLimitMin(), l.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM screen_limits WHERE id = ?", id);
    }

    private ScreenLimit map(ResultSet rs) throws SQLException {
        ScreenLimit l = new ScreenLimit(rs.getInt("user_id"), rs.getString("app_name"), rs.getInt("limit_min"));
        l.setId(rs.getInt("id"));
        return l;
    }
}
