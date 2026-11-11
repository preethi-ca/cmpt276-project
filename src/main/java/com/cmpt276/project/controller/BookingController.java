package com.cmpt276.project.controller;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.BookingConfirmationMessage;
import com.cmpt276.project.model.BookingEmailResult;
import com.cmpt276.project.model.BookingSmsMessage;
import com.cmpt276.project.model.BookingSmsResult;
import com.cmpt276.project.model.NotificationPreference;
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
import com.cmpt276.project.service.BookingSmsBuilder;
import com.cmpt276.project.service.TwilioSmsSender;
import java.time.LocalDate;
import java.util.List;


@Controller
public class BookingController {
    private final BookingService bookingService;
    private final OwnerService ownerService;
    private final BrevoEmailSender emailSender;
    private final BookingConfirmationBuilder confirmationBuilder = new BookingConfirmationBuilder();
    private final TwilioSmsSender smsSender;
    private final BookingSmsBuilder smsBuilder = new BookingSmsBuilder();

    public BookingController(BookingService bookingService, OwnerService ownerService, BrevoEmailSender emailSender, TwilioSmsSender smsSender) {
        this.bookingService = bookingService;
        this.ownerService = ownerService;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
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
        // Spring commits the service transaction before returning the appointment.
        model.addAttribute("service", appointment.getServiceId());
        model.addAttribute("serviceName", appointment.getServiceName());
        model.addAttribute("appointmentDate", appointment.getAppointmentDate());
        model.addAttribute("appointmentTime", appointment.getAppointmentTime());
        model.addAttribute("salonName", "Salon details are unavailable");

        BookingEmailResult emailResult = BookingEmailResult.UNAVAILABLE;
        BookingSmsResult smsResult = BookingSmsResult.UNAVAILABLE;
        Salon selectedSalon = showBookedSalon(appointment, model);

        // Accounts without a saved choice already read as EMAIL, so only a missing customer needs a default.
        NotificationPreference preference = NotificationPreference.EMAIL;
        if (appointment.getCustomer() != null) {
            preference = appointment.getCustomer().getNotificationPreference();
        }

        // Each channel is tried on its own so a problem with one never stops the other.
        if (preference == NotificationPreference.EMAIL || preference == NotificationPreference.BOTH) {
            emailResult = sendConfirmationEmail(appointment, selectedSalon);
            model.addAttribute("emailResult", emailResult);
            model.addAttribute("emailMessage", emailFeedback(emailResult));
        }
        if (preference == NotificationPreference.SMS || preference == NotificationPreference.BOTH) {
            smsResult = sendConfirmationText(appointment, selectedSalon);
            model.addAttribute("smsResult", smsResult);
            model.addAttribute("smsMessage", smsFeedback(smsResult));
        }
        return "customer/booking-confirmation";
    }

    // Adds the salon details to the page. Returns null when the salon cannot be loaded.
    private Salon showBookedSalon(Appointment appointment, Model model) {
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
            return selectedSalon;
        } catch (RuntimeException error) {
            // Missing salon details must not turn a committed booking into a failure.
            return null;
        }
    }

    private BookingEmailResult sendConfirmationEmail(Appointment appointment, Salon salon) {
        BookingConfirmationMessage message;
        try {
            message = confirmationBuilder.build(appointment, salon);
        } catch (RuntimeException error) {
            // Missing email details must not turn a committed booking into a failure.
            return BookingEmailResult.UNAVAILABLE;
        }
        try {
            return emailSender.send(message);
        } catch (RuntimeException error) {
            // Sending may have started, so a definite failure would be misleading.
            return BookingEmailResult.UNCERTAIN;
        }
    }

    private BookingSmsResult sendConfirmationText(Appointment appointment, Salon salon) {
        BookingSmsMessage message;
        try {
            message = smsBuilder.build(appointment, salon);
        } catch (RuntimeException error) {
            // A missing or invalid phone must not turn a committed booking into a failure.
            return BookingSmsResult.UNAVAILABLE;
        }
        try {
            return smsSender.send(message.getRecipient(), message.getText());
        } catch (RuntimeException error) {
            // Sending may have started, so a definite failure would be misleading.
            return BookingSmsResult.UNCERTAIN;
        }
    }

    private String emailFeedback(BookingEmailResult result) {
        if (result == BookingEmailResult.ACCEPTED) {
            return "Confirmation email submitted for delivery.";
        }
        if (result == BookingEmailResult.REJECTED) {
            return "The email provider rejected the confirmation email. Your appointment is still booked.";
        }
        if (result == BookingEmailResult.UNCERTAIN) {
            return "Email sending could not be confirmed; the email may still arrive. Your appointment is still booked.";
        }
        return "Confirmation email is unavailable. Your appointment is still booked.";
    }

    // A rejected text can also mean Twilio was busy, so the wording does not blame the number.
    private String smsFeedback(BookingSmsResult result) {
        if (result == BookingSmsResult.ACCEPTED) {
            return "Confirmation text submitted for delivery.";
        }
        if (result == BookingSmsResult.REJECTED) {
            return "The text message service could not send the confirmation text. Your appointment is still booked.";
        }
        if (result == BookingSmsResult.UNCERTAIN) {
            return "Text sending could not be confirmed. The text may still arrive. Your appointment is still booked.";
        }
        return "Confirmation text is unavailable. Your appointment is still booked.";
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
