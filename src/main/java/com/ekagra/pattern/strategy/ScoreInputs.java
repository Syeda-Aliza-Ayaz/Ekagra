package com.ekagra.pattern.strategy;

/**
 * Everything a scoring strategy may look at for one day.
 * A value of -1 for {@code wellbeingScore} means "not available".
 */
public record ScoreInputs(
    int focusMinutes,
    int dailyTargetMinutes,
    int tasksCompleted,
    int tasksDueOrOverdue,
    int productiveSeconds,
    int distractingSeconds,
    int goalsAchieved,
    int goalsTotal,
    int wellbeingScore
) {

    /** 0..1: focus time compared with the student's daily target. */
    public double focusPart() {
        return dailyTargetMinutes <= 0 ? 0 : Math.min(1.0, (double) focusMinutes / dailyTargetMinutes);
    }

    /** 0..1: share of due/overdue work that got done. No due work + something done = 1, nothing at all = 0.5. */
    public double taskPart() {
        int relevant = tasksCompleted + tasksDueOrOverdue;
        if (relevant == 0) {
            return 0.5;
        }
        return (double) tasksCompleted / relevant;
    }

    /** 0..1: productive time as a share of productive + distracting time. No data = 0.5. */
    public double focusBalancePart() {
        int sum = productiveSeconds + distractingSeconds;
        return sum == 0 ? 0.5 : (double) productiveSeconds / sum;
    }

    /** 0..1: share of goals currently on track. No goals = 0.5. */
    public double goalPart() {
        return goalsTotal == 0 ? 0.5 : (double) goalsAchieved / goalsTotal;
    }

    /** 0..1 from the Wellbeing Score, or 0.5 when unknown. */
    public double wellbeingPart() {
        return wellbeingScore < 0 ? 0.5 : wellbeingScore / 100.0;
    }
}
