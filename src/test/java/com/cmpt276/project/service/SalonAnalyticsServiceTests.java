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
import java.time.LocalDate;
import java.util.Map;

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
                                "Haircut", "service1", "salon1", null, null, null, BigDecimal.ZERO);

                Appointment haircut2 = new Appointment(
                                "Haircut", "service1", "salon1", null, null, null, BigDecimal.ZERO);

                Appointment color = new Appointment(
                                "Color", "service2", "salon1", null, null, null, BigDecimal.ZERO);

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
                                "Haircut", "service1", "salon1", null, "10:00", null, BigDecimal.ZERO);

                Appointment morning2 = new Appointment(
                                "Color", "service2", "salon1", null, "10:00", null, BigDecimal.ZERO);

                Appointment afternoon = new Appointment(
                                "Haircut", "service1", "salon1", null, "14:00", null, BigDecimal.ZERO);

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
                                "Haircut", "service1", "salon1", null, null, null,
                                new BigDecimal("25.00"));

                Appointment appointment2 = new Appointment(
                                "Haircut", "service1", "salon1", null, null, null,
                                new BigDecimal("25.00"));

                Appointment appointment3 = new Appointment(
                                "Color", "service2", "salon1", null, null, null,
                                new BigDecimal("40.00"));

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

        @Test
        void shouldReturnBusiestDay() {
                Appointment monday = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 23),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment saturday1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 28),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment saturday2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 28),
                                "14:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonId("salon1"))
                                .thenReturn(List.of(monday, saturday1, saturday2));

                String result = analyticsService.getBusiestDay("salon1");

                assertEquals("Saturday", result);
        }

        @Test
        void shouldReturnNoAppointmentsWhenNoDaysBooked() {
                when(appointmentRepository.findBySalonId("salon1"))
                                .thenReturn(List.of());

                String result = analyticsService.getBusiestDay("salon1");

                assertEquals("No appointments", result);
        }

        @Test
        void shouldReturnTotalAppointmentsForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                when(appointmentRepository
                                .countBySalonIdAndAppointmentDateBetween(
                                                "salon1",
                                                startDate,
                                                endDate))
                                .thenReturn(3L);

                int result = analyticsService.getTotalAppointments(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(3, result);
        }

        @Test
        void shouldReturnMostPopularServiceForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Appointment haircut1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 5),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment haircut2 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 10),
                                "11:00",
                                null,
                                BigDecimal.ZERO);

                Appointment color = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 15),
                                "12:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(haircut1, haircut2, color));

                String result = analyticsService.getMostPopularService(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals("Haircut", result);
        }

        @Test
        void shouldReturnBusiestTimeForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Appointment morning1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 5),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment morning2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 10),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment afternoon = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 15),
                                "14:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(morning1, morning2, afternoon));

                String result = analyticsService.getBusiestTime(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals("10:00", result);
        }

        @Test
        void shouldReturnBusiestDayForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Appointment monday = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 2),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment saturday1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 7),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment saturday2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 14),
                                "14:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(monday, saturday1, saturday2));

                String result = analyticsService.getBusiestDay(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals("Saturday", result);
        }

        @Test
        void shouldReturnEstimatedRevenueForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Salon salon = new Salon();
                salon.setId("salon1");

                Appointment appointment1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 5),
                                "10:00",
                                null,
                                new BigDecimal("25.00"));

                Appointment appointment2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 10),
                                "14:00",
                                null,
                                new BigDecimal("40.00"));

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(appointment1, appointment2));

                BigDecimal result = analyticsService.getEstimatedRevenue(
                                salon,
                                startDate,
                                endDate);

                assertEquals(new BigDecimal("65.00"), result);
        }

        @Test
        void shouldReturnBookingTrendsForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 4);

                Appointment appointment1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 1),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment appointment2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 1),
                                "14:00",
                                null,
                                BigDecimal.ZERO);

                Appointment appointment3 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 3),
                                "11:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(
                                                appointment1,
                                                appointment2,
                                                appointment3));

                Map<LocalDate, Integer> result = analyticsService.getBookingTrends(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(4, result.size());
                assertEquals(2, result.get(LocalDate.of(2026, 11, 1)));
                assertEquals(0, result.get(LocalDate.of(2026, 11, 2)));
                assertEquals(1, result.get(LocalDate.of(2026, 11, 3)));
                assertEquals(0, result.get(LocalDate.of(2026, 11, 4)));
        }

        @Test
        void shouldReturnWeeklyBookingTrendsForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 14);

                Appointment appointment1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 2),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment appointment2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 5),
                                "14:00",
                                null,
                                BigDecimal.ZERO);

                Appointment appointment3 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 10),
                                "11:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(
                                                appointment1,
                                                appointment2,
                                                appointment3));

                Map<String, Integer> result = analyticsService.getWeeklyBookingTrends(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(2, result.get("Nov 2 – Nov 8"));
                assertEquals(1, result.get("Nov 9 – Nov 14"));
        }

        @Test
        void shouldReturnRevenueTrendsForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 4);

                Appointment appointment1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 1),
                                "10:00",
                                null,
                                new BigDecimal("25.00"));

                Appointment appointment2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 1),
                                "14:00",
                                null,
                                new BigDecimal("40.00"));

                Appointment appointment3 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 3),
                                "11:00",
                                null,
                                new BigDecimal("30.00"));

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(
                                                appointment1,
                                                appointment2,
                                                appointment3));

                Map<LocalDate, BigDecimal> result = analyticsService.getRevenueTrends(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(new BigDecimal("65.00"),
                                result.get(LocalDate.of(2026, 11, 1)));

                assertEquals(BigDecimal.ZERO,
                                result.get(LocalDate.of(2026, 11, 2)));

                assertEquals(new BigDecimal("30.00"),
                                result.get(LocalDate.of(2026, 11, 3)));

                assertEquals(BigDecimal.ZERO,
                                result.get(LocalDate.of(2026, 11, 4)));
        }

        @Test
        void shouldReturnWeeklyRevenueTrendsForDateRange() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 14);

                Appointment appointment1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 2),
                                "10:00",
                                null,
                                new BigDecimal("25.00"));

                Appointment appointment2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 5),
                                "14:00",
                                null,
                                new BigDecimal("40.00"));

                Appointment appointment3 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 10),
                                "11:00",
                                null,
                                new BigDecimal("30.00"));

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(
                                                appointment1,
                                                appointment2,
                                                appointment3));

                Map<String, BigDecimal> result = analyticsService.getWeeklyRevenueTrends(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(
                                new BigDecimal("65.00"),
                                result.get("Nov 2 – Nov 8"));

                assertEquals(
                                new BigDecimal("30.00"),
                                result.get("Nov 9 – Nov 14"));
        }

        @Test
        void shouldReturnBusiestDayInsightWhenOneDayDominates() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Appointment saturday1 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 7),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                Appointment saturday2 = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 14),
                                "14:00",
                                null,
                                BigDecimal.ZERO);

                Appointment saturday3 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 21),
                                "11:00",
                                null,
                                BigDecimal.ZERO);

                Appointment saturday4 = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 28),
                                "12:00",
                                null,
                                BigDecimal.ZERO);

                Appointment monday = new Appointment(
                                "Color",
                                "service2",
                                "salon1",
                                LocalDate.of(2026, 11, 2),
                                "09:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(
                                                saturday1,
                                                saturday2,
                                                saturday3,
                                                saturday4,
                                                monday));

                String result = analyticsService.getBusiestDayInsight(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(
                                "Saturday accounts for 80% of appointments. Consider adding more availability on Saturday.",
                                result);
        }

        @Test
        void shouldReturnNoInsightWhenThereAreNoAppointments() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of());

                String result = analyticsService.getBusiestDayInsight(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(
                                "Not enough booking data for an insight yet.",
                                result);
        }

        @Test
        void shouldReturnPopularServiceInsightWhenOneServiceDominates() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Appointment haircut1 = new Appointment(
                                "Haircut", "service1", "salon1",
                                LocalDate.of(2026, 11, 2),
                                "10:00", null, BigDecimal.ZERO);

                Appointment haircut2 = new Appointment(
                                "Haircut", "service1", "salon1",
                                LocalDate.of(2026, 11, 5),
                                "11:00", null, BigDecimal.ZERO);

                Appointment haircut3 = new Appointment(
                                "Haircut", "service1", "salon1",
                                LocalDate.of(2026, 11, 8),
                                "12:00", null, BigDecimal.ZERO);

                Appointment haircut4 = new Appointment(
                                "Haircut", "service1", "salon1",
                                LocalDate.of(2026, 11, 12),
                                "13:00", null, BigDecimal.ZERO);

                Appointment color = new Appointment(
                                "Color", "service2", "salon1",
                                LocalDate.of(2026, 11, 10),
                                "14:00", null, BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(
                                                haircut1,
                                                haircut2,
                                                haircut3,
                                                haircut4,
                                                color));

                String result = analyticsService.getPopularServiceInsight(
                                "salon1",
                                startDate,
                                endDate);

                assertEquals(
                                "Haircut makes up 80% of appointments. Consider highlighting this service in promotions.",
                                result);
        }

        @Test
        void shouldNotGenerateInsightsWithFewerThanFiveAppointments() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Appointment appointment = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 7),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(appointment));

                assertEquals(
                                "Not enough booking data for an insight yet.",
                                analyticsService.getBusiestDayInsight(
                                                "salon1", startDate, endDate));

                assertEquals(
                                "Not enough booking data for an insight yet.",
                                analyticsService.getPopularServiceInsight(
                                                "salon1", startDate, endDate));
        }

        @Test
        void shouldNotGenerateInsightsWithFourAppointments() {
                LocalDate startDate = LocalDate.of(2026, 11, 1);
                LocalDate endDate = LocalDate.of(2026, 11, 30);

                Appointment appointment = new Appointment(
                                "Haircut",
                                "service1",
                                "salon1",
                                LocalDate.of(2026, 11, 7),
                                "10:00",
                                null,
                                BigDecimal.ZERO);

                when(appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                                "salon1",
                                startDate,
                                endDate))
                                .thenReturn(List.of(
                                                appointment,
                                                appointment,
                                                appointment,
                                                appointment));

                assertEquals(
                                "Not enough booking data for an insight yet.",
                                analyticsService.getBusiestDayInsight(
                                                "salon1", startDate, endDate));

                assertEquals(
                                "Not enough booking data for an insight yet.",
                                analyticsService.getPopularServiceInsight(
                                                "salon1", startDate, endDate));
        }

        @Test
        void shouldReturnBookingGrowthInsight() {
                LocalDate startDate = LocalDate.of(2026, 11, 8);
                LocalDate endDate = LocalDate.of(2026, 11, 14);

                when(appointmentRepository.countBySalonIdAndAppointmentDateBetween(
                                "salon1", startDate, endDate))
                                .thenReturn(15L);

                when(appointmentRepository.countBySalonIdAndAppointmentDateBetween(
                                "salon1", LocalDate.of(2026, 11, 1),
                                LocalDate.of(2026, 11, 7)))
                                .thenReturn(10L);

                assertEquals(
                                "Bookings increased by 50% compared to the previous period.",
                                analyticsService.getBookingTrendInsight(
                                                "salon1", startDate, endDate));
        }

        @Test
        void shouldReturnBookingDeclineInsight() {
                LocalDate startDate = LocalDate.of(2026, 11, 8);
                LocalDate endDate = LocalDate.of(2026, 11, 14);

                when(appointmentRepository.countBySalonIdAndAppointmentDateBetween(
                                "salon1", startDate, endDate))
                                .thenReturn(5L);

                when(appointmentRepository.countBySalonIdAndAppointmentDateBetween(
                                "salon1", LocalDate.of(2026, 11, 1),
                                LocalDate.of(2026, 11, 7)))
                                .thenReturn(10L);

                assertEquals(
                                "Bookings decreased by 50% compared to the previous period. Consider running a promotion.",
                                analyticsService.getBookingTrendInsight(
                                                "salon1", startDate, endDate));
        }

        @Test
        void shouldReturnInsufficientBookingHistory() {
                LocalDate startDate = LocalDate.of(2026, 11, 8);
                LocalDate endDate = LocalDate.of(2026, 11, 14);

                when(appointmentRepository.countBySalonIdAndAppointmentDateBetween(
                                "salon1", startDate, endDate))
                                .thenReturn(10L);

                when(appointmentRepository.countBySalonIdAndAppointmentDateBetween(
                                "salon1", LocalDate.of(2026, 11, 1),
                                LocalDate.of(2026, 11, 7)))
                                .thenReturn(0L);

                assertEquals(
                                "Not enough booking history to compare periods.",
                                analyticsService.getBookingTrendInsight(
                                                "salon1", startDate, endDate));
        }

}
