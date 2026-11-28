package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.AppointmentFeedback;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.store.SalonStore;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentFeedbackRepository;
import com.cmpt276.project.repository.AppointmentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment-feedback;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.sms.enabled=false"
})
@AutoConfigureMockMvc
class AppointmentFeedbackTests {
    private static final String ALEX = "alex@example.invalid";
    private static final String SAM = "sam@example.invalid";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentFeedbackRepository feedbackRepository;

    @Autowired
    private SalonStore salonStore;

    @Test
    void sendsFeedbackOnlyOnce() throws Exception {
        Account alex = accountRepository.save(new Account("Alex Lee", ALEX, "fake-password-hash", RoleType.CUSTOMER));

        saveSalon();

        Appointment appointment = appointmentRepository.save(new Appointment(
                "Haircut", "feedback-service", "feedback-salon", LocalDate.now().minusDays(2), "10:00", alex));

        MvcResult first = sendFeedback(appointment);
        MvcResult second = sendFeedback(appointment);

        assertEquals("Your feedback was sent to CS_Salon.", first.getFlashMap().get("message"));
        assertEquals("You already sent feedback for this appointment.", second.getFlashMap().get("errorMessage"));
        List<AppointmentFeedback> saved = feedbackRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(5, saved.get(0).getRating());
        assertEquals("Great cut", saved.get(0).getComment());

        String page = mockMvc.perform(get("/customer/appointments/feedback")
                .param("id", String.valueOf(appointment.getId()))
                .with(user(ALEX).roles("CUSTOMER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(page.contains("You already sent feedback for this appointment."), page);
        assertFalse(page.contains("Send feedback"));
    }

    @Test
    void showsFeedbackButtonOnlyForFinishedAppointments() throws Exception {
        Account sam = accountRepository.save(new Account("Sam Park", SAM, "fake-password-hash", RoleType.CUSTOMER));
        saveSalon();

        Appointment past = appointmentRepository.save(new Appointment(
                "Haircut", "feedback-service", "feedback-salon", LocalDate.now().minusDays(2), "10:00", sam));
        Appointment future = appointmentRepository.save(new Appointment(
                "Haircut", "feedback-service", "feedback-salon", LocalDate.now().plusDays(2), "10:00", sam));

        String page = mockMvc.perform(get("/customer/appointments")
                .with(user(SAM).roles("CUSTOMER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(page.contains("/customer/appointments/feedback?id=" + past.getId() + "\""), page);
        assertFalse(page.contains("/customer/appointments/feedback?id=" + future.getId() + "\""));
    }

    private void saveSalon() {
        Salon salon = new Salon();
        salon.setId("feedback-salon");
        salon.setOwnerId("feedback-owner");
        salon.setSlug("feedback-salon");
        salon.setName("CS_Salon");
        salon.setAddress("123 Main Street");
        SalonService service = new SalonService();
        service.setId("feedback-service");
        service.setName("Haircut");
        service.setPrice(new BigDecimal("25.00"));
        salon.getServices().add(service);
        salonStore.save(salon);
    }

    private MvcResult sendFeedback(Appointment appointment) throws Exception {
        return mockMvc.perform(post("/customer/appointments/feedback")
                .param("id", String.valueOf(appointment.getId()))
                .param("rating", "5").param("comment", "Great cut")
                .with(user(ALEX).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andReturn();
    }
}
