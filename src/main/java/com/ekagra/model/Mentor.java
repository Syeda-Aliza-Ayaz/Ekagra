package com.ekagra.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mentor/admin who supervises students.
 * AGGREGATION: a Mentor "has" Students, but students exist independently of the mentor.
 */
public class Mentor extends User {

    private String inviteCode;
    private final List<Student> students = new ArrayList<>();

    @Override
    public Role getRole() {
        return Role.MENTOR;
    }

    @Override
    public String getSubtitle() {
        return "Mentor · " + inviteCode;
    }

    @Override
    public boolean canManageRules() {
        return true;
    }

    public void addStudent(Student student) {
        if (!students.contains(student)) {
            students.add(student);
        }
    }

    public List<Student> getStudents() {
        return Collections.unmodifiableList(students);
    }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
