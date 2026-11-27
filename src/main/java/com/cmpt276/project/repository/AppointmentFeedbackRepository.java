package com.cmpt276.project.repository;

import com.cmpt276.project.model.AppointmentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentFeedbackRepository extends JpaRepository<AppointmentFeedback, Integer> {
    boolean existsByAppointmentId(int appointmentId);

    List<AppointmentFeedback> findBySalonIdOrderByCreatedAtDesc(String salonId);
}
