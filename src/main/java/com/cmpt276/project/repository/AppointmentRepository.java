package com.cmpt276.project.repository;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByCustomer(Account customer);
    Optional<Appointment> findByIdAndCustomer(int id, Account customer);
    List<Appointment> findBySalonIdAndAppointmentDate(String salonId, LocalDate appointmentDate);

    @EntityGraph(attributePaths = "customer")
    List<Appointment> findBySalonIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAscIdAsc(
            String salonId, LocalDate date, Pageable pageable);

    long countBySalonIdAndAppointmentDate(String salonId, LocalDate date);
    long countBySalonIdAndAppointmentDateAfter(String salonId, LocalDate date);
    long countBySalonIdAndAppointmentDateBetween(String salonId, LocalDate start, LocalDate end);
}
