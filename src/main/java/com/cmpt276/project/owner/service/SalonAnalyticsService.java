package com.cmpt276.project.owner.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
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
            for (SalonService service : salon.getServices()) {
                if (service.getId().equals(appointment.getServiceId())) {
                    totalRevenue = totalRevenue.add(service.getPrice());
                    break;
                }
            }
        }
        return totalRevenue;

    }

}
