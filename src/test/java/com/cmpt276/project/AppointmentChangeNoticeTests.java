package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.BookingConfirmationMessage;
import com.cmpt276.project.model.BookingEmailResult;
import com.cmpt276.project.model.BookingSmsResult;
import com.cmpt276.project.model.NotificationPreference;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.store.SalonStore;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.service.BrevoEmailSender;
import com.cmpt276.project.service.GoogleCalendarClient;
import com.cmpt276.project.service.TwilioSmsSender;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment-change-notice;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.sms.enabled=false"
})
@AutoConfigureMockMvc
class AppointmentChangeNoticeTests {
    private static final String ALEX = "alex@example.invalid";
    private static final LocalDate DATE = LocalDate.now().plusDays(2);
    private static final String EMAIL_SENT = "Cancellation email submitted.";
    private static final String TEXT_SENT = "Cancellation text submitted for delivery.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SalonStore salonStore;

    @MockitoBean
    private BrevoEmailSender emailSender;

    @MockitoBean
    private TwilioSmsSender smsSender;

    @MockitoBean
    private GoogleCalendarClient calendarClient;

    private Account alex;

    // Alex wants both an email and a text
    @BeforeEach
    void prepareSalonAndCustomer() {
        appointmentRepository.deleteAll();
        accountRepository.deleteAll();
        Account customer = new Account("Alex Lee", ALEX, "fake-password-hash", RoleType.CUSTOMER);
        customer.setPhoneNumber("6045551234");
        customer.setNotificationPreference(NotificationPreference.BOTH);
        alex = accountRepository.save(customer);

        Salon salon = new Salon();
        salon.setId("change-notice-salon");
        salon.setOwnerId("change-notice-owner");
        salon.setSlug("change-notice-salon");
        salon.setName("CS_Salon");
        salon.setAddress("123 Main Street");
        SalonService service = new SalonService();
        service.setId("change-notice-service");
        service.setName("Haircut");
        service.setPrice(new BigDecimal("25.00"));
        salon.getServices().add(service);
        OpeningDay hours = new OpeningDay();
        hours.setDayOfWeek(DATE.getDayOfWeek().getValue());
        hours.setOpen(true);
        hours.setStartTime("09:00");
        hours.setEndTime("17:00");
        salon.getOpeningHours().add(hours);
        salonStore.save(salon);
    }

    @Test
    void cancelSendsEmailAndText() throws Exception {
        Appointment appointment = appointmentRepository.save(
                new Appointment("Haircut", "change-notice-service", "change-notice-salon", DATE, "10:00", alex));
        when(emailSender.send(any(BookingConfirmationMessage.class))).thenReturn(BookingEmailResult.ACCEPTED);
        when(smsSender.send(anyString(), anyString())).thenReturn(BookingSmsResult.ACCEPTED);

        MvcResult result = cancel(ALEX, appointment);

        verify(emailSender, times(1)).send(any(BookingConfirmationMessage.class));
        verify(smsSender, times(1)).send(anyString(), anyString());
        assertFalse(appointmentRepository.findById(appointment.getId()).isPresent());
        String page = followRedirect(ALEX, result);
        assertTrue(page.contains(EMAIL_SENT), page);
        assertTrue(page.contains(TEXT_SENT), page);
    }

    @Test
    void rescheduleSendsEmailAndText() throws Exception {
        Appointment appointment = appointmentRepository.save(
                new Appointment("Haircut", "change-notice-service", "change-notice-salon", DATE, "10:00", alex));
        when(emailSender.send(any(BookingConfirmationMessage.class))).thenReturn(BookingEmailResult.ACCEPTED);
        when(smsSender.send(anyString(), anyString())).thenReturn(BookingSmsResult.ACCEPTED);

        MvcResult result = edit(ALEX, appointment, "11:00");

        verify(emailSender, times(1)).send(any(BookingConfirmationMessage.class));
        verify(smsSender, times(1)).send(anyString(), anyString());
        String page = followRedirect(ALEX, result);
        assertTrue(page.contains("Email with the new time submitted."), page);
        assertTrue(page.contains("Text with the new time submitted for delivery."), page);
    }

    private MvcResult cancel(String customerEmail, Appointment appointment) throws Exception {
        return mockMvc.perform(post("/customer/appointments/cancel")
                .param("id", String.valueOf(appointment.getId()))
                .with(user(customerEmail).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andReturn();
    }

    private MvcResult edit(String customerEmail, Appointment appointment, String time) throws Exception {
        return mockMvc.perform(post("/customer/appointments/edit")
                .param("id", String.valueOf(appointment.getId()))
                .param("appointmentDate", DATE.toString()).param("appointmentTime", time)
                .with(user(customerEmail).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andReturn();
    }

    // sends the session cookie back so the flash messages reach the page
    private String followRedirect(String customerEmail, MvcResult result) throws Exception {
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        return mockMvc.perform(get(result.getResponse().getRedirectedUrl()).cookie(cookie)
                .with(user(customerEmail).roles("CUSTOMER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
