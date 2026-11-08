package com.ekagra.pattern.observer;

import com.ekagra.model.FocusSession;
import com.ekagra.model.Habit;
import com.ekagra.model.MoodEntry;
import com.ekagra.model.Task;

/** Things that happen in the app that other modules may care about (e.g. XP and badges). */
public sealed interface AppEvent {

    int userId();

    record TaskCompleted(int userId, Task task) implements AppEvent {
    }

    record FocusCompleted(int userId, FocusSession session) implements AppEvent {
    }

    record HabitChecked(int userId, Habit habit) implements AppEvent {
    }

    record MoodLogged(int userId, MoodEntry entry) implements AppEvent {
    }
}
