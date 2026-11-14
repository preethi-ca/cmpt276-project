package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.GoogleCalendarConnection;
import com.cmpt276.project.model.GoogleCalendarEvent;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.store.SalonStore;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.repository.GoogleCalendarConnectionRepository;
import com.cmpt276.project.service.GoogleCalendarClient;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:google-calendar-appointment;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.calendar.enabled=true",
        "bookeasy.calendar.client-id=fake-client-id",
        "bookeasy.calendar.client-secret=fake-client-secret",
        "bookeasy.calendar.redirect-uri=https://bookeasy.example.invalid/calendar/google/callback"
})
@AutoConfigureMockMvc
class GoogleCalendarAppointmentTests {
    private static final String ALEX = "alex@example.invalid";
    private static final String SAM = "sam@example.invalid";
    private static final String REFRESH_TOKEN = "fake-refresh-token";
    private static final String EVENT_ID = "fake-event-id";
    private static final LocalDate DATE = LocalDate.now().plusDays(2);
    private static final String UPDATED = "Your Google Calendar event was updated.";
    private static final String REMOVED = "Removed from your Google Calendar.";
    private static final String UPDATE_FAILED = "Couldn't update Google Calendar. Your appointment change is still saved.";
    private static final String REMOVE_FAILED = "Couldn't remove it from Google Calendar. Your appointment is still cancelled.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private GoogleCalendarConnectionRepository connectionRepository;

    @Autowired
    private SalonStore salonStore;

    @MockitoBean
    private GoogleCalendarClient calendarClient;

    private Account alex;
    private Account sam;

    // Alex has connected Google Calendar, Sam has not
    @BeforeEach
    void prepareSalonAndCustomers() {
        appointmentRepository.deleteAll();
        connectionRepository.deleteAll();
        accountRepository.deleteAll();
        alex = accountRepository.save(new Account("Alex Lee", ALEX, "fake-password-hash", RoleType.CUSTOMER));
        sam = accountRepository.save(new Account("Sam Chen", SAM, "fake-password-hash", RoleType.CUSTOMER));
        connectionRepository.save(new GoogleCalendarConnection(alex, REFRESH_TOKEN));

        Salon salon = new Salon();
        salon.setId("calendar-edit-salon");
        salon.setOwnerId("calendar-edit-owner");
        salon.setSlug("calendar-edit-salon");
        salon.setName("CS_Salon");
        salon.setAddress("123 Main Street");
        SalonService service = new SalonService();
        service.setId("calendar-edit-service");
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
    void cancelRemovesTheEventAndDeletesTheAppointment() throws Exception {
        Appointment appointment = saveAppointment(alex, EVENT_ID, "10:00");
        when(calendarClient.deleteEvent(REFRESH_TOKEN, EVENT_ID)).thenReturn(true);

        MvcResult result = cancel(ALEX, appointment);

        verify(calendarClient, times(1)).deleteEvent(REFRESH_TOKEN, EVENT_ID);
        assertEquals(0, appointmentRepository.count());
        assertEquals(REMOVED, result.getFlashMap().get("calendarMessage"));
        String page = followRedirect(ALEX, result);
        assertTrue(page.contains(REMOVED));
        assertFalse(page.contains(REFRESH_TOKEN));
    }

    @Test
    void cancelStillDeletesWhenGoogleFails() throws Exception {
        Appointment appointment = saveAppointment(alex, EVENT_ID, "10:00");
        when(calendarClient.deleteEvent(anyString(), anyString())).thenReturn(false);

        MvcResult result = cancel(ALEX, appointment);

        assertEquals(0, appointmentRepository.count());
        assertEquals(REMOVE_FAILED, result.getFlashMap().get("calendarMessage"));
        // the page escapes the apostrophe, so check the rest of the sentence
        String page = followRedirect(ALEX, result);
        assertTrue(page.contains("remove it from Google Calendar. Your appointment is still cancelled."), page);
    }

    @Test
    void editUpdatesTheEventAndSavesTheChange() throws Exception {
        Appointment appointment = saveAppointment(alex, EVENT_ID, "10:00");
        when(calendarClient.updateEvent(eq(REFRESH_TOKEN), eq(EVENT_ID), any(GoogleCalendarEvent.class))).thenReturn(true);

        MvcResult result = edit(ALEX, appointment, "11:00");

        verify(calendarClient, times(1)).updateEvent(eq(REFRESH_TOKEN), eq(EVENT_ID), any(GoogleCalendarEvent.class));
        Appointment saved = appointmentRepository.findById(appointment.getId()).get();
        assertEquals("11:00", saved.getAppointmentTime());
        assertEquals(EVENT_ID, saved.getGoogleEventId());
        assertEquals(UPDATED, result.getFlashMap().get("calendarMessage"));
        String page = followRedirect(ALEX, result);
        assertTrue(page.contains(UPDATED));
        assertFalse(page.contains(REFRESH_TOKEN));
    }

    @Test
    void editStillSavesWhenGoogleFails() throws Exception {
        Appointment appointment = saveAppointment(alex, EVENT_ID, "10:00");
        when(calendarClient.updateEvent(anyString(), anyString(), any(GoogleCalendarEvent.class))).thenReturn(false);

        MvcResult result = edit(ALEX, appointment, "11:00");

        assertEquals("11:00", appointmentRepository.findById(appointment.getId()).get().getAppointmentTime());
        assertEquals("Your appointment has been successfully updated.", result.getFlashMap().get("message"));
        assertEquals(UPDATE_FAILED, result.getFlashMap().get("calendarMessage"));
        String page = followRedirect(ALEX, result);
        assertTrue(page.contains("update Google Calendar. Your appointment change is still saved."), page);
    }

    @Test
    void customerWithoutAConnectionSeesNoCalendarMessage() throws Exception {
        Appointment first = saveAppointment(sam, null, "10:00");
        Appointment second = saveAppointment(sam, null, "12:00");

        MvcResult edited = edit(SAM, first, "11:00");
        assertNull(edited.getFlashMap().get("calendarMessage"));
        assertFalse(followRedirect(SAM, edited).contains("Google Calendar"));

        MvcResult cancelled = cancel(SAM, second);
        assertNull(cancelled.getFlashMap().get("calendarMessage"));
        assertFalse(followRedirect(SAM, cancelled).contains("Google Calendar"));

        verifyNoInteractions(calendarClient);
    }

    @Test
    void flashMessagesNeverHoldTheRefreshToken() throws Exception {
        Appointment appointment = saveAppointment(alex, EVENT_ID, "10:00");
        when(calendarClient.updateEvent(anyString(), anyString(), any(GoogleCalendarEvent.class))).thenReturn(true);
        when(calendarClient.deleteEvent(anyString(), anyString())).thenReturn(true);

        MvcResult edited = edit(ALEX, appointment, "11:00");
        MvcResult cancelled = cancel(ALEX, appointment);

        assertFalse(edited.getFlashMap().toString().contains(REFRESH_TOKEN));
        assertFalse(cancelled.getFlashMap().toString().contains(REFRESH_TOKEN));
    }

    private Appointment saveAppointment(Account customer, String eventId, String time) {
        Appointment appointment = new Appointment("Haircut", "calendar-edit-service", "calendar-edit-salon", DATE, time, customer);
        appointment.setGoogleEventId(eventId);
        return appointmentRepository.save(appointment);
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

    // opens the page the redirect goes to, sending the session cookie back like a browser
    // so the flash messages come along (the app keeps sessions in the database)
    private String followRedirect(String customerEmail, MvcResult result) throws Exception {
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertNotNull(cookie);
        return mockMvc.perform(get(result.getResponse().getRedirectedUrl()).cookie(cookie)
                .with(user(customerEmail).roles("CUSTOMER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
