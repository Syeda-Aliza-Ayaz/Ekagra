package com.ekagra.dao;

import com.ekagra.model.Badge;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadgeDao extends BaseDao {

    public List<Badge> findAll() {
        return query("SELECT * FROM badges ORDER BY rowid", rs -> new Badge(rs.getString("code"), rs.getString("name"),
            rs.getString("description"), rs.getString("icon")));
    }

    /** Badge code -> when it was earned. */
    public Map<String, LocalDateTime> findEarned(int userId) {
        Map<String, LocalDateTime> out = new HashMap<>();
        query("SELECT badge_code, earned_at FROM user_badges WHERE user_id = ?", rs -> {
            out.put(rs.getString(1), toDateTime(rs.getString(2)));
            return null;
        }, userId);
        return out;
    }

    /** Returns true only the first time the badge is given. */
    public boolean award(int userId, String code) {
        return update("INSERT OR IGNORE INTO user_badges (user_id, badge_code, earned_at) VALUES (?, ?, ?)",
            userId, code, LocalDateTime.now()) == 1;
    }
}
