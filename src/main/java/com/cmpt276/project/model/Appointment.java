package com.cmpt276.project.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /*Name of the service being booked 
    - This is temporary until we connect Edwin's service data*/
    private String serviceName;

    private String serviceId;

    private String salonId;

    private LocalDate appointmentDate;

    private String appointmentTime;

    //connects the appointment to the customer who booked it
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Account customer;
    public Appointment() {
    }

    public Appointment(String serviceName,String serviceId, String salonId, LocalDate appointmentDate, String appointmentTime, Account customer) {
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.salonId = salonId;
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

    public String getServiceId() {
        return serviceId;
    }

    public String getSalonId() {
        return salonId;
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