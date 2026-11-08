package com.ekagra.service;

import com.ekagra.model.Student;
import com.ekagra.model.User;

/** SINGLETON pattern: holds the currently logged-in user for the whole app. */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private User currentUser;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isStudent() {
        return currentUser instanceof Student;
    }

    /** Convenience for student-only screens. */
    public Student getStudent() {
        if (currentUser instanceof Student s) {
            return s;
        }
        throw new IllegalStateException("Current user is not a student");
    }

    public void clear() {
        currentUser = null;
    }
}
