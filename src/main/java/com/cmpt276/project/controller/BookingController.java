package com.cmpt276.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;

import com.cmpt276.project.service.BookingService;

@Controller
public class BookingController {
    private final BookingService bookingService;
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Show the date selection page
    @GetMapping("/customer/booking")
    public String showBookingPage(@RequestParam String service, Model model) {model.addAttribute("service", service);
        return "select-date";
    }

    // Show the time selection page
    @GetMapping("/customer/booking/time")
    public String showTimePage(@RequestParam String service, @RequestParam String appointmentDate, Model model) {
        model.addAttribute("service", service);
        model.addAttribute("appointmentDate", appointmentDate);
        return "select-time";
    }

    // Show the booking summary page
    @GetMapping("/customer/booking/confirmation")
    public String showBookingSummary(@RequestParam String service,@RequestParam String appointmentDate, @RequestParam String appointmentTime,Model model){
        model.addAttribute("service", service);
        model.addAttribute("appointmentDate", appointmentDate);
        model.addAttribute("appointmentTime", appointmentTime);
        return "booking-summary";
    }

     //Save the appointment and links it to the customer after clicking confirm
    @PostMapping("/customer/booking/confirm")
    public String confirmBooking(@RequestParam String service,@RequestParam String appointmentDate,@RequestParam String appointmentTime,Authentication authentication, Model model) {
        String customerEmail = authentication.getName();
        bookingService.bookAppointment(service, appointmentDate,appointmentTime,customerEmail);
        model.addAttribute("service", service);
        model.addAttribute("appointmentDate", appointmentDate);
        model.addAttribute("appointmentTime", appointmentTime);
        return "booking-confirmation";
    }
}