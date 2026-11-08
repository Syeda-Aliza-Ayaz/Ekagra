package com.ekagra.pattern.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The same day's data scored by different strategies (Strategy pattern). */
class ScoreStrategyTest {

    // studied a lot, but was very distracted and feels bad
    private final ScoreInputs grindDay = new ScoreInputs(240, 240, 3, 0, 3600, 3 * 3600, 1, 3, 20);
    // studied little, but calm, focused and healthy
    private final ScoreInputs calmDay = new ScoreInputs(60, 240, 0, 1, 3600, 300, 3, 3, 95);

    @Test
    void weightsOfEveryStrategyAddUpToOne() {
        for (ScoreStrategy s : ScoreStrategy.all()) {
            double sum = s.score(grindDay).parts().stream().mapToDouble(ScoreStrategy.Part::weight).sum();
            assertEquals(1.0, sum, 1e-9, s.getName());
        }
    }

    @Test
    void perfectDayIs100InEveryStrategy() {
        ScoreInputs perfect = new ScoreInputs(300, 240, 5, 0, 4 * 3600, 0, 3, 3, 100);
        for (ScoreStrategy s : ScoreStrategy.all()) {
            assertEquals(100, s.score(perfect).total(), s.getName());
        }
    }

    @Test
    void strategiesRankDaysDifferently() {
        ScoreStrategy study = new StudyFocusedStrategy();
        ScoreStrategy wellbeing = new WellbeingFocusedStrategy();
        assertTrue(study.score(grindDay).total() > study.score(calmDay).total(),
            "study mode prefers the long study day");
        assertTrue(wellbeing.score(calmDay).total() > wellbeing.score(grindDay).total(),
            "wellbeing mode prefers the calm day");
    }

    @Test
    void partsAreClampedAndNeutralWhenNoData() {
        ScoreInputs empty = new ScoreInputs(0, 240, 0, 0, 0, 0, 0, 0, -1);
        assertEquals(0.0, empty.focusPart());
        assertEquals(0.5, empty.taskPart());
        assertEquals(0.5, empty.focusBalancePart());
        assertEquals(0.5, empty.goalPart());
        assertEquals(0.5, empty.wellbeingPart());
        ScoreInputs over = new ScoreInputs(999, 60, 0, 0, 0, 0, 0, 0, -1);
        assertEquals(1.0, over.focusPart(), "focus beyond target doesn't exceed 100%");
    }

    @Test
    void unknownIdFallsBackToBalanced() {
        assertEquals("BALANCED", ScoreStrategy.byId("nope").getId());
        assertEquals("STUDY", ScoreStrategy.byId("STUDY").getId());
    }
}
