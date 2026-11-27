package com.cmpt276.project.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class AppointmentFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private int appointmentId;

    // copied from the appointment because cancelling deletes the appointment row
    private String salonId;
    private String serviceName;
    private LocalDate appointmentDate;
    private String appointmentTime;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Account customer;

    private int rating;

    @Column(length = 500)
    private String comment;

    private LocalDateTime createdAt;

    public AppointmentFeedback() {
    }

    public AppointmentFeedback(Appointment appointment, int rating, String comment) {
        this.appointmentId = appointment.getId();
        this.salonId = appointment.getSalonId();
        this.serviceName = appointment.getServiceName();
        this.appointmentDate = appointment.getAppointmentDate();
        this.appointmentTime = appointment.getAppointmentTime();
        this.customer = appointment.getCustomer();
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now(ZoneId.of("America/Vancouver"));
    }

    public int getId() {
        return id;
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public String getSalonId() {
        return salonId;
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

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
