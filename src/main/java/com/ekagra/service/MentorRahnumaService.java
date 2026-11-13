package com.ekagra.service;

import com.ekagra.app.Screen;
import com.ekagra.model.Mentor;
import com.ekagra.service.MentorService.StudentOverview;

import java.util.List;
import java.util.Locale;

/** Mentor-facing Rahnuma responses grounded in the mentor's linked students. */
public class MentorRahnumaService {

    private final Mentor mentor;
    private final MentorService mentorService;

    public MentorRahnumaService(Mentor mentor) {
        this.mentor = mentor;
        this.mentorService = new MentorService(mentor);
    }

    public String getGreeting() {
        return "Salam " + mentor.getFirstName() + "! I am Rahnuma, your mentoring assistant. "
            + "I can summarize your linked students, highlight who may need support, or offer mentoring guidance.";
    }

    public RahnumaService.RahnumaResponse summarizeStudents() {
        List<StudentOverview> students = mentorService.overview();
        if (students.isEmpty()) {
            return new RahnumaService.RahnumaResponse(
                "You don't have any linked students yet. Share your mentor code so students can connect with you.",
                "Open My Students", Screen.MENTOR);
        }

        long atRisk = students.stream().filter(StudentOverview::isAtRisk).count();
        StringBuilder text = new StringBuilder("Student overview (last 7 days):\n\n");
        text.append("• Linked students: ").append(students.size()).append("\n");
        text.append("• Flagged for follow-up: ").append(atRisk).append("\n\n");
        for (StudentOverview student : students) {
            text.append("• ").append(student.student().getFullName())
                .append(" — average score ").append(student.avgScore())
                .append(", focus ").append(student.focusMinutes()).append(" min");
            if (student.overdue() > 0) {
                text.append(", ").append(student.overdue()).append(" overdue task(s)");
            }
            if (student.isAtRisk()) {
                text.append(" — ").append(String.join(", ", student.riskReasons()));
            }
            text.append("\n");
        }
        return new RahnumaService.RahnumaResponse(text.toString(), "Open My Students", Screen.MENTOR);
    }

    public RahnumaService.RahnumaResponse studentsNeedingSupport() {
        List<StudentOverview> atRisk = mentorService.overview().stream()
            .filter(StudentOverview::isAtRisk)
            .toList();
        if (atRisk.isEmpty()) {
            return new RahnumaService.RahnumaResponse(
                "No students are currently flagged for follow-up. Keep checking in and reviewing their progress.",
                "Open My Students", Screen.MENTOR);
        }

        StringBuilder text = new StringBuilder("Students who may need follow-up:\n\n");
        for (StudentOverview student : atRisk) {
            text.append("• ").append(student.student().getFullName())
                .append(": ").append(String.join(", ", student.riskReasons())).append("\n");
        }
        return new RahnumaService.RahnumaResponse(text.toString(), "Review My Students", Screen.MENTOR);
    }

    public RahnumaService.RahnumaResponse answerQuery(String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        if (normalized.contains("risk") || normalized.contains("support") || normalized.contains("struggling")
            || normalized.contains("attention") || normalized.contains("inactive")) {
            return studentsNeedingSupport();
        }
        if (normalized.contains("student") || normalized.contains("progress") || normalized.contains("score")
            || normalized.contains("overview") || normalized.contains("performance")) {
            return summarizeStudents();
        }
        return new RahnumaService.RahnumaResponse(
            "I can help review student progress or identify students who may need support. "
                + "Try asking “How are my students doing?” or “Who needs attention?”",
            "Open My Students", Screen.MENTOR);
    }
}
