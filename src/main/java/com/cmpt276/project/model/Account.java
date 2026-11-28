package com.cmpt276.project.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(unique = true)
    private String email;

    private String passwordHash;

    @Column(unique = true)
    private String googleSubject;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    // iteration 2: phone number and notification preference
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference = NotificationPreference.EMAIL;

    @ElementCollection
    @OrderColumn(name = "visit_position")
    private List<String> recentSalonIds = new ArrayList<>();

    public void recordSalonVisit(String salonId) {
        // Store each salon once, with the most recent visit at position zero.
        recentSalonIds.remove(salonId);
        recentSalonIds.add(0, salonId);
    }

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

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getGoogleSubject() {
        return googleSubject;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }
}
