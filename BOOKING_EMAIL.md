# Iteration 1: booking confirmation email

Vaibhav Grover - November 1, 2026

The message builder and Brevo delivery service are built. Booking does not call the sender yet. The booking flow, real inbox delivery and Render still need testing.

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

The delivery service sends plain-text messages through Brevo's HTTPS API using an API key. It uses Java's HTTP client and the project's Jackson JSON library. No new dependency was added. [Brevo email API](https://developers.brevo.com/docs/send-a-transactional-email)

The Brevo account has a verified Gmail sender named BookEasy and a restricted API key. Transactional sending eligibility is unverified and may need support activation. Brevo may change the visible sender address because Gmail's domain is not authenticated. [Sender verification](https://help.brevo.com/hc/en-us/articles/208836149-Create-a-new-sender-From-name-and-From-email), [activation](https://help.brevo.com/hc/en-us/articles/115000188150-Troubleshooting-Issues-with-Brevo-SMTP), [sender changes](https://help.brevo.com/hc/en-us/articles/16045394674066-Troubleshooting-Domain-authentication)

Brevo's free plan allows 300 emails a day without a credit card. [Free plan](https://help.brevo.com/hc/en-us/articles/208589409-About-Brevo-s-pricing-plans)

Mailjet is only a fallback. It also needs a verified sender and may limit new accounts. [Mailjet API](https://dev.mailjet.com/docs/email-api/send-api-v31/send-basic-email), [account limits](https://documentation.mailjet.com/hc/en-us/articles/360042624754-Blocked-or-Limited-Account-Reasons-and-How-to-Restore-Full-Functionality)

## Delivery settings

The service uses these settings:

- `BOOKEASY_EMAIL_ENABLED`: defaults to `false`. Set to `true` to enable delivery.
- `BOOKEASY_EMAIL_API_KEY`: required private Brevo key.
- `BOOKEASY_EMAIL_SENDER_EMAIL`: required verified sender mailbox.
- `BOOKEASY_EMAIL_SENDER_NAME`: defaults to `BookEasy`.

Disabled or missing settings return `UNAVAILABLE` without a request. The app can still start. The sender allows five seconds to connect and ten seconds for the full response. It makes one request through the HTTP client, with no redirects or automatic retry. This does not guarantee exactly-once delivery.

HTTP 201 with a message ID means `ACCEPTED` for delivery. A clear provider refusal means `REJECTED`. A timeout, connection problem, server error or unclear response means `UNCERTAIN`; the message may still have been accepted. Credentials and internal provider errors are not shown or logged.

For local use, put the settings in `env.local`. Git ignores this file. Use Java properties format, with no `export` or shell quotes:

```properties
BOOKEASY_EMAIL_ENABLED=false
BOOKEASY_EMAIL_API_KEY=replace-with-your-private-key
BOOKEASY_EMAIL_SENDER_EMAIL=replace-with-your-verified-mailbox
BOOKEASY_EMAIL_SENDER_NAME=BookEasy
```

The file is not loaded automatically. Run this from the repository root to load it:

```sh
SPRING_CONFIG_IMPORT='optional:file:./env.local[.properties]' ./mvnw spring-boot:run
```

Keep the key private. On Render, use environment settings instead of uploading the file. Enabling delivery alone does not send anything yet, because booking is not connected. [Spring external configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)

## Delivery tests

All seven tests passed: three message-builder tests, three delivery tests and one startup test. They check the saved details and correct customer, optional address, invalid data, outgoing request, provider rejection, timeout and missing settings. Delivery tests use a local fake server and fake credentials. They send no real emails.

Booking save timing, confirmation-page feedback, real inbox delivery and Render sending are still unverified.
