package com.cmpt276.project.model;

import jakarta.persistence.*;

@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(unique = true)
    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    // iteration 2: phone number and notification preference
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference = NotificationPreference.EMAIL;

    public Account() {
    }

    // keep existing constructor for compatibility
    public Account(String name, String email, String passwordHash, RoleType roleType) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleType = roleType;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public NotificationPreference getNotificationPreference() {
        return notificationPreference == null
                ? NotificationPreference.EMAIL
                : notificationPreference;
    }

    public void setNotificationPreference(NotificationPreference notificationPreference) {
        this.notificationPreference = notificationPreference;
    }
}