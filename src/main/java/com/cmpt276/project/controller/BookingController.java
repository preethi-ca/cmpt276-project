package com.cmpt276.project.controller;

import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import com.cmpt276.project.service.BookingService;
import java.time.LocalDate;
import java.util.List;


@Controller
public class BookingController {
    private final BookingService bookingService;
    private final OwnerService ownerService;
    public BookingController(BookingService bookingService, OwnerService ownerService) {
        this.bookingService = bookingService;
        this.ownerService = ownerService;
    }

    // Show the date selection page
    @GetMapping("/customer/booking")
    public String showBookingPage(@RequestParam String service,@RequestParam String salon, Model model) {
        Salon selectedSalon = ownerService.findPublicSalonBySlug(salon);
        SalonService selectedService = findService(selectedSalon, service);
        model.addAttribute("service", service);
        model.addAttribute("serviceName", selectedService.getName());
        model.addAttribute("salon", salon);
        model.addAttribute("openingHours", selectedSalon.getOpeningHours());
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday","Friday", "Saturday", "Sunday"};
        model.addAttribute("days", days);
        return "customer/select-date";
    }

    // Show the time selection page
    @GetMapping("/customer/booking/time")
    public String showTimePage(@RequestParam String service, @RequestParam String salon, @RequestParam String appointmentDate, Model model) {
        Salon selectedSalon = ownerService.findPublicSalonBySlug(salon);
        SalonService selectedService = findService(selectedSalon, service);
        LocalDate date = LocalDate.parse(appointmentDate);
        int selectedDay = date.getDayOfWeek().getValue();
        boolean salonIsOpen = false;
        for (OpeningDay day : selectedSalon.getOpeningHours()) {
            if (day.getDayOfWeek() == selectedDay && day.isOpen()) {
                salonIsOpen = true;
                break;
            }
        }
        if (!salonIsOpen) {
            model.addAttribute("error", "Sorry, salon is closed on the selected date");
            model.addAttribute("service", service);
            model.addAttribute("salon", salon);
            model.addAttribute("serviceName", selectedService.getName());
            model.addAttribute("openingHours", selectedSalon.getOpeningHours());
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            model.addAttribute("days", days);
            return "customer/select-date";
        }
        model.addAttribute("service", service);
        model.addAttribute("salon", salon);
        model.addAttribute("appointmentDate", appointmentDate);
        model.addAttribute("serviceName", selectedService.getName());
    
        List<String> availableTimes = bookingService.getAvailableTimes(salon, date);
        model.addAttribute("availableTimes", availableTimes);
        return "customer/select-time";
    }

    // Show the booking summary page
    @GetMapping("/customer/booking/confirmation")
    public String showBookingSummary(@RequestParam String service,@RequestParam String salon, @RequestParam String appointmentDate, @RequestParam String appointmentTime,Model model){
        Salon selectedSalon = ownerService.findPublicSalonBySlug(salon);
        SalonService selectedService = findService(selectedSalon, service);
        model.addAttribute("service", service);
        model.addAttribute("salon", salon);
        model.addAttribute("salonName", selectedSalon.getName());
        model.addAttribute("serviceName", selectedService.getName());
        model.addAttribute("appointmentDate", appointmentDate);
        model.addAttribute("appointmentTime", appointmentTime);
        return "customer/booking-summary";
    }

    //Save the appointment and links it to the customer after clicking confirm
    @PostMapping("/customer/booking/confirm")
    public String confirmBooking(@RequestParam String service, @RequestParam String salon, @RequestParam String appointmentDate,@RequestParam String appointmentTime,Authentication authentication, Model model) {
        String customerEmail = authentication.getName();
       try {
            bookingService.bookAppointment(service, salon, appointmentDate, appointmentTime, customerEmail);
        } catch (IllegalArgumentException error) {
            Salon selectedSalon = ownerService.findPublicSalonBySlug(salon);
            SalonService selectedService = findService(selectedSalon, service);
            LocalDate date = LocalDate.parse(appointmentDate);
            List<String> availableTimes = bookingService.getAvailableTimes(salon, date);
            model.addAttribute("error", error.getMessage());
            model.addAttribute("service", service);
            model.addAttribute("salon", salon);
            model.addAttribute("serviceName", selectedService.getName());
            model.addAttribute("appointmentDate", appointmentDate);
            model.addAttribute("availableTimes", availableTimes);
            return "customer/select-time";
        }
        Salon selectedSalon = ownerService.findPublicSalonBySlug(salon);
        SalonService selectedService = findService(selectedSalon, service);
        model.addAttribute("service", service);
        model.addAttribute("salon", salon);
        model.addAttribute("salonName", selectedSalon.getName());
        model.addAttribute("serviceName", selectedService.getName());
        model.addAttribute("appointmentDate", appointmentDate);
        model.addAttribute("appointmentTime", appointmentTime);
        return "customer/booking-confirmation";
    }

    private SalonService findService(Salon salon, String serviceId) {
        for (SalonService service : salon.getServices()) {
            if (service.getId().equals(serviceId)) {
                return service;
            }
        }
        throw new IllegalArgumentException("Service not found in this salon");
    }
}