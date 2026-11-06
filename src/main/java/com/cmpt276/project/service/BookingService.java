package com.cmpt276.project.service;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.service.OwnerService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.cmpt276.project.owner.model.OpeningDay;

@Service
public class BookingService{
    private final AppointmentRepository appointmentRepository;
    private final AccountRepository accountRepository;

    private final OwnerService ownerService;

    public BookingService(AppointmentRepository appointmentRepository, AccountRepository accountRepository,OwnerService ownerService) {
        this.appointmentRepository = appointmentRepository;
        this.accountRepository = accountRepository;
        this.ownerService = ownerService;
    }

    @Transactional
    public Appointment bookAppointment(String serviceId, String salonSlug, String appointmentDate, String appointmentTime, String customerEmail) {
        Optional<Account> customerOpt = accountRepository.findByEmail(customerEmail);
        if (!customerOpt.isPresent()) {
            throw new IllegalArgumentException("Customer account not found");
        }
        Account customer = customerOpt.get();

        Salon salon = ownerService.findPublicSalonBySlug(salonSlug);
        SalonService selectedService = null;
        for (SalonService service:salon.getServices()) {
            if (service.getId().equals(serviceId)) {
                selectedService = service;
                break;
            }
        }
        if (selectedService == null) {
            throw new IllegalArgumentException("Service not found in this salon");
        }
        LocalDate date = LocalDate.parse(appointmentDate);
        List<String> availableTimes = getAvailableTimes(salonSlug, date);
        if (!availableTimes.contains(appointmentTime)) {
            throw new IllegalArgumentException("Sorry, this appointment time is no longer available");
        }
        Appointment appointment = new Appointment(selectedService.getName(),selectedService.getId(), salon.getId(), date, appointmentTime, customer);

        return appointmentRepository.save(appointment);
    }

    //Generate available one-hour appointment times for a salon.
    public List<String> getAvailableTimes(String salonSlug, LocalDate date) {
        Salon salon = ownerService.findPublicSalonBySlug(salonSlug);
        int selectedDay = date.getDayOfWeek().getValue();
        OpeningDay selectedHours = null;
        for (OpeningDay day : salon.getOpeningHours()) {
            if (day.getDayOfWeek() == selectedDay && day.isOpen()) {
                selectedHours = day;
                break;
            }
        }
        if (selectedHours == null) {
            return new ArrayList<>();
        }
        LocalTime openingTime = LocalTime.parse(selectedHours.getStartTime());
        LocalTime closingTime = LocalTime.parse(selectedHours.getEndTime());
        List<Appointment> existingAppointments =
                appointmentRepository.findBySalonIdAndAppointmentDate(salon.getId(), date);
        List<String> availableTimes = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        // Minute arithmetic avoids wrapping past midnight for late closing times.
        int closingMinute = closingTime.getHour() * 60 + closingTime.getMinute();
        for (int minute = openingTime.getHour() * 60 + openingTime.getMinute();
                minute + 60 <= closingMinute; minute += 60) {
            LocalTime currentTime = LocalTime.of(minute / 60, minute % 60);
            boolean alreadyBooked = false;
            for (Appointment appointment: existingAppointments) {
                LocalTime bookedStart = LocalTime.parse(appointment.getAppointmentTime());
                int bookedMinute = bookedStart.getHour() * 60 + bookedStart.getMinute();
                // Changed opening times can shift the grid; reject every overlapping hour.
                if (minute < bookedMinute + 60 && bookedMinute < minute + 60) {
                    alreadyBooked = true;
                    break;
                }
            }
            if (!alreadyBooked) {
                availableTimes.add(currentTime.format(formatter));
            }
        }
        return availableTimes;
    }
}
