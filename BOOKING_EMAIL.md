# Iteration 1: booking confirmation email

Vaibhav Grover - November 1, 2026

This is my plan for Iteration 1. Email sending is not built yet. The tests below are planned.

## Scope

Send a plain-text confirmation to the customer's registered email after the database confirms that the appointment was saved.

If booking fails, send nothing. If email sending fails, keep the appointment and tell the customer to check My Appointments. Try sending once. Viewing an appointment should not send another email.

This iteration does not include owner emails, welcome emails, cancellation or rescheduling emails, Google Calendar, SMS, reminders, retries, or a resend button.

## Email details

Subject: Your booking at [salon name] is confirmed

Include:

- Customer name and booking reference.
- Salon name and address, if provided.
- Service name.
- Appointment date and start time in Vancouver time.
- Duration: one hour.
- A note to sign in and check My Appointments.

Use the saved booking and account details, not values changed in the browser. Send only to the customer who made the booking. If required details are missing or do not match, keep the booking but do not send the email. If the salon address is missing, leave that line out.

## User stories and tests

These three stories are for Iteration 1. The checks below describe what must work.

### 1. Get confirmation after booking

As a customer, I want an email after my booking succeeds so that I have a record that my appointment was accepted.

- Success: Book an available slot. The appointment saves before one email request is sent.
- Failure: Try an unavailable slot or test a failed save. No confirmation is sent.
- Other check: View the saved appointment or repeat a request that the booking system rejects. No extra email is sent.

### 2. Check the details of my visit

As a customer, I want my appointment details in the email so that I know which salon to visit, which service I booked, and when to arrive.

- Success: The recipient, booking reference, salon, service, date, and time match the saved booking.
- Failure: Test with two customers and salons. One customer's email must never contain the other's details. Do not send if required details are missing.
- Other check: A missing salon address does not stop the confirmation email.

### 3. Understand the outcome when email has a problem

As a customer, I want the confirmation page to show that my appointment is booked even if email sending has a problem so that I do not book again by mistake.

- Success: When the provider accepts the email, the page confirms the booking and says the email was submitted for delivery.
- Failure: If the provider rejects the email, the booking stays saved and the page explains the problem.
- Other checks: A timeout means sending could not be confirmed. Missing email settings should not stop booking, but the page must say email is unavailable.

The confirmation page includes a My Appointments link. It must not show API keys or internal errors. Provider acceptance does not guarantee that the email reaches the inbox.

## Sending approach

The plan is to use Brevo's transactional email API over HTTPS. It supports plain-text messages and uses an API key. Our Java backend can call this API. We will connect to one provider; Mailjet is only a backup option. [Brevo email API](https://developers.brevo.com/docs/send-a-transactional-email)

Brevo's free plan includes 300 emails per day without a credit card. We can verify an individual sender email address, but the account still needs activation and may need help from support. If the sender's domain is not verified, Brevo may change the sender address customers see. [Free plan](https://help.brevo.com/hc/en-us/articles/208589409-About-Brevo-s-pricing-plans), [sender verification](https://help.brevo.com/hc/en-us/articles/208836149-Create-a-new-sender-From-name-and-From-email), [activation](https://help.brevo.com/hc/en-us/articles/115000188150-Troubleshooting-Issues-with-Brevo-SMTP), [sender changes](https://help.brevo.com/hc/en-us/articles/16045394674066-Troubleshooting-Domain-authentication)

Mailjet is a backup if we cannot set up Brevo. Its HTTPS API also supports plain text. It needs a verified sender and may limit sending for new accounts. [Mailjet API](https://dev.mailjet.com/docs/email-api/send-api-v31/send-basic-email), [account limits](https://documentation.mailjet.com/hc/en-us/articles/360042624754-Blocked-or-Limited-Account-Reasons-and-How-to-Restore-Full-Functionality)

No provider account is set up for this feature yet. Sender approval and delivery to customer inboxes still need checking. API keys will stay in environment settings, outside Git. Sending from Render has not been tested. The official documentation was checked on November 1, 2026.
