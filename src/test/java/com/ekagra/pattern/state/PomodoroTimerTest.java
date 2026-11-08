package com.ekagra.pattern.state;

import com.ekagra.model.FocusSession;
import com.ekagra.model.SessionType;
import com.ekagra.model.Task;
import com.ekagra.pattern.observer.TimerListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the State pattern transitions of the Pomodoro timer without any UI or database. */
class PomodoroTimerTest {

    private PomodoroTimer timer;
    private final List<FocusSession> finished = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1-minute focus, 1-minute short break, 2-minute long break, long break after 2 sessions
        timer = new PomodoroTimer(1, new TimerConfig(1, 1, 2, 2, true), LocalDateTime::now);
        timer.addListener(new TimerListener() {
            @Override
            public void onSessionFinished(PomodoroTimer t, FocusSession s) {
                finished.add(s);
            }
        });
    }

    private void tick(int seconds) {
        for (int i = 0; i < seconds; i++) {
            timer.tick();
        }
    }

    @Test
    void startsIdleAndIgnoresTicks() {
        assertInstanceOf(IdleState.class, timer.getState());
        tick(10);
        assertEquals(60, timer.getRemainingSeconds());
    }

    @Test
    void startMovesToFocusAndCountsDown() {
        timer.start();
        assertInstanceOf(FocusState.class, timer.getState());
        tick(15);
        assertEquals(45, timer.getRemainingSeconds());
        assertEquals("00:45", timer.getFormattedRemaining());
    }

    @Test
    void pauseFreezesAndResumeContinues() {
        timer.start();
        tick(10);
        timer.pause();
        assertInstanceOf(PausedState.class, timer.getState());
        tick(20);
        assertEquals(50, timer.getRemainingSeconds(), "time must not pass while paused");
        timer.resume();
        assertInstanceOf(FocusState.class, timer.getState());
        tick(5);
        assertEquals(45, timer.getRemainingSeconds());
    }

    @Test
    void completedFocusStartsShortBreakThenLongBreak() {
        Task task = new Task();
        task.setId(7);
        timer.setTask(task);

        timer.start();
        tick(60);
        assertEquals(1, finished.size());
        assertTrue(finished.get(0).isCompleted());
        assertEquals(SessionType.FOCUS, finished.get(0).getType());
        assertEquals(60, finished.get(0).getActualSec());
        assertEquals(7, finished.get(0).getTaskId());
        assertInstanceOf(BreakState.class, timer.getState());
        assertEquals(SessionType.SHORT_BREAK, timer.getCurrentType());

        tick(60);   // short break ends
        assertInstanceOf(IdleState.class, timer.getState());
        assertEquals(SessionType.FOCUS, timer.getCurrentType());

        timer.start();
        tick(60);   // second focus done -> long break due (every 2)
        assertEquals(SessionType.LONG_BREAK, timer.getCurrentType());
        assertEquals(120, timer.getRemainingSeconds());
    }

    @Test
    void stopSavesPartialSessionAndReturnsToIdle() {
        timer.start();
        tick(20);
        timer.stop();
        assertInstanceOf(IdleState.class, timer.getState());
        assertEquals(1, finished.size());
        assertFalse(finished.get(0).isCompleted());
        assertEquals(20, finished.get(0).getActualSec());
        assertEquals(60, timer.getRemainingSeconds(), "timer resets after stop");
    }

    @Test
    void stopWhilePausedUsesPreviousState() {
        timer.start();
        tick(5);
        timer.pause();
        timer.stop();
        assertInstanceOf(IdleState.class, timer.getState());
        assertEquals(5, finished.get(0).getActualSec());
    }

    @Test
    void skipBreakGoesBackToFocus() {
        timer.start();
        tick(60);
        assertInstanceOf(BreakState.class, timer.getState());
        timer.skip();
        assertInstanceOf(IdleState.class, timer.getState());
        assertEquals(SessionType.FOCUS, timer.getCurrentType());
    }

    @Test
    void typeCanOnlyBeChangedWhileIdle() {
        timer.selectType(SessionType.LONG_BREAK);
        assertEquals(SessionType.LONG_BREAK, timer.getCurrentType());
        timer.selectType(SessionType.FOCUS);
        timer.start();
        timer.selectType(SessionType.SHORT_BREAK);
        assertEquals(SessionType.FOCUS, timer.getCurrentType(), "can't switch mode mid-session");
    }

    @Test
    void distractionsAreCountedOnlyDuringFocus() {
        timer.recordDistraction();   // idle: ignored
        timer.start();
        timer.recordDistraction();
        timer.recordDistraction();
        tick(60);
        assertEquals(2, finished.get(0).getDistractions());
        timer.recordDistraction();   // on break: ignored
        timer.skip();
        assertEquals(0, finished.get(1).getDistractions());
    }

    @Test
    void noAutoBreakWhenDisabled() {
        timer.setConfig(new TimerConfig(1, 1, 2, 2, false));
        timer.start();
        tick(60);
        assertInstanceOf(IdleState.class, timer.getState());
        assertEquals(SessionType.SHORT_BREAK, timer.getCurrentType());
    }
}
