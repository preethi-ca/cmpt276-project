package com.cmpt276.project;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.RoleType;
import com.cmpt276.project.owner.controller.OwnerController;
import com.cmpt276.project.owner.controller.OwnerController.HoursForm;
import com.cmpt276.project.owner.controller.OwnerController.SalonForm;
import com.cmpt276.project.owner.controller.OwnerController.ServiceForm;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.owner.service.SalonAnalyticsService;
import com.cmpt276.project.owner.store.JpaSalonStore;
import com.cmpt276.project.owner.store.MemorySalonStore;
import com.cmpt276.project.owner.store.SalonStore;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.service.BookingService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.ExtendedModelMap;
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
    @Autowired private SalonAnalyticsService analytics;
    @Autowired private SalonStore store;
    @Autowired private AccountRepository accounts;
    @Autowired private AppointmentRepository appointments;
    @Autowired private BookingService bookings;
    private Account owner;
    private String ownerId;

    private void createOwnerAndSalon() {
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
    void ownerCanPublishAndRemovePhotosWithinTheFivePhotoLimit() throws Exception {
        createOwnerAndSalon();
        Salon salon = store.findByOwnerId(ownerId);
        Salon otherSalon = createAdditionalSalon("Other Gallery", "200 Main Street");
        var signedIn = user(owner.getEmail()).roles("SALONOWNER");
        String upload = "/owner/profile/photos";
        mvc.perform(get("/owner/profile").with(signedIn)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose photos")))
                .andExpect(content().string(containsString("No photos selected")));
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGMwK1gCAAIqAUsxPtcIAAAAAElFTkSuQmCC");
        var photo = new MockMultipartFile("photos", "salon.png", "image/png", png);
        mvc.perform(multipart(upload).file(photo).with(signedIn).with(csrf()))
                .andExpect(status().is3xxRedirection());
        var ids = owners.getGalleryPhotoIds(salon.getId());
        assertEquals(1, ids.size());
        String publicPath = "/salons/" + salon.getSlug() + "/photos/" + ids.get(0);
        mvc.perform(get("/salons/" + salon.getSlug())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Salon Gallery")))
                .andExpect(content().string(containsString(publicPath)));
        mvc.perform(get(publicPath)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andExpect(content().bytes(png));
        mvc.perform(get("/salons/" + otherSalon.getSlug() + "/photos/" + ids.get(0)))
                .andExpect(status().isNotFound());

        mvc.perform(multipart(upload).file(photo).file(photo).file(photo).file(photo).file(photo)
                        .with(signedIn).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("photoError"));
        assertEquals(ids, owners.getGalleryPhotoIds(salon.getId()));
        mvc.perform(multipart(upload).file(photo).file(photo).file(photo).file(photo).with(signedIn).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(5, owners.getGalleryPhotoIds(salon.getId()).size());
        mvc.perform(post(upload + "/" + ids.get(0) + "/remove").with(signedIn).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(publicPath)).andExpect(status().isNotFound());
        assertEquals(4, owners.getGalleryPhotoIds(salon.getId()).size());
    }

    @Test
    void recentSalonListShowsTheFirstThreeInVisitOrder() {
        var memory = new MemorySalonStore();
        var salons = new ArrayList<Salon>();
        for (int i = 0; i < 4; i++) {
            Salon salon = memorySalon("salon-" + i, "Salon " + i);
            memory.save(salon);
            salons.add(salon);
        }
        var orderedIds = salons.stream().map(Salon::getId).toList();
        assertEquals(salons.subList(0, 3),
                new OwnerService(memory).findRecentlyVisitedSalons(orderedIds));
    }

    @Test
    void customerCanOpenEachSalonByItsOwnUrl() throws Exception {
        createOwnerAndSalon();
        Account dashboardCustomer = createCustomer();
        Salon first = store.findByOwnerId(ownerId);
        Salon second = createAdditionalSalon(first.getName(), "200 Second Street");
        var customer = user(dashboardCustomer.getEmail()).roles("CUSTOMER");

        // Even salons with identical names have distinct public URLs.
        for (Salon salon : List.of(first, second)) {
            mvc.perform(get("/salons/" + salon.getSlug()).with(customer))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(salon.getAddress())));
        }

        mvc.perform(get("/salons/" + first.getSlug()).with(customer)).andExpect(status().isOk());
        String html = mvc.perform(get("/customer/dashboard").with(customer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String firstLink = "href=\"/salons/" + first.getSlug() + "\"";
        String secondLink = "href=\"/salons/" + second.getSlug() + "\"";
        assertTrue(html.contains(firstLink));
        assertTrue(html.indexOf(firstLink) < html.indexOf(secondLink));
        assertEquals(html.indexOf(firstLink), html.lastIndexOf(firstLink));

        var other = accounts.save(new Account("Other", UUID.randomUUID() + "@example.com",
                "unused-test-hash", RoleType.CUSTOMER));
        mvc.perform(get("/customer/dashboard").with(user(other.getEmail()).roles("CUSTOMER")))
                .andExpect(content().string(containsString("Salons you visit using their links will appear here.")))
                .andExpect(content().string(not(containsString(firstLink))));
    }

    @Test
    void jpaSalonReloadsPublicContactsServicesAndHours() {
        createOwnerAndSalon();
        ServiceForm service = new ServiceForm();
        service.setName("Haircut");
        service.setDescription("Wash and cut");
        service.setPrice("35");
        assertTrue(owners.addService(ownerId, service, errors(service, "serviceForm")));
        saveHours("09:00", "17:00");
        Salon saved = store.findByOwnerId(ownerId);

        Salon reloaded = store.findById(saved.getId());
        assertNotSame(saved, reloaded);
        assertEquals(saved.getSlug(), reloaded.getSlug());
        assertEquals("Burnaby", reloaded.getCity());
        assertEquals("BC", reloaded.getProvince());
        assertEquals("604-555-0123", reloaded.getPhone());
        assertEquals("salon@example.com", reloaded.getEmail());
        assertEquals(1, reloaded.getServices().size());
        assertEquals("Haircut", reloaded.getServices().get(0).getName());
        assertEquals("Wash and cut", reloaded.getServices().get(0).getDescription());
        assertEquals("35.00", reloaded.getServices().get(0).getPrice().toPlainString());
        assertEquals(7, reloaded.getOpeningHours().size());
        assertTrue(reloaded.getOpeningHours().get(0).isOpen());
        assertEquals("09:00", reloaded.getOpeningHours().get(0).getStartTime());
        assertEquals("17:00", reloaded.getOpeningHours().get(0).getEndTime());
    }

    @Test
    void profileUpdatesPersistAndKeepLoginIdentityAndSalonId() throws Exception {
        createOwnerAndSalon();
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
        createOwnerAndSalon();
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
        createOwnerAndSalon();
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
        createOwnerAndSalon();
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
        createOwnerAndSalon();
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
        createOwnerAndSalon();
        saveHours("09:00", "17:00");
        for (String[] range : new String[][]{{"10:00", "10:29"}, {"17:00", "09:00"}, {"25:00", "17:00"}, {"", "17:00"}}) {
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
    void ownerCanChooseOnlySupportedServiceDurations() throws Exception {
        createOwnerAndSalon();
        var signedInOwner = user(owner.getEmail()).roles("SALONOWNER");
        mvc.perform(post("/owner/services").with(signedInOwner).with(csrf())
                        .param("name", "Colour").param("price", "90").param("durationMinutes", "45"))
                .andExpect(model().attributeHasFieldErrors("serviceForm", "durationMinutes"));
        mvc.perform(post("/owner/services").with(signedInOwner).with(csrf())
                        .param("name", "Colour").param("price", "90").param("durationMinutes", "90"))
                .andExpect(redirectedUrl("/owner/services?page=1"));
        var saved = store.findByOwnerId(ownerId).getServices().get(0);
        assertEquals(90, saved.getDurationMinutes());
        mvc.perform(get("/owner/services/" + saved.getId() + "/edit").with(signedInOwner))
                .andExpect(content().string(containsString("90 minutes")));
    }

    @Test
    void scheduleChangesKeepBookingsAndExcludeOverlapsOnShiftedGrid() {
        createOwnerAndSalon();
        ServiceForm service = new ServiceForm();
        service.setName("Cut");
        service.setPrice("30");
        service.setDurationMinutes(120);
        assertTrue(owners.addService(ownerId, service, errors(service, "serviceForm")));
        saveHours("09:00", "12:00");
        Salon salon = store.findByOwnerId(ownerId);
        String serviceId = salon.getServices().get(0).getId();
        Account customer = accounts.save(new Account("Customer", UUID.randomUUID() + "@example.com",
                "unused-test-hash", RoleType.CUSTOMER));
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Appointment booked = bookings.bookAppointment(serviceId, salon.getSlug(), monday.toString(),
                "10:00", customer.getEmail());
        service.setDurationMinutes(30);
        assertTrue(owners.updateService(ownerId, serviceId, service, errors(service, "serviceForm")));
        saveHours("09:30", "13:30");
        Salon updated = store.findByOwnerId(ownerId);
        int duration = updated.getServices().get(0).getDurationMinutes();
        assertEquals(30, duration);
        assertEquals(List.of("09:30", "12:00", "12:30", "13:00"),
                bookings.getAvailableTimes(updated, monday, duration, null));
        assertEquals(120, appointments.findById(booked.getId()).orElseThrow().getDurationMinutes());
        assertThrows(IllegalArgumentException.class, () ->
                bookings.updateAppointment(booked.getId(), monday.toString(), "12:00", customer.getEmail()));

        HoursForm closed = new HoursForm();
        assertTrue(owners.setOpeningHours(ownerId, closed, errors(closed, "hoursForm")));
        assertTrue(bookings.getAvailableTimes(store.findByOwnerId(ownerId), monday, duration, null).isEmpty());
        assertTrue(appointments.findById(booked.getId()).isPresent());
    }

    @Test
    void lateClosingDoesNotWrapSlotGenerationPastMidnight() {
        createOwnerAndSalon();
        saveHours("22:00", "23:59");
        Salon salon = store.findByOwnerId(ownerId);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                assertEquals(List.of("22:00", "22:30"), bookings.getAvailableTimes(salon, LocalDate.of(2026, 11, 9), 60, null)));
    }

    private Salon memorySalon(String slug, String name) {
        Salon salon = new Salon();
        salon.setId(slug);
        salon.setOwnerId("owner-" + slug);
        salon.setSlug(slug);
        salon.setName(name);
        return salon;
    }

    private Salon createAdditionalSalon(String name, String address) {
        Account otherOwner = accounts.save(new Account("Other Owner", UUID.randomUUID() + "@example.com",
                "unused-test-hash", RoleType.SALONOWNER));
        String otherOwnerId = String.valueOf(otherOwner.getId());
        SalonForm form = profile();
        form.setName(name);
        form.setAddress(address);
        assertTrue(owners.saveProfile(otherOwnerId, form, true, errors(form, "salonForm")));
        return store.findByOwnerId(otherOwnerId);
    }

    @Test
    void customerBookingRescheduleServiceDeletionAndCancellationReachOwner() throws Exception {
        createOwnerAndSalon();
        ServiceForm service = new ServiceForm();
        service.setName("Original <Cut>");
        service.setPrice("35");
        assertTrue(owners.addService(ownerId, service, errors(service, "serviceForm")));
        saveHours("09:00", "17:00");
        Salon salon = store.findByOwnerId(ownerId);
        String serviceId = salon.getServices().get(0).getId();
        Account customer = accounts.save(new Account("Customer <Alice>", UUID.randomUUID() + "@example.com", "private-hash", RoleType.CUSTOMER));
        LocalDate monday = LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        mvc.perform(post("/customer/booking/confirm").with(csrf()).with(user(customer.getEmail()).roles("CUSTOMER"))
                        .param("salon", salon.getSlug()).param("service", serviceId)
                        .param("appointmentDate", monday.toString()).param("appointmentTime", "10:00"))
                .andExpect(status().isOk());
        var savedBookings = appointments.findByCustomer(customer);
        assertEquals(1, savedBookings.size());
        var booked = savedBookings.get(0);
        mvc.perform(get("/owner/appointments").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(status().isOk()).andExpect(model().attribute("upcomingAppointmentCount", 1L))
                .andExpect(content().string(containsString("Customer &lt;Alice&gt;")))
                .andExpect(content().string(containsString("Original &lt;Cut&gt;")))
                .andExpect(content().string(containsString(monday.toString())))
                .andExpect(content().string(not(containsString("private-hash"))));
        mvc.perform(post("/customer/appointments/edit").with(csrf()).with(user(customer.getEmail()).roles("CUSTOMER"))
                        .param("id", String.valueOf(booked.getId())).param("appointmentDate", monday.plusWeeks(1).toString())
                        .param("appointmentTime", "11:00"))
                .andExpect(redirectedUrl("/customer/appointments/details?id=" + booked.getId()));
        owners.deleteService(ownerId, serviceId);
        mvc.perform(get("/owner/appointments").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString(monday.plusWeeks(1).toString())))
                .andExpect(content().string(containsString("11:00")))
                .andExpect(content().string(containsString("Original &lt;Cut&gt;")));
        mvc.perform(post("/customer/appointments/cancel").with(csrf()).with(user(customer.getEmail()).roles("CUSTOMER"))
                        .param("id", String.valueOf(booked.getId())))
                .andExpect(redirectedUrl("/customer/appointments"));
        mvc.perform(get("/owner/appointments").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(status().isOk()).andExpect(model().attribute("upcomingAppointmentCount", 0L))
                .andExpect(model().attribute("todayAppointmentCount", 0L))
                .andExpect(content().string(containsString("No appointments to display")));
    }

    @Test
    void printableScheduleIncludesTheEntireDayForTheOwnersSalon() throws Exception {
        createOwnerAndSalon();
        Account dashboardCustomer = createCustomer();
        Salon salon = store.findByOwnerId(ownerId);
        Salon other = createAdditionalSalon("Other Salon", "200 Main Street");
        LocalDate today = LocalDate.now(ZoneId.of("America/Vancouver"));
        List<Appointment> expected = new ArrayList<>();
        for (int hour = 17; hour >= 7; hour--) {
            String time = String.format("%02d:00", hour);
            expected.add(appointments.save(new Appointment("Visit at " + time, "service", salon.getId(),
                    today, time, dashboardCustomer, null, hour == 17 ? 90 : 30)));
        }
        appointments.save(new Appointment("Other shop visit", "service", other.getId(), today, "11:00", dashboardCustomer));
        appointments.save(new Appointment("Tomorrow visit", "service", salon.getId(), today.plusDays(1), "11:00", dashboardCustomer));

        var result = mvc.perform(get("/owner/appointments/print").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visit at 17:00")))
                .andExpect(content().string(containsString("18:30")))
                .andExpect(content().string(not(containsString("Other shop visit"))))
                .andExpect(content().string(not(containsString("Tomorrow visit"))))
                .andReturn();
        var printed = (List<Appointment>) result.getModelAndView().getModel().get("appointments");
        expected.sort(Comparator.comparing(Appointment::getAppointmentTime).thenComparingInt(Appointment::getId));
        assertEquals(expected.stream().map(Appointment::getId).toList(), printed.stream().map(Appointment::getId).toList());
    }

    @Test
    void returningCustomerRequiresAnEarlierCompletedVisitAtThisSalon() {
        createOwnerAndSalon();
        Account dashboardCustomer = createCustomer();
        String salonId = store.findByOwnerId(ownerId).getId();
        String otherSalonId = createAdditionalSalon("Other Salon", "200 Main Street").getId();
        LocalDate day = LocalDate.of(2026, 11, 23);
        appointments.save(new Appointment("Cut", "cut", otherSalonId, day.minusDays(1), "09:00", dashboardCustomer));
        Appointment first = appointments.save(new Appointment("Color", "color", salonId, day,
                "10:00", dashboardCustomer, null, 120));
        Appointment second = appointments.save(new Appointment("Cut", "cut", salonId, day.plusDays(1),
                "09:00", dashboardCustomer, null, 30));
        var schedule = List.of(first, second);

        assertEquals(Set.of(), analytics.getReturningAppointmentIds(salonId, schedule, day.atTime(11, 59)));
        assertEquals(Set.of(second.getId()), analytics.getReturningAppointmentIds(salonId, schedule, day.atTime(12, 0)));
        assertEquals(Set.of(second.getId()), analytics.getReturningAppointmentIds(salonId, schedule,
                day.plusDays(1).atTime(9, 30)));
    }

    @Test
    void appointmentCountIncludesMondayThroughSunday() throws Exception {
        createOwnerAndSalon();
        String salonId = store.findByOwnerId(ownerId).getId();
        Account customer = createCustomer();
        LocalDate monday = LocalDate.now(ZoneId.of("America/Vancouver"))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        appointments.save(new Appointment("Monday morning", "cut", salonId, monday, "09:00", customer));
        appointments.save(new Appointment("Monday noon", "cut", salonId, monday, "12:00", customer));
        appointments.save(new Appointment("Sunday morning", "cut", salonId, monday.plusDays(6), "09:00", customer));
        appointments.save(new Appointment("Sunday noon", "cut", salonId, monday.plusDays(6), "12:00", customer));
        appointments.save(new Appointment("Previous week", "cut", salonId, monday.minusDays(1), "09:00", customer));
        appointments.save(new Appointment("Next week", "cut", salonId, monday.plusWeeks(1), "09:00", customer));

        mvc.perform(get("/owner/appointments").with(user(owner.getEmail()).roles("SALONOWNER")))
                .andExpect(model().attribute("weekAppointmentCount", 4L));
    }

    @Test
    void ownerSeesOnlyTheirAppointmentsInTimeOrderAcrossPages() throws Exception {
        createOwnerAndSalon();
        String salonId = store.findByOwnerId(ownerId).getId();
        Account customer = createCustomer();
        LocalDate today = LocalDate.now(ZoneId.of("America/Vancouver"));
        appointments.save(new Appointment("Previous visit", "cut", salonId, today.minusDays(1), "09:00", customer));
        // The two visits at the same time fall on opposite sides of the page boundary, in ID order.
        appointments.save(new Appointment("Tomorrow A", "cut", salonId, today.plusDays(1), "08:00", customer));
        Appointment lastVisit = appointments.save(new Appointment("Tomorrow B", "cut", salonId, today.plusDays(1), "08:00", customer));
        for (int hour = 17; hour >= 9; hour--) {
            String time = String.format("%02d:00", hour);
            appointments.save(new Appointment(time, "cut", salonId, today, time, customer));
        }
        Salon otherSalon = createAdditionalSalon("Other Salon", "200 Main Street");
        Account otherOwner = accounts.findById(Integer.valueOf(otherSalon.getOwnerId())).orElseThrow();
        appointments.save(new Appointment("Other salon visit", "cut", otherSalon.getId(), today, "09:00", customer));

        var signedIn = user(owner.getEmail()).roles("SALONOWNER");
        var first = mvc.perform(get("/owner/appointments").param("page", "-1")
                        .param("salonId", otherSalon.getId()).param("ownerId", otherSalon.getOwnerId()).with(signedIn))
                .andExpect(status().isOk()).andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("todayAppointmentCount", 9L))
                .andExpect(model().attribute("upcomingAppointmentCount", 2L))
                .andExpect(content().string(containsString("Returning customer")))
                .andReturn();
        assertEquals(List.of("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "Tomorrow A"),
                serviceNamesOnPage(first));

        var last = mvc.perform(get("/owner/appointments").param("page", "2147483647").with(signedIn))
                .andExpect(model().attribute("currentPage", 2)).andReturn();
        assertEquals(List.of("Tomorrow B"), serviceNamesOnPage(last));
        var otherPage = mvc.perform(get("/owner/appointments").with(user(otherOwner.getEmail()).roles("SALONOWNER")))
                .andExpect(model().attribute("todayAppointmentCount", 1L)).andReturn();
        assertEquals(List.of("Other salon visit"), serviceNamesOnPage(otherPage));

        appointments.deleteById(lastVisit.getId());
        mvc.perform(get("/owner/appointments").param("page", "2").with(signedIn))
                .andExpect(model().attribute("currentPage", 1));
    }

    private List<String> serviceNamesOnPage(MvcResult result) {
        var rows = (List<?>) result.getModelAndView().getModel().get("appointments");
        return rows.stream().map(Appointment.class::cast).map(Appointment::getServiceName).toList();
    }

    @Test
    void servicePageKeepsItsMenuWhileAnotherTabEditsIt(@Autowired OwnerController controller) {
        var memory = new MemorySalonStore();
        var menu = new OwnerService(memory);
        Salon salon = memorySalon("cedar", "Cedar Salon");
        memory.save(salon);
        ServiceForm service = new ServiceForm();
        service.setName("Cut");
        service.setPrice("30");
        assertTrue(menu.addService(salon.getOwnerId(), service, errors(service, "serviceForm")));
        String id = salon.getServices().get(0).getId();

        var model = new ExtendedModelMap();
        model.addAttribute("salon", salon);
        controller.services(1, "", model);

        // Another tab changes the menu after the GET has built its model but before rendering.
        service.setName("Trim");
        assertTrue(menu.updateService(salon.getOwnerId(), id, service, errors(service, "serviceForm")));
        menu.deleteService(salon.getOwnerId(), id);
        assertTrue(salon.getServices().isEmpty());
        var page = (List<?>) model.getAttribute("servicePage");
        assertEquals("Cut", ((SalonService) page.get(0)).getName());
    }

    @Test
    void overlappingOwnerEditsPreserveProfileMenuAndHours(@Autowired PlatformTransactionManager transactions) {
        createOwnerAndSalon();
        var secondInstance = new OwnerService(store);
        var otherTransaction = new TransactionTemplate(transactions);
        otherTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        new TransactionTemplate(transactions).executeWithoutResult(first -> {
            assertTrue(store.findByOwnerId(ownerId).getServices().isEmpty());
            otherTransaction.executeWithoutResult(second -> {
                var service = new ServiceForm();
                service.setName("Haircut");
                service.setPrice("35");
                assertTrue(owners.addService(ownerId, service, errors(service, "serviceForm")));
                saveHours("09:00", "17:00");
            });
            var changed = profile();
            changed.setCity("Richmond");
            assertTrue(secondInstance.saveProfile(ownerId, changed, false, errors(changed, "salonForm")));
        });

        Salon saved = store.findByOwnerId(ownerId);
        assertEquals("Richmond", saved.getCity());
        assertEquals(List.of("Haircut"), saved.getServices().stream().map(item -> item.getName()).toList());
        assertEquals("09:00", saved.getOpeningHours().get(0).getStartTime());
    }

    private Account createCustomer() {
        return accounts.save(new Account("Customer", UUID.randomUUID() + "@example.com",
                "unused-test-hash", RoleType.CUSTOMER));
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
