package com.cmpt276.project.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

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
    private BigDecimal bookedPrice;
    private Integer durationMinutes;

    // connects the appointment to the customer who booked it
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Account customer;

    // id of the matching Google Calendar event, null until synced
    @Column(length = 1024)
    private String googleEventId;

    public Appointment() {
    }

    public Appointment(
            String serviceName,
            String serviceId,
            String salonId,
            LocalDate appointmentDate,
            String appointmentTime,
            Account customer,
            BigDecimal bookedPrice) {
        this(serviceName, serviceId, salonId, appointmentDate, appointmentTime, customer, bookedPrice, 60);
    }

    public Appointment(
            String serviceName,
            String serviceId,
            String salonId,
            LocalDate appointmentDate,
            String appointmentTime,
            Account customer,
            BigDecimal bookedPrice,
            int durationMinutes) {

        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.salonId = salonId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.customer = customer;
        this.bookedPrice = bookedPrice;
        this.durationMinutes = durationMinutes;
    }

    public Appointment(
            String serviceName,
            String serviceId,
            String salonId,
            LocalDate appointmentDate,
            String appointmentTime,
            Account customer) {

        this(
                serviceName,
                serviceId,
                salonId,
                appointmentDate,
                appointmentTime,
                customer,
                null);
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

    public BigDecimal getBookedPrice() {
        return bookedPrice;
    }

    public int getDurationMinutes() {
        return durationMinutes == null ? 60 : durationMinutes;
    }

    public LocalTime getEndTime() {
        return LocalTime.parse(appointmentTime).plusMinutes(getDurationMinutes());
    }
  
    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }
}
