package com.ekagra.model;

/** A student whose productivity and wellbeing are tracked. */
public class Student extends User {

    public static final int XP_PER_LEVEL = 500;

    private String university;
    private int semester;
    private int dailyTargetMin = 240;
    private int xp;
    private Integer mentorId;

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public String getSubtitle() {
        return (semester > 0 ? "Sem " + semester + " · " : "Student · ") + "Level " + getLevel();
    }

    @Override
    public boolean canManageRules() {
        return false;
    }

    public int getLevel() {
        return xp / XP_PER_LEVEL + 1;
    }

    public int getXpIntoLevel() {
        return xp % XP_PER_LEVEL;
    }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public int getDailyTargetMin() { return dailyTargetMin; }
    public void setDailyTargetMin(int dailyTargetMin) { this.dailyTargetMin = dailyTargetMin; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public Integer getMentorId() { return mentorId; }
    public void setMentorId(Integer mentorId) { this.mentorId = mentorId; }
}
