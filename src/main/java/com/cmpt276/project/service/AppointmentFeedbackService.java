package com.cmpt276.project.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AppointmentFeedbackRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class AppointmentFeedbackService {
    private final AppointmentFeedbackRepository feedbackRepository;

    public AppointmentFeedbackService(AppointmentFeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public boolean isOver(Appointment appointment) {
        return isOver(appointment, LocalDateTime.now(ZoneId.of("America/Vancouver")));
    }

    boolean isOver(Appointment appointment, LocalDateTime now) {
        LocalTime time = LocalTime.parse(appointment.getAppointmentTime());
        // appointments are always one hour
        LocalDateTime end = appointment.getAppointmentDate().atTime(time).plusHours(1);
        return end.isBefore(now);
    }
}
