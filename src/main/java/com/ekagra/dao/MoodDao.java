package com.ekagra.dao;

import com.ekagra.model.MoodEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MoodDao extends BaseDao implements Dao<MoodEntry> {

    @Override
    public Optional<MoodEntry> findById(int id) {
        return queryOne("SELECT * FROM mood_entries WHERE id = ?", this::map, id);
    }

    public Optional<MoodEntry> findByDate(int userId, LocalDate date) {
        return queryOne("SELECT * FROM mood_entries WHERE user_id = ? AND date = ?", this::map, userId, date);
    }

    public List<MoodEntry> findBetween(int userId, LocalDate from, LocalDate toInclusive) {
        return query("SELECT * FROM mood_entries WHERE user_id = ? AND date >= ? AND date <= ? ORDER BY date",
            this::map, userId, from, toInclusive);
    }

    /** One check-in per day: saving again on the same day replaces it. */
    public void upsert(MoodEntry m) {
        update("""
            INSERT INTO mood_entries (user_id, date, mood, stress, note) VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (user_id, date) DO UPDATE SET mood = excluded.mood, stress = excluded.stress, note = excluded.note""",
            m.getUserId(), m.getDate(), m.getMood(), m.getStress(), m.getNote());
    }

    @Override
    public int insert(MoodEntry m) {
        upsert(m);
        return findByDate(m.getUserId(), m.getDate()).map(MoodEntry::getId).orElse(-1);
    }

    @Override
    public void update(MoodEntry m) {
        upsert(m);
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM mood_entries WHERE id = ?", id);
    }

    private MoodEntry map(ResultSet rs) throws SQLException {
        MoodEntry m = new MoodEntry(rs.getInt("user_id"), toDate(rs.getString("date")), rs.getInt("mood"),
            rs.getInt("stress"), rs.getString("note"));
        m.setId(rs.getInt("id"));
        return m;
    }
}
