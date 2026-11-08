package com.ekagra.pattern.strategy;

/** For exam weeks: focus time and finishing tasks matter most. */
public class StudyFocusedStrategy extends WeightedScoreStrategy {

    @Override public String getId() { return "STUDY"; }
    @Override public String getName() { return "Study-focused"; }
    @Override public String getDescription() { return "Exam mode: rewards focus time and finished tasks above everything else."; }

    @Override protected double focusWeight() { return 0.50; }
    @Override protected double taskWeight() { return 0.30; }
    @Override protected double balanceWeight() { return 0.15; }
    @Override protected double goalWeight() { return 0.05; }
    @Override protected double wellbeingWeight() { return 0.0; }
}
