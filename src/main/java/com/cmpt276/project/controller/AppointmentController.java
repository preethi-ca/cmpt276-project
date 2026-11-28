package com.cmpt276.project.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.GoogleCalendarResult;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.service.BookingService;
import com.cmpt276.project.service.GoogleCalendarSync;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppointmentController {
    private final AppointmentRepository appointmentRepository;
    private final AccountRepository accountRepository;
    private final OwnerService ownerService;
    private final BookingService bookingService;
    private final GoogleCalendarSync calendarSync;
    public AppointmentController(AppointmentRepository appointmentRepository,AccountRepository accountRepository, OwnerService ownerService, BookingService bookingService, GoogleCalendarSync calendarSync){
        this.appointmentRepository = appointmentRepository;
        this.accountRepository = accountRepository;
        this.ownerService = ownerService;
        this.bookingService = bookingService;
        this.calendarSync = calendarSync;
    }

    //show all appointments the customer has
    @GetMapping("/customer/appointments")
    public String showAppointments(Authentication authentication, Model model) {
        //find the customer's account using their email
        String customerEmail = authentication.getName();
        Optional<Account> customerOpt =accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();
        List<Appointment> appointments = appointmentRepository.findByCustomer(customer);
        
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Vancouver"));

        //create separate lists for upcoming and past appointments
        List<Appointment> upcomingAppointments = new ArrayList<>();
        List<Appointment> pastAppointments = new ArrayList<>();

        //Separate appointments based on their scheduled date and time
        for (Appointment appointment : appointments) {
            LocalDateTime appointmentDateTime = LocalDateTime.of(
                    appointment.getAppointmentDate(),
                    LocalTime.parse(appointment.getAppointmentTime())
            );

            if (!appointmentDateTime.isBefore(now)) {
                upcomingAppointments.add(appointment);
            } else {
                pastAppointments.add(appointment);
            }
        }

        //Sort appointments by date and time
        Comparator<Appointment> appointmentDateTimeComparator = Comparator.comparing(Appointment::getAppointmentDate)
            .thenComparing(appointment -> LocalTime.parse(appointment.getAppointmentTime()));

        upcomingAppointments.sort(appointmentDateTimeComparator);
        pastAppointments.sort(appointmentDateTimeComparator.reversed());
        
        Map<Integer, String> salonNames = new HashMap<>();
        for (Appointment appointment : appointments){ 
            Salon salon = ownerService.findPublicSalon(appointment.getSalonId()); 
            salonNames.put(appointment.getId(), salon.getName());
        }
        model.addAttribute("salonNames", salonNames);
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        model.addAttribute("pastAppointments", pastAppointments);
        return "customer/my-appointments";
    }

    // Show the details of one appointment
    @GetMapping("/customer/appointments/details")
    public String showAppointmentDetails(@RequestParam int id,Authentication authentication,Model model) {
        String customerEmail = authentication.getName();
        Optional<Account> customerOpt = accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();

        Optional<Appointment> appointmentOpt =appointmentRepository.findById(id);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Appointment not found");
        }
        Appointment appointment = appointmentOpt.get();
        if (appointment.getCustomer().getId() != customer.getId()) {
            throw new IllegalArgumentException("You cannot view this appointment");
        }
        Salon salon = ownerService.findPublicSalon(appointment.getSalonId());
        SalonService bookedService = null;
        for (SalonService service : salon.getServices()) { 
            if (service.getId().equals(appointment.getServiceId())){ 
                bookedService = service; 
                break;
            }
        }
        model.addAttribute("appointment", appointment);
        model.addAttribute("salon", salon);
        model.addAttribute("bookedService", bookedService);
        return "customer/appointment-details";
    }

    //Rebooking past apppointments
    @GetMapping("/customer/appointments/rebook")
    public String rebookAppointment(@RequestParam int id, Authentication authentication, RedirectAttributes redirectAttributes) {
        Optional<Account> customerOpt = accountRepository.findByEmail(authentication.getName());
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();

        Optional<Appointment> appointmentOpt = appointmentRepository.findByIdAndCustomer(id, customer);
        if (!appointmentOpt.isPresent()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Appointment not found."
            );
            return "redirect:/customer/appointments";
        }

        Appointment appointment = appointmentOpt.get();

        // Check whether the original appointment is in the past
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Vancouver"));

        LocalDateTime appointmentDateTime = LocalDateTime.of(
                appointment.getAppointmentDate(),
                LocalTime.parse(appointment.getAppointmentTime())
        );

        if (!appointmentDateTime.isBefore(now)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only past appointments can be rebooked."
            );
            return "redirect:/customer/appointments";
        }

        //find the original salon
        Salon salon = ownerService.findPublicSalon(appointment.getSalonId());
        if (salon == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This salon is no longer available for booking."
            );
            return "redirect:/customer/appointments";
        }

        //check if the original service still exists at this salon
        SalonService originalService = null;
        for (SalonService service : salon.getServices()) {
            if (service.getId().equals(appointment.getServiceId())) {
                originalService = service;
                break;
            }
        }

        if (originalService == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This service is no longer offered by the salon."
            );
            return "redirect:/customer/appointments";
        }

        //redirect to the existing booking page with the original salon and service selected
        redirectAttributes.addAttribute("service", appointment.getServiceId());
        redirectAttributes.addAttribute("salon", salon.getSlug());

        return "redirect:/customer/booking";
    }

    //Show the page for editing an appointment
    @GetMapping("/customer/appointments/edit")
    public String editAppointmentPage(@RequestParam int id, Authentication authentication, Model model) {
        String customerEmail = authentication.getName();
        Optional<Account> customerOpt = accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();

        Optional<Appointment> appointmentOpt =appointmentRepository.findByIdAndCustomer(id, customer);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Appointment not found");
        }
        Appointment appointment = appointmentOpt.get();

        //load the salon and available time slots, excluding the current appointment
        Salon salon = ownerService.findPublicSalon(appointment.getSalonId());
        List<String> availableTimes = bookingService.getAvailableTimes(
                salon.getSlug(),
                appointment.getAppointmentDate(),
                appointment.getId()
        );

        //give the appointment details and available times to the edit page
        model.addAttribute("appointment", appointment);
        model.addAttribute("salon", salon);
        model.addAttribute("availableTimes", availableTimes);
        model.addAttribute("minDate", LocalDate.now().toString());
        return "customer/edit-appointment";
    }

    //finds the available appointment times for a selected date
    @GetMapping("/customer/appointments/available-times")
    @ResponseBody
    public List<String> getAvailableTimesForEditing(@RequestParam int id, @RequestParam String date, Authentication authentication) {
        Optional<Account> customerOpt = accountRepository.findByEmail(authentication.getName());
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();
        Optional<Appointment> appointmentOpt =appointmentRepository.findByIdAndCustomer(id, customer);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Appointment not found");
        }
        Appointment appointment = appointmentOpt.get();
        Salon salon = ownerService.findPublicSalon(appointment.getSalonId());
        LocalDate selectedDate = LocalDate.parse(date);
        return bookingService.getAvailableTimes(salon.getSlug(), selectedDate, appointment.getId());
    }

    //Cancel appointment
    @PostMapping("/customer/appointments/cancel")
    public String cancelAppointment(@RequestParam("id") int appointmentId, Authentication authentication, RedirectAttributes redirectAttributes) {

        //cancel the appointment belonging to the logged-in customer
        Appointment appointment = bookingService.cancelAppointment(appointmentId, authentication.getName());

        // Google Calendar runs after the appointment is deleted, a failure only changes the message
        GoogleCalendarResult calendarResult = calendarSync.removeBooking(appointment);

        //Show a success message
        redirectAttributes.addFlashAttribute("message", "Your appointment has been cancelled");
        redirectAttributes.addFlashAttribute("calendarMessage", calendarSync.removeFeedback(calendarResult));
        return "redirect:/customer/appointments";
    }

    //Handles editing an existing appointment (date and time)
    @PostMapping("/customer/appointments/edit")
    public String updateAppointment(@RequestParam int id, @RequestParam String appointmentDate, @RequestParam String appointmentTime, Authentication authentication, RedirectAttributes redirectAttributes) {
        Appointment appointment;
        try {
            appointment = bookingService.updateAppointment(id, appointmentDate, appointmentTime, authentication.getName());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/appointments/edit?id=" + id;
        }
        redirectAttributes.addFlashAttribute("message", "Your appointment has been successfully updated.");

        // Google Calendar runs after the change is saved, a failure only changes the message
        GoogleCalendarResult calendarResult = calendarSync.updateBooking(appointment);
        redirectAttributes.addFlashAttribute("calendarMessage", calendarSync.updateFeedback(calendarResult));
        return "redirect:/customer/appointments/details?id=" + id;
    }

}