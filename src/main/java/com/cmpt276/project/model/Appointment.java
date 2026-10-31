package com.cmpt276.project.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /*Name of the service being booked 
    - This is temporary until we connect Edwin's service data*/
    private String serviceName;

    private LocalDate appointmentDate;

    private String appointmentTime;

    //connects the appointment to the customer who booked it
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Account customer;
    public Appointment() {
    }

    public Appointment(String serviceName, LocalDate appointmentDate, String appointmentTime, Account customer) {
        this.serviceName = serviceName;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.customer = customer;
    }

    public int getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public Account getCustomer() {
        return customer;
    }
}