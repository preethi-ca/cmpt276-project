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
    // Public salon contacts are independent of the owner's login account.
    private String city;
    private String province;
    private String phone;
    private String email;
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
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<SalonService> getServices() { return services; }
    public void setServices(List<SalonService> services) { this.services = services; }
    public List<OpeningDay> getOpeningHours() { return openingHours; }
    public void setOpeningHours(List<OpeningDay> openingHours) { this.openingHours = openingHours; }
    public boolean isHoursConfigured() { return !openingHours.isEmpty(); }
}
