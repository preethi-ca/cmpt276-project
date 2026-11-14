package com.cmpt276.project.controller;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.BookingConfirmationMessage;
import com.cmpt276.project.model.BookingEmailResult;
import com.cmpt276.project.model.GoogleCalendarResult;
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
import com.cmpt276.project.service.BookingConfirmationBuilder;
import com.cmpt276.project.service.BrevoEmailSender;
import com.cmpt276.project.service.GoogleCalendarSync;
import java.time.LocalDate;
import java.util.List;


@Controller
public class BookingController {
    private final BookingService bookingService;
    private final OwnerService ownerService;
    private final BrevoEmailSender emailSender;
    private final GoogleCalendarSync calendarSync;
    private final BookingConfirmationBuilder confirmationBuilder = new BookingConfirmationBuilder();

    public BookingController(BookingService bookingService, OwnerService ownerService, BrevoEmailSender emailSender, GoogleCalendarSync calendarSync) {
        this.bookingService = bookingService;
        this.ownerService = ownerService;
        this.emailSender = emailSender;
        this.calendarSync = calendarSync;
    }

    // Show the date selection page
    @GetMapping("/customer/booking")
    public String showBookingPage(@RequestParam String service,@RequestParam String salon, Model model) {
        Salon selectedSalon = ownerService.findPublicSalonBySlug(salon);
        SalonService selectedService = findService(selectedSalon, service);
        model.addAttribute("service", service);
        model.addAttribute("serviceName", selectedService.getName());
        model.addAttribute("salon", salon);
        model.addAttribute("minDate", LocalDate.now().toString());
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
        //prevents customers from selecting an appointment date that has already passed
        if (date.isBefore(LocalDate.now())){
            model.addAttribute("error", "Sorry, this date has already passed");
            model.addAttribute("service", service);
            model.addAttribute("salon", salon);
            model.addAttribute("serviceName", selectedService.getName());
            model.addAttribute("openingHours", selectedSalon.getOpeningHours());
            model.addAttribute("minDate", LocalDate.now().toString());
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            model.addAttribute("days", days);
            return "customer/select-date";
        }
        int selectedDay = date.getDayOfWeek().getValue();
        boolean salonIsOpen = false;
        for (OpeningDay day: selectedSalon.getOpeningHours()) {
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
            model.addAttribute("minDate", LocalDate.now().toString());
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
        model.addAttribute("bookedService", selectedService);
        return "customer/booking-summary";
    }

    //Save the appointment and links it to the customer after clicking confirm
    @PostMapping("/customer/booking/confirm")
    public String confirmBooking(@RequestParam String service, @RequestParam String salon, @RequestParam String appointmentDate,@RequestParam String appointmentTime,Authentication authentication, Model model) {
        String customerEmail = authentication.getName();
        Appointment appointment;
        try {
            appointment = bookingService.bookAppointment(service, salon, appointmentDate, appointmentTime, customerEmail);
        } catch (IllegalArgumentException error) {
            Salon selectedSalon = ownerService.findPublicSalonBySlug(salon);
            SalonService selectedService = findService(selectedSalon, service);
            LocalDate date = LocalDate.parse(appointmentDate);
            //return customers back to the select date page if they try to book a date that has already passed
            if (date.isBefore(LocalDate.now())) { 
                model.addAttribute("error", error.getMessage());
                model.addAttribute("service", service);
                model.addAttribute("salon", salon);
                model.addAttribute("serviceName", selectedService.getName());
                model.addAttribute("openingHours", selectedSalon.getOpeningHours());
                model.addAttribute("minDate", LocalDate.now().toString());
                String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                model.addAttribute("days", days);
                return "customer/select-date"; // CHANGED
            }
            List<String> availableTimes = bookingService.getAvailableTimes(salon, date);
            model.addAttribute("error", error.getMessage());
            model.addAttribute("service", service);
            model.addAttribute("salon", salon);
            model.addAttribute("serviceName", selectedService.getName());
            model.addAttribute("appointmentDate", appointmentDate);
            model.addAttribute("availableTimes", availableTimes);
            model.addAttribute("bookedService", selectedService);
            return "customer/select-time";
        }
        model.addAttribute("service", appointment.getServiceId());
        model.addAttribute("serviceName", appointment.getServiceName());
        model.addAttribute("appointmentDate", appointment.getAppointmentDate());
        model.addAttribute("appointmentTime", appointment.getAppointmentTime());
        model.addAttribute("salonName", "Salon details are unavailable");


        //Confirmation email
        BookingEmailResult emailResult = BookingEmailResult.UNAVAILABLE;
        BookingConfirmationMessage message = null;
        try {
            Salon selectedSalon = ownerService.findPublicSalon(appointment.getSalonId());
            model.addAttribute("salon", selectedSalon.getSlug());
            model.addAttribute("salonName", selectedSalon.getName());
            for (SalonService selectedService : selectedSalon.getServices()) {
                if (selectedService.getId().equals(appointment.getServiceId())) {
                    model.addAttribute("bookedService", selectedService);
                    break;
                }
            }
            message = confirmationBuilder.build(appointment, selectedSalon);
        } catch (RuntimeException error) {
            // Missing email details must not turn a committed booking into a failure.
            emailResult = BookingEmailResult.UNAVAILABLE;
        }
        if (message != null) {
            try {
                emailResult = emailSender.send(message);
            } catch (RuntimeException error) {
                // Sending may have started, so a definite failure would be misleading.
                emailResult = BookingEmailResult.UNCERTAIN;
            }
        }
        model.addAttribute("emailResult", emailResult);
        model.addAttribute("emailMessage", emailFeedback(emailResult));
        // Google Calendar runs after the booking is saved, a failure only changes the message
        GoogleCalendarResult calendarResult = calendarSync.addBooking(appointment);
        model.addAttribute("calendarMessage", calendarSync.feedback(calendarResult));
        return "customer/booking-confirmation";
    }

    private String emailFeedback(BookingEmailResult result) {
        if (result == BookingEmailResult.ACCEPTED) {
            return "Confirmation email submitted.";
        }
        if (result == BookingEmailResult.REJECTED) {
            return "The email provider rejected the confirmation email. Your appointment is still booked.";
        }
        if (result == BookingEmailResult.UNCERTAIN) {
            return "Email sending could not be confirmed, the email may still arrive. Your appointment is still booked.";
        }
        return "Confirmation email is unavailable. Your appointment is still booked.";
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
