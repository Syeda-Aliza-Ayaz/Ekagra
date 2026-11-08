package com.ekagra.tracking;

import com.ekagra.model.AppRule;
import com.ekagra.model.Category;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Decides whether a window is Productive, Neutral or Distracting (FR-16).
 * The most specific matching rule wins: "process + title" beats "title only" beats "process only".
 * Anything that matches no rule is Neutral.
 */
public class AppClassifier {

    private final Supplier<List<AppRule>> ruleSource;
    private volatile List<AppRule> rules;

    public AppClassifier(Supplier<List<AppRule>> ruleSource) {
        this.ruleSource = ruleSource;
        reload();
    }

    /** Re-reads the rules, e.g. after a mentor edits them. */
    public final void reload() {
        rules = ruleSource.get().stream()
            .sorted(Comparator.comparingInt(AppRule::specificity).reversed())
            .toList();
    }

    public Category classify(WindowInfo window) {
        return classify(window.processName(), window.title());
    }

    public Category classify(String process, String title) {
        for (AppRule rule : rules) {
            if (rule.matches(process, title)) {
                return rule.getCategory();
            }
        }
        return Category.NEUTRAL;
    }
}
