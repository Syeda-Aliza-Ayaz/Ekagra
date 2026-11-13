package com.ekagra.service;

import com.ekagra.app.Screen;
import com.ekagra.dao.UserDao;
import com.ekagra.db.DatabaseManager;
import com.ekagra.db.DemoDataSeeder;
import com.ekagra.model.Mentor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentorRahnumaServiceTest {

    private MentorRahnumaService rahnuma;

    @BeforeEach
    void setUp() {
        DatabaseManager.shutdown();
        DatabaseManager.configure(DatabaseManager.IN_MEMORY);
        new DemoDataSeeder().seed();
        Mentor mentor = (Mentor) new UserDao().findByEmail(DemoDataSeeder.MENTOR_EMAIL).orElseThrow();
        rahnuma = new MentorRahnumaService(mentor);
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.shutdown();
    }

    @Test
    void answersStudentProgressQuestionsWithLinkedStudentData() {
        var response = rahnuma.answerQuery("How are my students doing?");

        assertTrue(response.text().contains("Linked students: 3"));
        assertTrue(response.text().contains("Bilal Raza"));
    }

    @Test
    void identifiesStudentsNeedingSupport() {
        var response = rahnuma.answerQuery("Who needs attention?");

        assertTrue(response.text().contains("Bilal Raza"));
        assertEquals(Screen.MENTOR, response.targetScreen());
    }
}
