package com.cmpt276.project;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.controller.OwnerController.HoursForm;
import com.cmpt276.project.owner.controller.OwnerController.SalonForm;
import com.cmpt276.project.owner.controller.OwnerController.ServiceForm;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.owner.store.JpaSalonStore;
import com.cmpt276.project.owner.store.SalonStore;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.service.BookingService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercise the actual PostgreSQL store adapter against an isolated H2 database.
// Tests intentionally avoid an enclosing transaction: each reload must read committed data.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:owner-updates;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false"
})

@ActiveProfiles("postgres")
@AutoConfigureMockMvc
class Cmpt276ProjectApplicationTests {
    @Autowired private MockMvc mvc;
    @Autowired private OwnerService owners;
    @Autowired private SalonStore store;
    @Autowired private AccountRepository accounts;
    @Autowired private AppointmentRepository appointments;
    @Autowired private BookingService bookings;
    private Account owner;
    private String ownerId;

    @BeforeEach
    void createOwnerAndSalon() {
        owner = accounts.save(new Account("Owner", UUID.randomUUID() + "@example.com",
                "unused-test-hash", RoleType.SALONOWNER));
        ownerId = String.valueOf(owner.getId());
        SalonForm form = profile();
        assertTrue(owners.saveProfile(ownerId, form, true, errors(form, "salonForm")));
    }

    @Test
    void contextLoads() {
        assertInstanceOf(JpaSalonStore.class, store);
    }

    @Test
    void profileUpdatesPersistAndKeepLoginIdentityAndSalonId() throws Exception {
        Salon original = store.findByOwnerId(ownerId);
        mvc.perform(post("/owner/profile").with(user(owner.getEmail()).roles("SALONOWNER")).with(csrf())
                .param("name", "Updated Salon").param("address", "456 Main Street")
                .param("city", " Vancouver ").param("province", " British Columbia ")
                .param("phone", " +1 (604) 555-0123 ").param("email", " contact@example.com ")
                .param("ownerId", "someone-else").param("id", "tampered").param("slug", "tampered"))
                .andExpect(redirectedUrl("/owner/profile"));
        Salon saved = store.findById(original.getId());
        assertEquals("Vancouver", saved.getCity());
        assertEquals("British Columbia", saved.getProvince());
        assertEquals("+1 (604) 555-0123", saved.getPhone());
        assertEquals("contact@example.com", saved.getEmail());
        assertEquals(original.getSlug(), saved.getSlug());
        assertEquals(ownerId, saved.getOwnerId());
        assertEquals(owner.getEmail(), accounts.findById(owner.getId()).orElseThrow().getEmail());
        mvc.perform(get("/owner/profile").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Vancouver\"")))
                .andExpect(content().string(containsString("value=\"British Columbia\"")))
                .andExpect(content().string(containsString("value=\"+1 (604) 555-0123\"")))
                .andExpect(content().string(containsString("value=\"contact@example.com\"")))
                .andExpect(content().string(containsString("name=\"city\"")));
        mvc.perform(get("/salons/" + saved.getSlug())).andExpect(status().isOk())
                .andExpect(content().string(containsString("contact@example.com")))
                .andExpect(content().string(containsString("Vancouver")))
                .andExpect(content().string(not(containsString(owner.getEmail()))));
    }

    @Test
    void invalidContactFieldsKeepInputAndDoNotPartiallySave() throws Exception {
        mvc.perform(post("/owner/profile").with(user(owner.getEmail()).roles("SALONOWNER")).with(csrf())
                .param("name", "Must not save").param("city", "Retain this city")
                .param("phone", "abc").param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("salonForm", "phone", "email"))
                .andExpect(content().string(containsString("value=\"Retain this city\"")));
        assertEquals("Owner Test Salon", store.findByOwnerId(ownerId).getName());
        assertEquals("Burnaby", store.findByOwnerId(ownerId).getCity());

        SalonForm tooLong = profile();
        tooLong.setCity("c".repeat(81));
        tooLong.setProvince("p".repeat(81));
        tooLong.setPhone("1".repeat(33));
        tooLong.setEmail("a".repeat(243) + "@example.com");
        var errors = errors(tooLong, "salonForm");
        assertFalse(owners.saveProfile(ownerId, tooLong, false, errors));
        for (String field : List.of("city", "province", "phone", "email")) {
            assertTrue(errors.hasFieldErrors(field), field);
        }
        assertEquals("Burnaby", store.findByOwnerId(ownerId).getCity());
    }

    @Test
    void optionalContactsCanBeClearedAndTextIsEscaped() throws Exception {
        SalonForm form = profile();
        form.setCity("<script>alert(1)</script>");
        form.setProvince("");
        form.setPhone("");
        form.setEmail("");
        assertTrue(owners.saveProfile(ownerId, form, false, errors(form, "salonForm")));
        Salon saved = store.findByOwnerId(ownerId);
        assertEquals("", saved.getPhone());
        assertEquals("", saved.getEmail());
        mvc.perform(get("/salons/" + saved.getSlug()))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    void legacyNullContactsRenderAndCanBeUpdated() throws Exception {
        Salon salon = store.findByOwnerId(ownerId);
        salon.setCity(null);
        salon.setProvince(null);
        salon.setPhone(null);
        salon.setEmail(null);
        store.save(salon);
        mvc.perform(get("/owner/profile").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("value=\"\"")));
        mvc.perform(get("/salons/" + salon.getSlug())).andExpect(status().isOk());
        SalonForm form = profile();
        assertTrue(owners.saveProfile(ownerId, form, false, errors(form, "salonForm")));
        assertEquals("Burnaby", store.findByOwnerId(ownerId).getCity());
    }

    @Test
    void savedScheduleCanBeEditedAndReloadedWithoutMutatingItOnGet() throws Exception {
        saveHours("09:00", "17:00");
        mvc.perform(get("/owner/hours").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"09:00\"")))
                .andExpect(content().string(containsString("form=\"hours-form\"")));
        mvc.perform(post("/owner/hours").with(user(owner.getEmail()).roles("SALONOWNER")).with(csrf())
                .param("days[0].open", "true").param("days[0].startTime", "10:30")
                .param("days[0].endTime", "18:30").param("days[0].dayOfWeek", "7")
                .param("days[2].open", "true").param("days[2].startTime", "12:00")
                .param("days[2].endTime", "14:00"))
                .andExpect(redirectedUrl("/owner/hours"));
        Salon saved = store.findByOwnerId(ownerId);
        assertEquals(7, saved.getOpeningHours().size());
        assertEquals(1, saved.getOpeningHours().get(0).getDayOfWeek());
        assertEquals("10:30", saved.getOpeningHours().get(0).getStartTime());
        assertNull(saved.getOpeningHours().get(1).getStartTime());
        assertTrue(saved.getOpeningHours().get(2).isOpen());
        mvc.perform(get("/owner/hours").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(content().string(containsString("value=\"10:30\"")))
                .andExpect(content().string(containsString("value=\"18:30\"")));
        assertNull(store.findByOwnerId(ownerId).getOpeningHours().get(1).getStartTime());

        // Unchecking every day replaces, rather than appends to, the previous week.
        mvc.perform(post("/owner/hours").with(user(owner.getEmail()).roles("SALONOWNER")).with(csrf()))
                .andExpect(redirectedUrl("/owner/hours"));
        saved = store.findByOwnerId(ownerId);
        assertEquals(7, saved.getOpeningHours().size());
        assertTrue(saved.getOpeningHours().stream().noneMatch(day -> day.isOpen()));
        saveHours("11:00", "12:00");
        assertEquals("11:00", store.findByOwnerId(ownerId).getOpeningHours().get(0).getStartTime());
    }

    @Test
    void invalidRepeatScheduleRetainsPreviousWeekAndShowsErrors() throws Exception {
        saveHours("09:00", "17:00");
        for (String[] range : new String[][]{{"10:00", "10:59"}, {"17:00", "09:00"}, {"25:00", "17:00"}, {"", "17:00"}}) {
            mvc.perform(post("/owner/hours").with(user(owner.getEmail()).roles("SALONOWNER")).with(csrf())
                    .param("days[0].open", "true").param("days[0].startTime", range[0])
                    .param("days[0].endTime", range[1]))
                    .andExpect(status().isOk()).andExpect(model().attributeHasErrors("hoursForm"))
                    .andExpect(content().string(containsString("value=\"" + range[0] + "\"")))
                    .andExpect(content().string(containsString("form=\"hours-form\"")));
            assertEquals("09:00", store.findByOwnerId(ownerId).getOpeningHours().get(0).getStartTime());
        }
        HoursForm incomplete = new HoursForm();
        incomplete.getDays().remove(6);
        assertFalse(owners.setOpeningHours(ownerId, incomplete, errors(incomplete, "hoursForm")));
        HoursForm missingDay = new HoursForm();
        missingDay.getDays().set(2, null);
        assertFalse(owners.setOpeningHours(ownerId, missingDay, errors(missingDay, "hoursForm")));
        assertEquals(7, store.findByOwnerId(ownerId).getOpeningHours().size());
    }

    @Test
    void scheduleChangesKeepBookingsAndExcludeOverlapsOnShiftedGrid() {
        ServiceForm service = new ServiceForm();
        service.setName("Cut");
        service.setPrice("30");
        assertTrue(owners.addService(ownerId, service, errors(service, "serviceForm")));
        saveHours("09:00", "12:00");
        Salon salon = store.findByOwnerId(ownerId);
        Account customer = accounts.save(new Account("Customer", UUID.randomUUID() + "@example.com",
                "unused-test-hash", RoleType.CUSTOMER));
        LocalDate monday = LocalDate.of(2026, 11, 9);
        Appointment booked = bookings.bookAppointment(salon.getServices().get(0).getId(),
                salon.getSlug(), monday.toString(), "10:00", customer.getEmail());
        saveHours("09:30", "13:30");
        assertEquals(List.of("11:30", "12:30"), bookings.getAvailableTimes(salon.getSlug(), monday));
        assertEquals("10:00", appointments.findById(booked.getId()).orElseThrow().getAppointmentTime());
        HoursForm closed = new HoursForm();
        assertTrue(owners.setOpeningHours(ownerId, closed, errors(closed, "hoursForm")));
        assertTrue(bookings.getAvailableTimes(salon.getSlug(), monday).isEmpty());
        assertTrue(appointments.findById(booked.getId()).isPresent());
        saveHours("11:00", "12:00");
        assertEquals(List.of("11:00"), bookings.getAvailableTimes(salon.getSlug(), monday));
    }

    @Test
    void lateClosingDoesNotWrapSlotGenerationPastMidnight() {
        saveHours("22:00", "23:59");
        Salon salon = store.findByOwnerId(ownerId);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                assertEquals(List.of("22:00"), bookings.getAvailableTimes(salon.getSlug(), LocalDate.of(2026, 11, 9))));
    }

    private SalonForm profile() {
        SalonForm form = new SalonForm();
        form.setName("Owner Test Salon");
        form.setAddress("123 Main Street");
        form.setCity("Burnaby");
        form.setProvince("BC");
        form.setPhone("604-555-0123");
        form.setEmail("salon@example.com");
        return form;
    }

    private void saveHours(String start, String end) {
        HoursForm form = new HoursForm();
        form.getDays().get(0).setOpen(true);
        form.getDays().get(0).setStartTime(start);
        form.getDays().get(0).setEndTime(end);
        assertTrue(owners.setOpeningHours(ownerId, form, errors(form, "hoursForm")));
    }

    private BeanPropertyBindingResult errors(Object form, String name) {
        return new BeanPropertyBindingResult(form, name);
    }
}
