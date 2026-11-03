package com.cmpt276.project.owner.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "salon")
public class SalonEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String ownerId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String servicesJson;

    @Column(columnDefinition = "TEXT")
    private String openingHoursJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getServicesJson() { return servicesJson; }
    public void setServicesJson(String servicesJson) {
        this.servicesJson = servicesJson;
    }

    public String getOpeningHoursJson() { return openingHoursJson; }
    public void setOpeningHoursJson(String openingHoursJson) {
        this.openingHoursJson = openingHoursJson;
    }
}