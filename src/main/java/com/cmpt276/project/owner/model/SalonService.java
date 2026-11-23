package com.cmpt276.project.owner.model;

import java.math.BigDecimal;
import java.util.List;

public class SalonService {
    public static final List<Integer> DURATION_OPTIONS = List.of(30, 60, 90, 120);
    private String id;
    private String name;
    private BigDecimal price;
    private String description = "";
    private int durationMinutes = 60;
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
