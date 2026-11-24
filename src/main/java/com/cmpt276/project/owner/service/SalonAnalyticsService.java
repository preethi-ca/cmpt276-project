package com.cmpt276.project.owner.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import java.util.List;

@Service
public class SalonAnalyticsService {

    private final AppointmentRepository appointmentRepository;

    public SalonAnalyticsService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Set<Integer> getReturningAppointmentIds(String salonId, List<Appointment> schedule, LocalDateTime now) {
        if (schedule.isEmpty()) {
            return Set.of();
        }
        var customerIds = schedule.stream().map(visit -> visit.getCustomer().getId()).distinct().toList();
        var history = appointmentRepository.findBySalonIdAndCustomer_IdInAndAppointmentDateLessThanEqual(
                salonId, customerIds, now.toLocalDate());
        Map<Integer, LocalDateTime> firstCompletedEnd = new HashMap<>();
        for (Appointment visit : history) {
            LocalDateTime end = visit.getEndDateTime();
            if (!end.isAfter(now)) {
                firstCompletedEnd.merge(visit.getCustomer().getId(), end,
                        (first, other) -> first.isBefore(other) ? first : other);
            }
        }

        // A completed visit counts only for later appointments, never for its own row.
        Set<Integer> returning = new HashSet<>();
        for (Appointment visit : schedule) {
            LocalDateTime firstEnd = firstCompletedEnd.get(visit.getCustomer().getId());
            if (firstEnd != null && !firstEnd.isAfter(visit.getStartDateTime())) {
                returning.add(visit.getId());
            }
        }
        return returning;
    }

    public int getTotalAppointments(String salonId) {
        List<Appointment> appointments = appointmentRepository.findBySalonId(salonId);
        return appointments.size();
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

}
