package com.cmpt276.project.owner.service;

import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.store.SalonStore;
import com.cmpt276.project.owner.controller.OwnerController.HoursForm;
import com.cmpt276.project.owner.controller.OwnerController.SalonForm;
import com.cmpt276.project.owner.controller.OwnerController.ServiceForm;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OwnerService {
    private final SalonStore store;

    public OwnerService(SalonStore store) {
        this.store = store;
    }

    public Salon findForOwner(String ownerId) {
        return store.findByOwnerId(ownerId);
    }

    public Salon findPublicSalon(String id) {
        Salon salon = store.findById(id);
        if (salon == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return salon;
    }
    //Use synchronized keyword to avoid data errors caused by concurrent modifications
    public synchronized boolean saveProfile(String ownerId, SalonForm form, boolean create, Errors errors) {
        validateText(form.getName(), "name", 80, true, errors);
        validateText(form.getDescription(), "description", 500, false, errors);
        validateText(form.getAddress(), "address", 200, false, errors);
        Salon salon = store.findByOwnerId(ownerId);
        if (create && salon != null) {
            errors.reject("exists", "You already have a salon. Open your workspace to view it.");
        }

        if (errors.hasErrors()) {
            return false;
        }
        if (create) {
            salon = new Salon();
            salon.setId(UUID.randomUUID().toString());
            salon.setOwnerId(ownerId);
        } else if (salon == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        salon.setName(form.getName().trim());
        salon.setDescription(clean(form.getDescription()));
        salon.setAddress(clean(form.getAddress()));
        store.save(salon);
        return true;
    }

    public synchronized boolean addService(String ownerId, ServiceForm form, Errors errors) {
        Salon salon = requireSalon(ownerId);
        validateText(form.getName(), "name", 80, true, errors);
        BigDecimal price = null;
        String value = clean(form.getPrice());
        // Allow 1 to 5-digit integers and optional 1 to 2-digit decimals;
        // Negative numbers, letters or scientific notation are not accepted
        if (!value.matches("[0-9]{1,5}(\\.[0-9]{1,2})?")) {
            errors.rejectValue("price", "price", "Enter a price from 0 to 99999.99, with up to two decimal places.");
        } else {
            // e.g. 35 -> 35.00。
            price = new BigDecimal(value).setScale(2);
        }
        for (SalonService service : salon.getServices()) {
            if (service.getName().equalsIgnoreCase(clean(form.getName()))) {
                errors.rejectValue("name", "duplicate", "A service with this name already exists.");
                break;
            }
        }
        if (errors.hasErrors()) {
            return false;
        }
        SalonService service = new SalonService();
        service.setId(UUID.randomUUID().toString());
        service.setName(form.getName().trim());
        service.setPrice(price);
        salon.getServices().add(service);
        store.save(salon);
        return true;
    }

    public synchronized boolean setOpeningHours(String ownerId, HoursForm form, Errors errors) {
        Salon salon = requireSalon(ownerId);
        if (salon.isHoursConfigured()) {
            errors.reject("configured", "Opening hours are already saved. Changes are not available in this version.");
            return false;
        }
        if (form.getDays() == null || form.getDays().size() != 7) {
            errors.reject("days", "Provide a weekly schedule with seven days.");
            return false;
        }
        List<OpeningDay> hours = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            OpeningDay input = form.getDays().get(i);
            OpeningDay day = new OpeningDay();
            day.setDayOfWeek(i + 1); // Monday = 1; the client cannot reorder the week.
            day.setOpen(input.isOpen());
            if (input.isOpen()) {
                String startField = "days[" + i + "].startTime";
                String endField = "days[" + i + "].endTime";
                LocalTime start = parseTime(input.getStartTime(), startField, errors);
                LocalTime end = parseTime(input.getEndTime(), endField, errors);
                if (start != null && end != null) {
                    int startMinutes = start.getHour() * 60 + start.getMinute();
                    int endMinutes = end.getHour() * 60 + end.getMinute();
                    if (endMinutes - startMinutes < 60) {
                        errors.rejectValue(endField, "range", "Closing time must be at least one hour after opening.");
                    }
                    day.setStartTime(start.toString());
                    day.setEndTime(end.toString());
                }
            }
            hours.add(day);
        }
        if (errors.hasErrors()) {
            return false;
        }
        salon.setOpeningHours(hours);
        store.save(salon);
        return true;
    }

    private Salon requireSalon(String ownerId) {
        Salon salon = store.findByOwnerId(ownerId);
        if (salon == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return salon;
    }

    private void validateText(String value, String field, int max, boolean required, Errors errors) {
        String text = clean(value);
        if (required && text.isEmpty()) {
            errors.rejectValue(field, "required", "Enter a name.");
        } else if (text.length() > max) {
            errors.rejectValue(field, "length", "Use " + max + " characters or fewer.");
        }
    }

    private LocalTime parseTime(String value, String field, Errors errors) {
        // Check the shape of HH:mm; LocalTime.parse then determines whether it is the real time, for example, it rejects 25:00.
        if (value == null || !value.matches("[0-9]{2}:[0-9]{2}")) {
            errors.rejectValue(field, "time", "Enter a time in HH:mm format.");
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            errors.rejectValue(field, "time", "Enter a valid time in HH:mm format.");
            return null;
        }
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
