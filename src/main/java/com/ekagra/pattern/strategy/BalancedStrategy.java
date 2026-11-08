package com.ekagra.pattern.strategy;

/** Default: a bit of everything. */
public class BalancedStrategy extends WeightedScoreStrategy {

    @Override public String getId() { return "BALANCED"; }
    @Override public String getName() { return "Balanced"; }
    @Override public String getDescription() { return "Equal attention to studying, tasks, staying focused and your goals."; }

    @Override protected double focusWeight() { return 0.30; }
    @Override protected double taskWeight() { return 0.20; }
    @Override protected double balanceWeight() { return 0.20; }
    @Override protected double goalWeight() { return 0.15; }
    @Override protected double wellbeingWeight() { return 0.15; }
}
