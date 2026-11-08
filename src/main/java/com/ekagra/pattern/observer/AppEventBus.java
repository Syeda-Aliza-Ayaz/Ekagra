package com.ekagra.pattern.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * OBSERVER pattern (publish / subscribe): services publish {@link AppEvent}s, subscribers react.
 * TaskService doesn't know that GamificationService exists; it just says "a task was completed".
 */
public final class AppEventBus {

    private static final List<Consumer<AppEvent>> SUBSCRIBERS = new CopyOnWriteArrayList<>();

    private AppEventBus() {
    }

    public static void subscribe(Consumer<AppEvent> subscriber) {
        SUBSCRIBERS.add(subscriber);
    }

    public static void unsubscribe(Consumer<AppEvent> subscriber) {
        SUBSCRIBERS.remove(subscriber);
    }

    public static void publish(AppEvent event) {
        for (Consumer<AppEvent> s : SUBSCRIBERS) {
            try {
                s.accept(event);
            } catch (RuntimeException e) {
                // one failing subscriber must not break the action that published the event
                System.err.println("Event subscriber failed: " + e.getMessage());
            }
        }
    }

    /** For tests. */
    public static void clear() {
        SUBSCRIBERS.clear();
    }
}
