package com.ekagra.pattern.strategy;

/** For recovery weeks: healthy screen habits and wellbeing count most. */
public class WellbeingFocusedStrategy extends WeightedScoreStrategy {

    @Override public String getId() { return "WELLBEING"; }
    @Override public String getName() { return "Wellbeing-focused"; }
    @Override public String getDescription() { return "Gentle mode: rewards low distraction, healthy habits and how you feel."; }

    @Override protected double focusWeight() { return 0.15; }
    @Override protected double taskWeight() { return 0.10; }
    @Override protected double balanceWeight() { return 0.30; }
    @Override protected double goalWeight() { return 0.10; }
    @Override protected double wellbeingWeight() { return 0.35; }
}
