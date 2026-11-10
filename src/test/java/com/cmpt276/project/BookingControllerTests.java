package com.cmpt276.project;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.service.BookingService;


@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private OwnerService ownerService;

    @Test
    void shouldShowBookingDatePage() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(salon.getOpeningHours())
                .thenReturn(List.of());

        mockMvc.perform(get("/customer/booking")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-date"))
                .andExpect(model().attribute("service", "service1"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("salon", "magiccuts-salon"))
                .andExpect(model().attributeExists("openingHours"))
                .andExpect(model().attributeExists("days"))
                .andExpect(model().attributeExists("minDate"));
    }

    @Test
    void shouldReturnToDatePageWhenSalonIsClosed() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);
        OpeningDay monday = mock(OpeningDay.class);
        String appointmentDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toString();

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(monday.getDayOfWeek())
                .thenReturn(1);

        when(monday.isOpen())
                .thenReturn(false);

        when(salon.getOpeningHours())
                .thenReturn(List.of(monday));

        mockMvc.perform(get("/customer/booking/time")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", appointmentDate))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-date"))
                .andExpect(model().attribute(
                        "error",
                        "Sorry, salon is closed on the selected date"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attributeExists("openingHours"))
                .andExpect(model().attributeExists("days"));
    }

    @Test
    void shouldShowAvailableTimesWhenSalonIsOpen() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);
        OpeningDay monday = mock(OpeningDay.class);
        String appointmentDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toString();

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(monday.getDayOfWeek())
                .thenReturn(1);

        when(monday.isOpen())
                .thenReturn(true);

        when(salon.getOpeningHours())
                .thenReturn(List.of(monday));

        when(bookingService.getAvailableTimes(
                "magiccuts-salon",
                LocalDate.parse(appointmentDate)))
                .thenReturn(List.of("09:00", "10:00"));

        mockMvc.perform(get("/customer/booking/time")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", appointmentDate))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-time"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("appointmentDate", appointmentDate))
                .andExpect(model().attribute(
                        "availableTimes",
                        List.of("09:00", "10:00")));
    }

    @Test
    void shouldShowBookingSummary() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);
        String appointmentDate = LocalDate.now().plusDays(2).toString();

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(salon.getName())
                .thenReturn("Magic Cuts");

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        mockMvc.perform(get("/customer/booking/confirmation")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", appointmentDate)
                .param("appointmentTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/booking-summary"))
                .andExpect(model().attribute("salonName", "Magic Cuts"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("appointmentDate", appointmentDate))
                .andExpect(model().attribute("appointmentTime", "10:00"))
                .andExpect(model().attribute("bookedService", service));
    }

    @Test
    void shouldConfirmBookingSuccessfully() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);
        when(ownerService.findPublicSalon("salon1"))
        .thenReturn(salon);
        when(salon.getId())
                .thenReturn("salon1");
        when(salon.getSlug())
                .thenReturn("magiccuts-salon");
        when(salon.getName()).thenReturn("Magic Cuts");

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");
        when(service.getName())
                .thenReturn("Haircut");
        /*Tests that the controller correctly handles a successful booking by simulating the booking service returning an appointment 
        and checking that the customer is redirected to the booking confirmation page with the correct details */
        String appointmentDate = LocalDate.now().plusDays(2).toString();
        Appointment appointment = mock(Appointment.class);
        when(appointment.getServiceId()).thenReturn("service1");
        when(appointment.getServiceName()).thenReturn("Haircut");
        when(appointment.getAppointmentDate()).thenReturn(
                LocalDate.parse(appointmentDate));
        when(appointment.getAppointmentTime()).thenReturn("10:00");
        when(appointment.getSalonId()).thenReturn("salon1");

        when(bookingService.bookAppointment(
                "service1",
                "magiccuts-salon",
                appointmentDate,
                "10:00",
                "customer@test.com"))
                .thenReturn(appointment); 

        mockMvc.perform(post("/customer/booking/confirm")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .with(csrf())
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", appointmentDate)
                .param("appointmentTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/booking-confirmation"))
                .andExpect(model().attribute("salonName", "Magic Cuts"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute("appointmentDate", LocalDate.parse(appointmentDate)))
                .andExpect(model().attribute("appointmentTime", "10:00"))
                .andExpect(model().attribute("bookedService", service));
    }

    @Test
    void shouldReturnToTimePageWhenBookingFails() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);
        String appointmentDate = LocalDate.now().plusDays(2).toString();

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        when(bookingService.getAvailableTimes(
                "magiccuts-salon",
                LocalDate.parse(appointmentDate)))
                .thenReturn(List.of("09:00", "10:00"));

        doThrow(new IllegalArgumentException("Appointment time is no longer available"))
                .when(bookingService)
                .bookAppointment(
                        "service1",
                        "magiccuts-salon",
                        appointmentDate,
                        "10:00",
                        "customer@test.com");

        mockMvc.perform(post("/customer/booking/confirm")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .with(csrf())
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", appointmentDate)
                .param("appointmentTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-time"))
                .andExpect(model().attribute(
                        "error",
                        "Appointment time is no longer available"))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attribute(
                        "availableTimes",
                        List.of("09:00", "10:00")));
    }

    @Test
    void shouldReturnToDatePageWhenBookingDateIsInPast() throws Exception {

        Salon salon = mock(Salon.class);
        SalonService service = mock(SalonService.class);
        String pastDate = LocalDate.now().minusDays(1).toString();

        when(ownerService.findPublicSalonBySlug("magiccuts-salon"))
                .thenReturn(salon);

        when(salon.getServices())
                .thenReturn(List.of(service));

        when(service.getId())
                .thenReturn("service1");

        when(service.getName())
                .thenReturn("Haircut");

        doThrow(new IllegalArgumentException("Sorry, this date has already passed."))
                .when(bookingService)
                .bookAppointment(
                        "service1",
                        "magiccuts-salon",
                        pastDate,
                        "10:00",
                        "customer@test.com");

        mockMvc.perform(post("/customer/booking/confirm")
                .with(user("customer@test.com").roles("CUSTOMER"))
                .with(csrf())
                .param("service", "service1")
                .param("salon", "magiccuts-salon")
                .param("appointmentDate", pastDate)
                .param("appointmentTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/select-date"))
                .andExpect(model().attribute("error", "Sorry, this date has already passed."))
                .andExpect(model().attribute("serviceName", "Haircut"))
                .andExpect(model().attributeExists("minDate"));
    }
}