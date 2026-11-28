package com.cmpt276.project.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.service.AppointmentFeedbackService;

@Controller
public class AppointmentFeedbackController {
    private final AppointmentRepository appointmentRepository;
    private final AccountRepository accountRepository;
    private final OwnerService ownerService;
    private final AppointmentFeedbackService feedbackService;

    public AppointmentFeedbackController(AppointmentRepository appointmentRepository, AccountRepository accountRepository, OwnerService ownerService, AppointmentFeedbackService feedbackService) {
        this.appointmentRepository = appointmentRepository;
        this.accountRepository = accountRepository;
        this.ownerService = ownerService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/customer/appointments/feedback")
    public String feedbackPage(@RequestParam int id, Authentication authentication, Model model) {
        Optional<Appointment> appointmentOpt = findCustomerAppointment(id, authentication);
        if (!appointmentOpt.isPresent()) {
            return "redirect:/customer/appointments";
        }
        Appointment appointment = appointmentOpt.get();
        Salon salon = ownerService.findPublicSalon(appointment.getSalonId());

        model.addAttribute("appointment", appointment);
        model.addAttribute("salonName", salon.getName());
        model.addAttribute("isOver", feedbackService.isOver(appointment));
        model.addAttribute("feedbackSent", feedbackService.hasFeedback(appointment.getId()));
        return "customer/feedback";
    }

    @PostMapping("/customer/appointments/feedback")
    public String sendFeedback(@RequestParam int id, @RequestParam(defaultValue = "0") int rating, @RequestParam(required = false) String comment, Authentication authentication, RedirectAttributes redirectAttributes) {
        Optional<Appointment> appointmentOpt = findCustomerAppointment(id, authentication);
        if (!appointmentOpt.isPresent()) {
            return "redirect:/customer/appointments";
        }
        Appointment appointment = appointmentOpt.get();
        Salon salon = ownerService.findPublicSalon(appointment.getSalonId());

        try {
            feedbackService.saveFeedback(appointment, rating, comment);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/appointments/feedback?id=" + id;
        }
        redirectAttributes.addFlashAttribute("message", "Your feedback was sent to " + salon.getName() + ".");
        return "redirect:/customer/appointments/feedback?id=" + id;
    }

    private Optional<Appointment> findCustomerAppointment(int id, Authentication authentication) {
        Optional<Account> customerOpt = accountRepository.findByEmail(authentication.getName());
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();
        return appointmentRepository.findByIdAndCustomer(id, customer);
    }
}
