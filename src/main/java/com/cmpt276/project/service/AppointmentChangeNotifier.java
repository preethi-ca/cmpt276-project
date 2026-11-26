package com.cmpt276.project.service;

import com.cmpt276.project.model.Appointment;
import com.cmpt276.project.model.AppointmentNoticeResult;
import com.cmpt276.project.model.BookingConfirmationMessage;
import com.cmpt276.project.model.BookingEmailResult;
import com.cmpt276.project.model.BookingSmsMessage;
import com.cmpt276.project.model.BookingSmsResult;
import com.cmpt276.project.model.NotificationPreference;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.service.OwnerService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

// tells the customer by email or text when their appointment is moved or cancelled
@Service
public class AppointmentChangeNotifier {
    private final BrevoEmailSender emailSender;
    private final TwilioSmsSender smsSender;
    private final OwnerService ownerService;
    private final BookingConfirmationBuilder emailBuilder = new BookingConfirmationBuilder();
    private final BookingSmsBuilder smsBuilder = new BookingSmsBuilder();

    public AppointmentChangeNotifier(BrevoEmailSender emailSender, TwilioSmsSender smsSender, OwnerService ownerService) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.ownerService = ownerService;
    }

    // the change is already saved here, so this never throws
    public AppointmentNoticeResult sendRescheduled(Appointment appointment, LocalDate previousDate, String previousTime) {
        Salon salon = findSalon(appointment);
        NotificationPreference preference = findPreference(appointment);
        String ending = "Your appointment change is still saved.";
        String emailMessage = null;
        String smsMessage = null;

        if (preference == NotificationPreference.EMAIL || preference == NotificationPreference.BOTH) {
            BookingEmailResult emailResult = BookingEmailResult.UNAVAILABLE;
            try {
                BookingConfirmationMessage email = emailBuilder.buildRescheduled(appointment, salon, previousDate, previousTime);
                emailResult = sendEmail(email);
            } catch (RuntimeException error) {
                // sendEmail never throws, so this is missing customer or salon details
            }
            emailMessage = emailFeedback(emailResult, "Email with the new time submitted.", ending);
        }
        if (preference == NotificationPreference.SMS || preference == NotificationPreference.BOTH) {
            BookingSmsResult smsResult = BookingSmsResult.UNAVAILABLE;
            try {
                BookingSmsMessage text = smsBuilder.buildRescheduled(appointment, salon, previousDate, previousTime);
                smsResult = sendText(text);
            } catch (RuntimeException error) {
                // a missing or invalid phone, nothing was sent
            }
            smsMessage = smsFeedback(smsResult, "Text with the new time submitted for delivery.", ending);
        }
        return new AppointmentNoticeResult(emailMessage, smsMessage);
    }

    // the appointment is already deleted here, so this never throws
    public AppointmentNoticeResult sendCancelled(Appointment appointment) {
        Salon salon = findSalon(appointment);
        NotificationPreference preference = findPreference(appointment);
        String ending = "Your appointment is still cancelled.";
        String emailMessage = null;
        String smsMessage = null;

        if (preference == NotificationPreference.EMAIL || preference == NotificationPreference.BOTH) {
            BookingEmailResult emailResult = BookingEmailResult.UNAVAILABLE;
            try {
                BookingConfirmationMessage email = emailBuilder.buildCancelled(appointment, salon);
                emailResult = sendEmail(email);
            } catch (RuntimeException error) {
                // sendEmail never throws, so this is missing customer or salon details
            }
            emailMessage = emailFeedback(emailResult, "Cancellation email submitted.", ending);
        }
        if (preference == NotificationPreference.SMS || preference == NotificationPreference.BOTH) {
            BookingSmsResult smsResult = BookingSmsResult.UNAVAILABLE;
            try {
                BookingSmsMessage text = smsBuilder.buildCancelled(appointment, salon);
                smsResult = sendText(text);
            } catch (RuntimeException error) {
                // a missing or invalid phone, nothing was sent
            }
            smsMessage = smsFeedback(smsResult, "Cancellation text submitted for delivery.", ending);
        }
        return new AppointmentNoticeResult(emailMessage, smsMessage);
    }

    // null makes the builders throw, which shows as unavailable
    private Salon findSalon(Appointment appointment) {
        try {
            return ownerService.findPublicSalon(appointment.getSalonId());
        } catch (RuntimeException error) {
            return null;
        }
    }

    private NotificationPreference findPreference(Appointment appointment) {
        if (appointment == null || appointment.getCustomer() == null) {
            return NotificationPreference.EMAIL;
        }
        return appointment.getCustomer().getNotificationPreference();
    }

    private BookingEmailResult sendEmail(BookingConfirmationMessage email) {
        try {
            return emailSender.send(email);
        } catch (RuntimeException error) {
            // sending may have started, so a definite failure would be misleading
            return BookingEmailResult.UNCERTAIN;
        }
    }

    private BookingSmsResult sendText(BookingSmsMessage text) {
        try {
            return smsSender.send(text.getRecipient(), text.getText());
        } catch (RuntimeException error) {
            return BookingSmsResult.UNCERTAIN;
        }
    }

    private String emailFeedback(BookingEmailResult result, String acceptedText, String ending) {
        if (result == BookingEmailResult.ACCEPTED) {
            return acceptedText;
        }
        if (result == BookingEmailResult.REJECTED) {
            return "The email provider rejected the email. " + ending;
        }
        if (result == BookingEmailResult.UNCERTAIN) {
            return "Email sending could not be confirmed, the email may still arrive. " + ending;
        }
        return "Email is unavailable. " + ending;
    }

    private String smsFeedback(BookingSmsResult result, String acceptedText, String ending) {
        if (result == BookingSmsResult.ACCEPTED) {
            return acceptedText;
        }
        if (result == BookingSmsResult.REJECTED) {
            return "The text message service could not send the text. " + ending;
        }
        if (result == BookingSmsResult.UNCERTAIN) {
            return "Text sending could not be confirmed. The text may still arrive. " + ending;
        }
        return "Text is unavailable. " + ending;
    }
}
