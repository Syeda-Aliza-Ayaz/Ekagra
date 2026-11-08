package com.ekagra.dao;

import com.ekagra.model.SleepEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class SleepDao extends BaseDao implements Dao<SleepEntry> {

    @Override
    public Optional<SleepEntry> findById(int id) {
        return queryOne("SELECT * FROM sleep_entries WHERE id = ?", this::map, id);
    }

    public Optional<SleepEntry> findByDate(int userId, LocalDate date) {
        return queryOne("SELECT * FROM sleep_entries WHERE user_id = ? AND date = ?", this::map, userId, date);
    }

    public List<SleepEntry> findBetween(int userId, LocalDate from, LocalDate toInclusive) {
        return query("SELECT * FROM sleep_entries WHERE user_id = ? AND date >= ? AND date <= ? ORDER BY date",
            this::map, userId, from, toInclusive);
    }

    public void upsert(SleepEntry s) {
        update("""
            INSERT INTO sleep_entries (user_id, date, bedtime, wake_time, duration_min) VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (user_id, date) DO UPDATE SET bedtime = excluded.bedtime, wake_time = excluded.wake_time,
                duration_min = excluded.duration_min""",
            s.getUserId(), s.getDate(), s.getBedtime(), s.getWakeTime(), s.getDurationMin());
    }

    @Override
    public int insert(SleepEntry s) {
        upsert(s);
        return findByDate(s.getUserId(), s.getDate()).map(SleepEntry::getId).orElse(-1);
    }

    @Override
    public void update(SleepEntry s) {
        upsert(s);
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM sleep_entries WHERE id = ?", id);
    }

    private SleepEntry map(ResultSet rs) throws SQLException {
        SleepEntry s = new SleepEntry(rs.getInt("user_id"), toDate(rs.getString("date")),
            LocalTime.parse(rs.getString("bedtime")), LocalTime.parse(rs.getString("wake_time")));
        s.setId(rs.getInt("id"));
        return s;
    }
}
