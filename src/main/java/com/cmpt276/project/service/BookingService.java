package com.cmpt276.project.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;

@Service
public class BookingService{
    private final AppointmentRepository appointmentRepository;
    private final AccountRepository accountRepository;

    public BookingService(AppointmentRepository appointmentRepository, AccountRepository accountRepository) {
        this.appointmentRepository = appointmentRepository;
        this.accountRepository = accountRepository;
    }

    public void bookAppointment(String service, String appointmentDate, String appointmentTime, String customerEmail) {
        Optional<Account> customerOpt = accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();

        LocalDate date = LocalDate.parse(appointmentDate);
        Appointment appointment = new Appointment(service, date, appointmentTime, customer);

        appointmentRepository.save(appointment);
    }
}