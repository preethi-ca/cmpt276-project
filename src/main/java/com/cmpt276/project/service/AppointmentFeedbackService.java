package com.cmpt276.project.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.AppointmentFeedback;
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

    public boolean hasFeedback(int appointmentId) {
        return feedbackRepository.existsByAppointmentId(appointmentId);
    }

    public AppointmentFeedback saveFeedback(Appointment appointment, int rating, String comment) {
        if (!isOver(appointment)) {
            throw new IllegalArgumentException("You can leave feedback after your appointment is over.");
        }
        if (hasFeedback(appointment.getId())) {
            throw new IllegalArgumentException("You already sent feedback for this appointment.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Pick a rating from 1 to 5.");
        }
        if (comment != null) {
            comment = comment.trim();
            if (comment.isEmpty()) {
                comment = null;
            }
        }
        if (comment != null && comment.length() > 500) {
            throw new IllegalArgumentException("Your comment can be up to 500 characters.");
        }
        return feedbackRepository.save(new AppointmentFeedback(appointment, rating, comment));
    }
}
