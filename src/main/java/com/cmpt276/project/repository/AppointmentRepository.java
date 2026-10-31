package com.cmpt276.project.repository;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByCustomer(Account customer);
    Optional<Appointment> findByIdAndCustomer(int id, Account customer);
}