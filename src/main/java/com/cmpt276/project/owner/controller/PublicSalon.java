package com.cmpt276.project.owner.controller;

import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import java.util.List;

public class PublicSalon {
    private final Salon salon;

    public PublicSalon(Salon salon) {
        this.salon = salon;
    }

    public String getId() { return salon.getId(); }
    public String getName() { return salon.getName(); }
    public String getSlug() {return salon.getSlug();}
    public String getDescription() { return salon.getDescription(); }
    public String getAddress() { return salon.getAddress(); }
    public List<SalonService> getServices() { return salon.getServices(); }
    public List<OpeningDay> getOpeningHours() { return salon.getOpeningHours(); }
    public boolean isHoursConfigured() { return salon.isHoursConfigured(); }
    public int getAppointmentMinutes() { return 60; }
    public String getCurrency() { return "CAD"; }
    public String getTimeZone() { return "America/Vancouver"; }
}
