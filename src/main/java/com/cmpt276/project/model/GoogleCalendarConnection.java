package com.cmpt276.project.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

// saved Google Calendar link for one customer
@Entity
public class GoogleCalendarConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Account customer;

    @Column(nullable = false, length = 2048)
    private String refreshToken;

    private LocalDateTime connectedAt;

    public GoogleCalendarConnection() {
    }

    public GoogleCalendarConnection(Account customer, String refreshToken) {
        this.customer = customer;
        this.refreshToken = refreshToken;
        this.connectedAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public Account getCustomer() {
        return customer;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
}
