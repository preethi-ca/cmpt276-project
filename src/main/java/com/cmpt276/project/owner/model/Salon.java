package com.cmpt276.project.owner.model;

import java.util.ArrayList;
import java.util.List;

/** Salon data object: Stores information about the Salon,
 * as well as the service list of this store
 * and the weekly business hours.
 * **/
public class Salon {
    private String id;
    // The "id" field identifies "which store", and the "ownerId" field identifies "which account it belongs to".
    private String ownerId;
    private String name;
    private String slug;
    private String description;
    private String address;
    private List<SalonService> services = new ArrayList<>();
    private List<OpeningDay> openingHours = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public List<SalonService> getServices() { return services; }
    public void setServices(List<SalonService> services) { this.services = services; }
    public List<OpeningDay> getOpeningHours() { return openingHours; }
    public void setOpeningHours(List<OpeningDay> openingHours) { this.openingHours = openingHours; }
    public boolean isHoursConfigured() { return !openingHours.isEmpty(); }
}
