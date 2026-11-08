package com.ekagra.service;

import com.ekagra.dao.AppRuleDao;
import com.ekagra.model.AppRule;
import com.ekagra.model.Category;
import com.ekagra.model.User;
import com.ekagra.tracking.AppClassifier;

import java.util.List;
import java.util.Locale;

/** Managing the global app-classification rules (FR-40). Only mentors may change them. */
public class RuleService {

    private final User user;
    private final AppRuleDao dao = new AppRuleDao();

    public RuleService(User user) {
        this.user = user;
    }

    public List<AppRule> all() {
        return dao.findAll();
    }

    public AppRule add(String process, String keyword, Category category) {
        requireMentor();
        String p = normaliseProcess(process);
        String k = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (p == null && k == null) {
            throw new ValidationException("Enter an app (e.g. discord.exe), a title keyword (e.g. YouTube), or both.");
        }
        if (category == null) {
            throw new ValidationException("Choose a category.");
        }
        boolean duplicate = all().stream().anyMatch(r -> eq(r.getProcessName(), p) && eq(r.getTitleKeyword(), k));
        if (duplicate) {
            throw new ValidationException("A rule for that app/keyword already exists. Change its category instead.");
        }
        AppRule r = new AppRule(p, k, category);
        r.setCreatedBy(user.getId());
        dao.insert(r);
        return r;
    }

    public void changeCategory(AppRule r, Category c) {
        requireMentor();
        r.setCategory(c);
        dao.update(r);
    }

    public void delete(AppRule r) {
        requireMentor();
        dao.delete(r.getId());
    }

    /** "What would Ekagra call this window?" Uses the real classifier. */
    public Category test(String process, String title) {
        return new AppClassifier(dao::findAll).classify(normaliseProcess(process), title == null ? "" : title);
    }

    private void requireMentor() {
        if (!user.canManageRules()) {      // polymorphism: Mentor returns true, Student false
            throw new ValidationException("Only mentors can change app rules.");
        }
    }

    /** "Discord" -> "discord.exe"; blank -> null. */
    static String normaliseProcess(String process) {
        if (process == null || process.isBlank()) {
            return null;
        }
        String p = process.trim().toLowerCase(Locale.ROOT);
        return p.endsWith(".exe") ? p : p + ".exe";
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
