package com.ekagra.dao;

import com.ekagra.model.AppRule;
import com.ekagra.model.Category;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AppRuleDao extends BaseDao implements Dao<AppRule> {

    @Override
    public Optional<AppRule> findById(int id) {
        return queryOne("SELECT * FROM app_rules WHERE id = ?", this::map, id);
    }

    public List<AppRule> findAll() {
        return query("SELECT * FROM app_rules ORDER BY category, COALESCE(process_name, ''), COALESCE(title_keyword, '')",
            this::map);
    }

    @Override
    public int insert(AppRule r) {
        int id = insert("INSERT INTO app_rules (process_name, title_keyword, category, created_by) VALUES (?, ?, ?, ?)",
            r.getProcessName(), r.getTitleKeyword(), r.getCategory(), r.getCreatedBy());
        r.setId(id);
        return id;
    }

    @Override
    public void update(AppRule r) {
        update("UPDATE app_rules SET process_name = ?, title_keyword = ?, category = ? WHERE id = ?",
            r.getProcessName(), r.getTitleKeyword(), r.getCategory(), r.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM app_rules WHERE id = ?", id);
    }

    private AppRule map(ResultSet rs) throws SQLException {
        AppRule r = new AppRule(rs.getString("process_name"), rs.getString("title_keyword"),
            Category.valueOf(rs.getString("category")));
        r.setId(rs.getInt("id"));
        r.setCreatedBy(nullableInt(rs, "created_by"));
        return r;
    }
}
