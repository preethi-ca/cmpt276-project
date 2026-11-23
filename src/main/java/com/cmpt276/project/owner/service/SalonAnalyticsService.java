package com.cmpt276.project.owner.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import com.cmpt276.project.owner.model.Salon;
import java.util.List;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.time.temporal.WeekFields;

@Service
public class SalonAnalyticsService {

    private final AppointmentRepository appointmentRepository;

    public SalonAnalyticsService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public int getTotalAppointments(String salonId) {
        List<Appointment> appointments = appointmentRepository.findBySalonId(salonId);
        return appointments.size();
    }

    public int getTotalAppointments(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        return (int) appointmentRepository
                .countBySalonIdAndAppointmentDateBetween(
                        salonId,
                        startDate,
                        endDate);
    }

    public String getMostPopularService(String salonId) {
        List<Appointment> appointments = appointmentRepository.findBySalonId(salonId);
        Map<String, Integer> serviceCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            String serviceName = appointment.getServiceName();

            serviceCounts.put(
                    serviceName,
                    serviceCounts.getOrDefault(serviceName, 0) + 1);
        }

        String mostPopularService = "No appointments";
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry : serviceCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                highestCount = entry.getValue();
                mostPopularService = entry.getKey();
            }
        }
        return mostPopularService;

    }

    public String getMostPopularService(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        Map<String, Integer> serviceCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            String serviceName = appointment.getServiceName();

            serviceCounts.put(
                    serviceName,
                    serviceCounts.getOrDefault(serviceName, 0) + 1);
        }

        String mostPopularService = "No appointments";
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry : serviceCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                highestCount = entry.getValue();
                mostPopularService = entry.getKey();
            }
        }

        return mostPopularService;
    }

    public String getBusiestTime(String salonId) {
        List<Appointment> appointments = appointmentRepository.findBySalonId(salonId);
        Map<String, Integer> timeCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            String appointmentTime = appointment.getAppointmentTime();

            timeCounts.put(
                    appointmentTime,
                    timeCounts.getOrDefault(appointmentTime, 0) + 1);
        }

        String busiestTime = "No appointments";
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry : timeCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                highestCount = entry.getValue();
                busiestTime = entry.getKey();
            }
        }
        return busiestTime;

    }

    public String getBusiestTime(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        Map<String, Integer> timeCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            String appointmentTime = appointment.getAppointmentTime();

            timeCounts.put(
                    appointmentTime,
                    timeCounts.getOrDefault(appointmentTime, 0) + 1);
        }

        String busiestTime = "No appointments";
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry : timeCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                highestCount = entry.getValue();
                busiestTime = entry.getKey();
            }
        }

        return busiestTime;
    }

    public String getBusiestDay(String salonId) {
        List<Appointment> appointments = appointmentRepository.findBySalonId(salonId);
        Map<DayOfWeek, Integer> dayCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentDate() != null) {
                DayOfWeek day = appointment.getAppointmentDate().getDayOfWeek();

                dayCounts.put(
                        day,
                        dayCounts.getOrDefault(day, 0) + 1);
            }
        }

        DayOfWeek busiestDay = null;
        int highestCount = 0;

        for (Map.Entry<DayOfWeek, Integer> entry : dayCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                highestCount = entry.getValue();
                busiestDay = entry.getKey();
            }
        }

        if (busiestDay == null) {
            return "No appointments";
        }

        return busiestDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public String getBusiestDay(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        Map<DayOfWeek, Integer> dayCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentDate() != null) {
                DayOfWeek day = appointment.getAppointmentDate().getDayOfWeek();

                dayCounts.put(
                        day,
                        dayCounts.getOrDefault(day, 0) + 1);
            }
        }

        DayOfWeek busiestDay = null;
        int highestCount = 0;

        for (Map.Entry<DayOfWeek, Integer> entry : dayCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                highestCount = entry.getValue();
                busiestDay = entry.getKey();
            }
        }

        if (busiestDay == null) {
            return "No appointments";
        }

        return busiestDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public Map<LocalDate, Integer> getBookingTrends(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        Map<LocalDate, Integer> bookingTrends = new LinkedHashMap<>();

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            bookingTrends.put(currentDate, 0);
            currentDate = currentDate.plusDays(1);
        }

        for (Appointment appointment : appointments) {
            LocalDate appointmentDate = appointment.getAppointmentDate();

            if (appointmentDate != null) {
                bookingTrends.put(
                        appointmentDate,
                        bookingTrends.getOrDefault(appointmentDate, 0) + 1);
            }
        }

        return bookingTrends;
    }

    public Map<LocalDate, BigDecimal> getRevenueTrends(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        Map<LocalDate, BigDecimal> revenueTrends = new LinkedHashMap<>();

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            revenueTrends.put(currentDate, BigDecimal.ZERO);
            currentDate = currentDate.plusDays(1);
        }

        for (Appointment appointment : appointments) {
            LocalDate appointmentDate = appointment.getAppointmentDate();

            if (appointmentDate != null && appointment.getBookedPrice() != null) {
                revenueTrends.put(
                        appointmentDate,
                        revenueTrends.getOrDefault(
                                appointmentDate,
                                BigDecimal.ZERO)
                                .add(appointment.getBookedPrice()));
            }
        }

        return revenueTrends;
    }

    public Map<String, BigDecimal> getWeeklyRevenueTrends(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        Map<String, BigDecimal> weeklyRevenue = new LinkedHashMap<>();

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String weekLabel = "Week " +
                    currentDate.get(WeekFields.ISO.weekOfWeekBasedYear());

            weeklyRevenue.putIfAbsent(weekLabel, BigDecimal.ZERO);

            currentDate = currentDate.plusDays(1);
        }

        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentDate() != null
                    && appointment.getBookedPrice() != null) {

                String weekLabel = "Week " +
                        appointment.getAppointmentDate()
                                .get(WeekFields.ISO.weekOfWeekBasedYear());

                weeklyRevenue.put(
                        weekLabel,
                        weeklyRevenue.getOrDefault(
                                weekLabel,
                                BigDecimal.ZERO)
                                .add(appointment.getBookedPrice()));
            }
        }

        return weeklyRevenue;
    }

    public Map<String, Integer> getWeeklyBookingTrends(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        Map<String, Integer> weeklyTrends = new LinkedHashMap<>();

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String weekLabel = "Week " +
                    currentDate.get(WeekFields.ISO.weekOfWeekBasedYear());

            weeklyTrends.putIfAbsent(weekLabel, 0);

            currentDate = currentDate.plusDays(1);
        }

        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentDate() != null) {

                String weekLabel = "Week " +
                        appointment.getAppointmentDate()
                                .get(WeekFields.ISO.weekOfWeekBasedYear());

                weeklyTrends.put(
                        weekLabel,
                        weeklyTrends.getOrDefault(weekLabel, 0) + 1);
            }
        }

        return weeklyTrends;
    }

    public BigDecimal getEstimatedRevenue(Salon salon) {
        List<Appointment> appointments = appointmentRepository.findBySalonId(salon.getId());

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Appointment appointment : appointments) {
            if (appointment.getBookedPrice() != null) {
                totalRevenue = totalRevenue.add(appointment.getBookedPrice());
            }
        }

        return totalRevenue;
    }

    public BigDecimal getEstimatedRevenue(
            Salon salon,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salon.getId(),
                startDate,
                endDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Appointment appointment : appointments) {
            if (appointment.getBookedPrice() != null) {
                totalRevenue = totalRevenue.add(appointment.getBookedPrice());
            }
        }

        return totalRevenue;
    }

    public String getBusiestDayInsight(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        if (appointments.isEmpty()) {
            return "Not enough booking data for an insight yet.";
        }

        Map<DayOfWeek, Integer> dayCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentDate() != null) {
                DayOfWeek day = appointment.getAppointmentDate().getDayOfWeek();

                dayCounts.put(
                        day,
                        dayCounts.getOrDefault(day, 0) + 1);
            }
        }

        DayOfWeek busiestDay = null;
        int highestCount = 0;

        for (Map.Entry<DayOfWeek, Integer> entry : dayCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                busiestDay = entry.getKey();
                highestCount = entry.getValue();
            }
        }

        if (busiestDay == null) {
            return "Not enough booking data for an insight yet.";
        }

        double percentage = (highestCount * 100.0) / appointments.size();

        if (percentage < 40.0) {
            return "Bookings are fairly evenly distributed across the week.";
        }

        String dayName = busiestDay.getDisplayName(
                TextStyle.FULL,
                Locale.ENGLISH);

        return dayName + " accounts for "
                + Math.round(percentage)
                + "% of appointments. Consider adding more availability on "
                + dayName + ".";
    }

    public String getPopularServiceInsight(
            String salonId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Appointment> appointments = appointmentRepository.findBySalonIdAndAppointmentDateBetween(
                salonId,
                startDate,
                endDate);

        if (appointments.isEmpty()) {
            return "Not enough booking data for an insight yet.";
        }

        Map<String, Integer> serviceCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            String serviceName = appointment.getServiceName();

            serviceCounts.put(
                    serviceName,
                    serviceCounts.getOrDefault(serviceName, 0) + 1);
        }

        String topService = null;
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry : serviceCounts.entrySet()) {
            if (entry.getValue() > highestCount) {
                topService = entry.getKey();
                highestCount = entry.getValue();
            }
        }

        if (topService == null) {
            return "Not enough booking data for an insight yet.";
        }

        double percentage = (highestCount * 100.0) / appointments.size();

        if (percentage < 40.0) {
            return "Bookings are spread across multiple services.";
        }

        return topService + " makes up "
                + Math.round(percentage)
                + "% of appointments. Consider highlighting this service in promotions.";
    }

}
