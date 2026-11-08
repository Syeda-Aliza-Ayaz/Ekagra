package com.ekagra.pattern.factory;

import com.ekagra.model.Mentor;
import com.ekagra.model.Role;
import com.ekagra.model.Student;
import com.ekagra.model.User;

/**
 * FACTORY pattern: callers ask for a user by {@link Role} and get the right subclass,
 * without writing "new Student()" / "new Mentor()" and if/else checks everywhere.
 */
public final class UserFactory {

    private UserFactory() {
    }

    public static User create(Role role) {
        return switch (role) {
            case STUDENT -> new Student();
            case MENTOR -> new Mentor();
        };
    }

    public static User create(Role role, String fullName, String email) {
        User user = create(role);
        user.setFullName(fullName);
        user.setEmail(email);
        return user;
    }
}
