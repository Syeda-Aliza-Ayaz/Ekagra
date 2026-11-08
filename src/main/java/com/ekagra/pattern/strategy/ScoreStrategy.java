package com.ekagra.pattern.strategy;

import java.util.List;

/**
 * STRATEGY pattern: different ways to turn a day's data into a 0..100 Productivity Score (FR-31).
 * The analytics code just calls {@code strategy.score(inputs)}; which formula runs depends on the
 * strategy the student picked (Balanced, Study-focused or Wellbeing-focused), and new formulas can be
 * added without touching the callers.
 */
public interface ScoreStrategy {

    /** Stable id stored in settings, e.g. "BALANCED". */
    String getId();

    String getName();

    String getDescription();

    ScoreBreakdown score(ScoreInputs inputs);

    /** One weighted ingredient of the score. {@code value} is 0..1. */
    record Part(String name, double value, double weight) {
        public int points() {
            return (int) Math.round(value * weight * 100);
        }
    }

    /** Total 0..100 and how it was made up. */
    record ScoreBreakdown(int total, List<Part> parts) {
    }

    /** All available strategies, in the order shown in the UI. */
    static List<ScoreStrategy> all() {
        return List.of(new BalancedStrategy(), new StudyFocusedStrategy(), new WellbeingFocusedStrategy());
    }

    static ScoreStrategy byId(String id) {
        return all().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(new BalancedStrategy());
    }
}
