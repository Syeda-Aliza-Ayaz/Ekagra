package com.ekagra.service;

import com.ekagra.db.DatabaseManager;
import com.ekagra.model.Mentor;
import com.ekagra.model.Role;
import com.ekagra.model.Student;
import com.ekagra.model.User;
import com.ekagra.util.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService auth;

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
        auth = new AuthService();
    }

    @AfterEach
    void tearDown() {
        SessionContext.getInstance().clear();
        DatabaseManager.shutdown();
    }

    private AuthService.SignupRequest student(String email, String code) {
        return new AuthService.SignupRequest(Role.STUDENT, "Ayesha Khan", email, "secret12", "secret12",
            "NED", 3, code);
    }

    @Test
    void registerStudentAndLogin() {
        User created = auth.register(student("ayesha@uni.edu", null));
        assertTrue(created instanceof Student);
        assertTrue(created.getId() > 0);

        User loggedIn = auth.login("AYESHA@uni.edu", "secret12");
        assertEquals(created.getId(), loggedIn.getId());
        assertEquals(Role.STUDENT, loggedIn.getRole());
        assertSame(loggedIn, SessionContext.getInstance().getCurrentUser());
    }

    @Test
    void wrongPasswordIsRejected() {
        auth.register(student("a@uni.edu", null));
        assertThrows(ValidationException.class, () -> auth.login("a@uni.edu", "wrongpass"));
    }

    @Test
    void duplicateEmailIsRejected() {
        auth.register(student("a@uni.edu", null));
        assertThrows(ValidationException.class, () -> auth.register(student("a@uni.edu", null)));
    }

    @Test
    void shortOrMismatchedPasswordIsRejected() {
        var shortPw = new AuthService.SignupRequest(Role.STUDENT, "Ali", "b@uni.edu", "123", "123", null, 1, null);
        assertThrows(ValidationException.class, () -> auth.register(shortPw));
        var mismatch = new AuthService.SignupRequest(Role.STUDENT, "Ali", "b@uni.edu", "secret12", "secret13", null, 1, null);
        assertThrows(ValidationException.class, () -> auth.register(mismatch));
    }

    @Test
    void mentorGetsInviteCodeAndStudentCanLink() {
        Mentor mentor = (Mentor) auth.register(new AuthService.SignupRequest(Role.MENTOR, "Dr. Sara", "sara@uni.edu",
            "secret12", "secret12", null, null, null));
        assertNotNull(mentor.getInviteCode());
        assertTrue(mentor.getInviteCode().startsWith("EK-"));

        Student s = (Student) auth.register(student("ali@uni.edu", mentor.getInviteCode().toLowerCase()));
        assertEquals(mentor.getId(), s.getMentorId());

        var students = new ProfileService().getStudentsOf(mentor);
        assertEquals(1, students.size());
        assertEquals("ali@uni.edu", students.get(0).getEmail());
    }

    @Test
    void unknownMentorCodeIsRejected() {
        assertThrows(ValidationException.class, () -> auth.register(student("x@uni.edu", "EK-NOPE1")));
    }

    @Test
    void changePasswordWorks() {
        User u = auth.register(student("c@uni.edu", null));
        auth.changePassword(u, "secret12", "newpass99", "newpass99");
        assertThrows(ValidationException.class, () -> auth.login("c@uni.edu", "secret12"));
        assertNotNull(auth.login("c@uni.edu", "newpass99"));
    }

    @Test
    void hashingIsSaltedAndVerifiable() {
        String s1 = PasswordHasher.newSalt();
        String s2 = PasswordHasher.newSalt();
        assertNotEquals(PasswordHasher.hash("pw", s1), PasswordHasher.hash("pw", s2));
        assertTrue(PasswordHasher.verify("pw", s1, PasswordHasher.hash("pw", s1)));
        assertFalse(PasswordHasher.verify("PW", s1, PasswordHasher.hash("pw", s1)));
    }
}
