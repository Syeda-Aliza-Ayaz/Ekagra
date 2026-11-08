package com.ekagra.service;

import com.ekagra.dao.UserDao;
import com.ekagra.model.Mentor;
import com.ekagra.model.Student;
import com.ekagra.model.User;

import java.util.List;
import java.util.Optional;

/** Profile editing and mentor linking (FR-02, FR-03). */
public class ProfileService {

    private final UserDao userDao;

    public ProfileService() {
        this(new UserDao());
    }

    public ProfileService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void updateProfile(User user, String fullName, String avatarColor,
                              String university, int semester, int dailyTargetMin) {
        if (fullName == null || fullName.trim().length() < 2) {
            throw new ValidationException("Please enter your full name.");
        }
        user.setFullName(fullName.trim());
        user.setAvatarColor(avatarColor);
        if (user instanceof Student s) {
            if (semester < 1 || semester > 12) {
                throw new ValidationException("Semester must be between 1 and 12.");
            }
            if (dailyTargetMin < 15 || dailyTargetMin > 960) {
                throw new ValidationException("Daily study target must be between 15 min and 16 hours.");
            }
            s.setUniversity(university == null ? "" : university.trim());
            s.setSemester(semester);
            s.setDailyTargetMin(dailyTargetMin);
        }
        userDao.update(user);
    }

    public Mentor linkMentor(Student student, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new ValidationException("Please enter your mentor's code.");
        }
        Mentor mentor = userDao.findMentorByInviteCode(inviteCode)
            .orElseThrow(() -> new ValidationException("No mentor found with code \"" + inviteCode.trim() + "\"."));
        userDao.linkStudentToMentor(student.getId(), mentor.getId());
        student.setMentorId(mentor.getId());
        return mentor;
    }

    public void unlinkMentor(Student student) {
        userDao.unlinkStudent(student.getId());
        student.setMentorId(null);
    }

    public Optional<User> findUser(int id) {
        return userDao.findById(id);
    }

    public List<Student> getStudentsOf(Mentor mentor) {
        List<Student> students = userDao.findStudentsOfMentor(mentor.getId());
        students.forEach(mentor::addStudent);
        return students;
    }
}
