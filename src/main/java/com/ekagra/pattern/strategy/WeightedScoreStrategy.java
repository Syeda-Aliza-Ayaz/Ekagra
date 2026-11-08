package com.ekagra.pattern.strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for strategies that are a weighted sum of the same ingredients.
 * Subclasses only choose the weights (they must add up to 1).
 */
public abstract class WeightedScoreStrategy implements ScoreStrategy {

    protected abstract double focusWeight();

    protected abstract double taskWeight();

    protected abstract double balanceWeight();

    protected abstract double goalWeight();

    protected abstract double wellbeingWeight();

    @Override
    public ScoreBreakdown score(ScoreInputs in) {
        List<Part> parts = new ArrayList<>();
        add(parts, "Focus time", in.focusPart(), focusWeight());
        add(parts, "Tasks", in.taskPart(), taskWeight());
        add(parts, "Focus vs distraction", in.focusBalancePart(), balanceWeight());
        add(parts, "Goals", in.goalPart(), goalWeight());
        add(parts, "Wellbeing", in.wellbeingPart(), wellbeingWeight());
        double total = parts.stream().mapToDouble(p -> p.value() * p.weight()).sum() * 100;
        return new ScoreBreakdown((int) Math.round(Math.max(0, Math.min(100, total))), parts);
    }

    private static void add(List<Part> parts, String name, double value, double weight) {
        if (weight > 0) {
            parts.add(new Part(name, value, weight));
        }
    }
}
