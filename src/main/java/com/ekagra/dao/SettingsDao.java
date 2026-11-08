package com.ekagra.dao;

import java.util.Optional;

/** Simple per-user key/value store for preferences. */
public class SettingsDao extends BaseDao {

    public Optional<String> get(int userId, String key) {
        return queryOne("SELECT value FROM settings WHERE user_id = ? AND key = ?",
            rs -> rs.getString(1), userId, key);
    }

    public void set(int userId, String key, String value) {
        update("""
            INSERT INTO settings (user_id, key, value) VALUES (?, ?, ?)
            ON CONFLICT (user_id, key) DO UPDATE SET value = excluded.value""",
            userId, key, value);
    }
}
