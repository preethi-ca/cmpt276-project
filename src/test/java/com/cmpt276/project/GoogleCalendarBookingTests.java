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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:google-calendar-booking;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.calendar.enabled=true",
        "bookeasy.calendar.client-id=fake-client-id",
        "bookeasy.calendar.client-secret=fake-client-secret",
        "bookeasy.calendar.redirect-uri=https://bookeasy.example.invalid/calendar/google/callback"
})
@AutoConfigureMockMvc
class GoogleCalendarBookingTests {
    private static final String ALEX = "alex@example.invalid";
    private static final String SAM = "sam@example.invalid";
    private static final String REFRESH_TOKEN = "fake-refresh-token";
    private static final String DATE = LocalDate.now().plusDays(2).toString();
    private static final String ADDED = "Added to your Google Calendar.";
    private static final String FAILED = "Couldn't add it to Google Calendar. Your appointment is still booked.";

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

    private Salon salon;

    // Alex has connected Google Calendar, Sam has not
    @BeforeEach
    void prepareSalonAndCustomers() {
        appointmentRepository.deleteAll();
        connectionRepository.deleteAll();
        accountRepository.deleteAll();
        Account alex = accountRepository.save(new Account("Alex Lee", ALEX, "fake-password-hash", RoleType.CUSTOMER));
        accountRepository.save(new Account("Sam Chen", SAM, "fake-password-hash", RoleType.CUSTOMER));
        connectionRepository.save(new GoogleCalendarConnection(alex, REFRESH_TOKEN));

        salon = new Salon();
        salon.setId("calendar-test-salon");
        salon.setOwnerId("calendar-test-owner");
        salon.setSlug("calendar-test-salon");
        salon.setName("CS_Salon");
        salon.setAddress("123 Main Street");
        SalonService service = new SalonService();
        service.setId("calendar-test-service");
        service.setName("Haircut");
        service.setPrice(new BigDecimal("25.00"));
        salon.getServices().add(service);
        OpeningDay hours = new OpeningDay();
        hours.setDayOfWeek(LocalDate.parse(DATE).getDayOfWeek().getValue());
        hours.setOpen(true);
        hours.setStartTime("09:00");
        hours.setEndTime("17:00");
        salon.getOpeningHours().add(hours);
        salonStore.save(salon);
    }

    @Test
    void connectedCustomerBookingIsAddedToGoogleCalendar() throws Exception {
        when(calendarClient.createEvent(eq(REFRESH_TOKEN), any(GoogleCalendarEvent.class))).thenReturn("fake-event-id");

        MvcResult result = book(ALEX, DATE, "09:00");

        assertEquals("customer/booking-confirmation", result.getModelAndView().getViewName());
        assertTrue(result.getResponse().getContentAsString().contains(ADDED));
        verify(calendarClient, times(1)).createEvent(eq(REFRESH_TOKEN), any(GoogleCalendarEvent.class));
        Appointment saved = onlyAppointment();
        assertEquals("fake-event-id", saved.getGoogleEventId());
    }

    @Test
    void customerWithoutAConnectionBooksWithNoCalendarMessage() throws Exception {
        MvcResult result = book(SAM, DATE, "09:00");

        assertEquals("customer/booking-confirmation", result.getModelAndView().getViewName());
        assertNull(result.getModelAndView().getModel().get("calendarMessage"));
        String page = result.getResponse().getContentAsString();
        assertFalse(page.contains("Google Calendar"));
        verify(calendarClient, never()).createEvent(anyString(), any());
        assertNull(onlyAppointment().getGoogleEventId());
    }

    @Test
    void googleFailureKeepsTheBookingAndShowsTheFailureMessage() throws Exception {
        when(calendarClient.createEvent(anyString(), any(GoogleCalendarEvent.class))).thenReturn(null);

        MvcResult result = book(ALEX, DATE, "09:00");

        assertEquals("customer/booking-confirmation", result.getModelAndView().getViewName());
        String page = result.getResponse().getContentAsString();
        assertTrue(page.contains("Booking Confirmed!"));
        assertEquals(FAILED, result.getModelAndView().getModel().get("calendarMessage"));
        // the page escapes the apostrophe, so check the rest of the sentence
        assertTrue(page.contains("add it to Google Calendar. Your appointment is still booked."), page);
        assertNull(onlyAppointment().getGoogleEventId());
    }

    @Test
    void failedBookingIsNeverSentToGoogle() throws Exception {
        // Sam takes the slot first, so Alex's booking fails
        book(SAM, DATE, "09:00");
        assertEquals("customer/select-time", book(ALEX, DATE, "09:00").getModelAndView().getViewName());

        String pastDate = LocalDate.now().minusDays(1).toString();
        assertEquals("customer/select-date", book(ALEX, pastDate, "09:00").getModelAndView().getViewName());

        assertEquals(1, appointmentRepository.count());
        verify(calendarClient, never()).createEvent(anyString(), any());
    }

    @Test
    void viewingAppointmentsAfterBookingDoesNotAddItAgain() throws Exception {
        when(calendarClient.createEvent(anyString(), any(GoogleCalendarEvent.class))).thenReturn("fake-event-id");
        book(ALEX, DATE, "09:00");
        Appointment saved = onlyAppointment();

        mockMvc.perform(get("/customer/appointments").with(user(ALEX).roles("CUSTOMER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/customer/appointments/details").param("id", String.valueOf(saved.getId()))
                .with(user(ALEX).roles("CUSTOMER")))
                .andExpect(status().isOk());

        verify(calendarClient, times(1)).createEvent(anyString(), any());
        assertEquals("fake-event-id", onlyAppointment().getGoogleEventId());
    }

    @Test
    void confirmationPageNeverShowsTheRefreshToken() throws Exception {
        when(calendarClient.createEvent(anyString(), any(GoogleCalendarEvent.class))).thenReturn("fake-event-id");

        String page = book(ALEX, DATE, "09:00").getResponse().getContentAsString();

        assertFalse(page.contains(REFRESH_TOKEN));
        assertFalse(page.contains("fake-event-id"));
    }

    private MvcResult book(String customerEmail, String date, String time) throws Exception {
        return mockMvc.perform(post("/customer/booking/confirm")
                .param("service", "calendar-test-service").param("salon", salon.getSlug())
                .param("appointmentDate", date).param("appointmentTime", time)
                .with(user(customerEmail).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isOk()).andReturn();
    }

    private Appointment onlyAppointment() {
        assertEquals(1, appointmentRepository.count());
        return appointmentRepository.findAll().get(0);
    }
}
