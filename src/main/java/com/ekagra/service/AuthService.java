package com.ekagra.service;

import com.ekagra.dao.UserDao;
import com.ekagra.model.Mentor;
import com.ekagra.model.Role;
import com.ekagra.model.Student;
import com.ekagra.model.User;
import com.ekagra.pattern.factory.UserFactory;
import com.ekagra.util.PasswordHasher;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Pattern;

/** Sign up, log in, log out and password changes (FR-01, FR-04). */
public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final String[] AVATAR_COLORS = {
        "#226192", "#EF8557", "#3A8D8C", "#C0574F", "#7B6BA8",
        "#C99A3E", "#4F7A5A", "#2E4E6E", "#B5677D", "#6B7A89"
    };

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDao userDao;

    public AuthService() {
        this(new UserDao());
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /** Data entered on the sign-up form. University, semester and mentorCode are only used for students. */
    public record SignupRequest(Role role, String fullName, String email, String password, String confirmPassword,
                                String university, Integer semester, String mentorCode) {
    }

    public User register(SignupRequest req) {
        String name = trim(req.fullName());
        String email = trim(req.email()).toLowerCase();

        if (name.length() < 2) {
            throw new ValidationException("Please enter your full name.");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
        validatePassword(req.password(), req.confirmPassword());
        if (userDao.emailExists(email)) {
            throw new ValidationException("An account with this email already exists.");
        }

        Mentor mentor = null;
        String code = trim(req.mentorCode());
        if (req.role() == Role.STUDENT && !code.isEmpty()) {
            mentor = userDao.findMentorByInviteCode(code)
                .orElseThrow(() -> new ValidationException("Mentor code \"" + code + "\" was not found."));
        }

        User user = UserFactory.create(req.role(), name, email);
        user.setSalt(PasswordHasher.newSalt());
        user.setPasswordHash(PasswordHasher.hash(req.password(), user.getSalt()));
        user.setAvatarColor(AVATAR_COLORS[RANDOM.nextInt(AVATAR_COLORS.length)]);

        if (user instanceof Student s) {
            s.setUniversity(trim(req.university()));
            if (req.semester() != null) {
                if (req.semester() < 1 || req.semester() > 12) {
                    throw new ValidationException("Semester must be between 1 and 12.");
                }
                s.setSemester(req.semester());
            }
        } else if (user instanceof Mentor m) {
            m.setInviteCode(generateUniqueInviteCode());
        }

        userDao.insert(user);
        if (mentor != null && user instanceof Student s) {
            userDao.linkStudentToMentor(s.getId(), mentor.getId());
            s.setMentorId(mentor.getId());
        }
        return user;
    }

    public User login(String email, String password) {
        if (trim(email).isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Please enter your email and password.");
        }
        Optional<User> found = userDao.findByEmail(trim(email));
        if (found.isEmpty() || !PasswordHasher.verify(password, found.get().getSalt(), found.get().getPasswordHash())) {
            throw new ValidationException("Incorrect email or password.");
        }
        SessionContext.getInstance().setCurrentUser(found.get());
        return found.get();
    }

    public void logout() {
        SessionContext.getInstance().clear();
    }

    public void changePassword(User user, String currentPassword, String newPassword, String confirm) {
        if (!PasswordHasher.verify(currentPassword, user.getSalt(), user.getPasswordHash())) {
            throw new ValidationException("Your current password is incorrect.");
        }
        validatePassword(newPassword, confirm);
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        userDao.updatePassword(user.getId(), hash, salt);
        user.setSalt(salt);
        user.setPasswordHash(hash);
    }

    private void validatePassword(String password, String confirm) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!password.equals(confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("EK-");
            for (int i = 0; i < 5; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (userDao.findMentorByInviteCode(code).isPresent());
        return code;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
