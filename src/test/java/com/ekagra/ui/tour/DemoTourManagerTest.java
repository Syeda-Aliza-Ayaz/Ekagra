package com.ekagra.ui.tour;

import com.ekagra.app.Screen;
import com.ekagra.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoTourManagerTest {

    @Test
    void mentorTourUsesOnlyMentorScreens() {
        var steps = DemoTourManager.stepsFor(Role.MENTOR);

        assertFalse(steps.isEmpty());
        assertTrue(steps.stream().allMatch(step -> Screen.navFor(Role.MENTOR).contains(step.screen())));
        assertEquals(Screen.MENTOR, steps.get(0).screen());
    }
}
