package com.cmpt276.project.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
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

    private String serviceName;
    private String serviceId;
    private String salonId;
    private LocalDate appointmentDate;
    private String appointmentTime;

    //connects the appointment to the customer who booked it
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Account customer;

    // id of the matching Google Calendar event, null until synced
    @Column(length = 1024)
    private String googleEventId;

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

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }
}