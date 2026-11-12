package com.cmpt276.project.service;

import com.cmpt276.project.owner.service.SalonAnalyticsService;
import com.cmpt276.project.repository.AppointmentRepository;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cmpt276.project.model.Appointment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
public class SalonAnalyticsServiceTests {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private SalonAnalyticsService analyticsService;

    @Test
    void shouldReturnTotalAppointments() {
        Appointment appointment1 = new Appointment();
        Appointment appointment2 = new Appointment();

        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of(appointment1, appointment2));

        int result = analyticsService.getTotalAppointments("salon1");

        assertEquals(2, result);
    }

    @Test
    void shouldReturnZeroWhenNoAppointments() {
        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of());

        int result = analyticsService.getTotalAppointments("salon1");

        assertEquals(0, result);
    }

    @Test
    void shouldReturnMostPopularService() {
        Appointment haircut1 = new Appointment(
                "Haircut", "service1", "salon1", null, null, null);

        Appointment haircut2 = new Appointment(
                "Haircut", "service1", "salon1", null, null, null);

        Appointment color = new Appointment(
                "Color", "service2", "salon1", null, null, null);

        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of(haircut1, haircut2, color));

        String result = analyticsService.getMostPopularService("salon1");

        assertEquals("Haircut", result);
    }

    @Test
    void shouldReturnNoAppointmentsWhenNoServicesBooked() {
        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of());

        String result = analyticsService.getMostPopularService("salon1");

        assertEquals("No appointments", result);
    }

    @Test
    void shouldReturnBusiestTime() {
        Appointment morning1 = new Appointment(
                "Haircut", "service1", "salon1", null, "10:00", null);

        Appointment morning2 = new Appointment(
                "Color", "service2", "salon1", null, "10:00", null);

        Appointment afternoon = new Appointment(
                "Haircut", "service1", "salon1", null, "14:00", null);

        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of(morning1, morning2, afternoon));

        String result = analyticsService.getBusiestTime("salon1");

        assertEquals("10:00", result);
    }

    @Test
    void shouldReturnNoAppointmentsWhenNoTimesBooked() {
        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of());

        String result = analyticsService.getBusiestTime("salon1");

        assertEquals("No appointments", result);
    }

    @Test
    void shouldReturnEstimatedRevenue() {
        SalonService haircut = new SalonService();
        haircut.setId("service1");
        haircut.setPrice(new BigDecimal("25.00"));

        SalonService color = new SalonService();
        color.setId("service2");
        color.setPrice(new BigDecimal("40.00"));

        Salon salon = new Salon();
        salon.setId("salon1");
        salon.setServices(List.of(haircut, color));

        Appointment appointment1 = new Appointment(
                "Haircut", "service1", "salon1", null, null, null);

        Appointment appointment2 = new Appointment(
                "Haircut", "service1", "salon1", null, null, null);

        Appointment appointment3 = new Appointment(
                "Color", "service2", "salon1", null, null, null);

        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of(appointment1, appointment2, appointment3));

        BigDecimal result = analyticsService.getEstimatedRevenue(salon);

        assertEquals(new BigDecimal("90.00"), result);
    }

    @Test
    void shouldReturnZeroRevenueWhenNoAppointments() {
        Salon salon = new Salon();
        salon.setId("salon1");
        salon.setServices(List.of());

        when(appointmentRepository.findBySalonId("salon1"))
                .thenReturn(List.of());

        BigDecimal result = analyticsService.getEstimatedRevenue(salon);

        assertEquals(BigDecimal.ZERO, result);
    }

}
