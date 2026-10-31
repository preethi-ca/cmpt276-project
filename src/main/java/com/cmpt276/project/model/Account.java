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

    public Account() {
    }

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

    public RoleType getRoleType() {
        return roleType;
    }
}