package com.cmpt276.project.owner.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

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

}
