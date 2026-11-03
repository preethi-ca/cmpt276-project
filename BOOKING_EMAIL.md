# Iteration 1: booking confirmation email

Vaibhav Grover - November 1, 2026

My Iteration 1 work is the customer booking-confirmation email. Local booking and real inbox delivery worked on November 1. Render sending still needs checking after team integration.

## How it works

After a customer confirms a booking, the app saves the appointment and waits for the database to commit it. It then tries once to send a plain-text email to that customer's registered address.

An unsuccessful booking sends no email. Email problems leave a successful booking saved, with an explanation and a My Appointments link on the confirmation page. Opening an existing appointment does not send another message.

This version covers customer booking confirmation only. Owner emails, welcome emails, cancellation or rescheduling emails, Google Calendar, SMS, reminders, retries and a resend button are outside this iteration.

The subject is **Your booking at [salon name] is confirmed**. The email contains:

- The customer's name and booking reference.
- The salon name and address, if available.
- The service, date and start time in Vancouver time.
- The one-hour duration and a note about signing in to My Appointments.

The details come from the saved appointment, its customer account and the matching salon. Missing or mismatched required details stop the email without removing the booking. A missing address is left out.

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

## Email setup

The app uses Brevo's HTTPS API with Java's HTTP client and the existing Jackson library. No new dependency was needed. [Brevo email API](https://developers.brevo.com/docs/send-a-transactional-email)

The account has a verified Gmail sender named BookEasy and a restricted API key. The test email displayed BookEasy. Its full From address was not checked; Brevo may change it because Gmail's domain is not authenticated. [Sender verification](https://help.brevo.com/hc/en-us/articles/208836149-Create-a-new-sender-From-name-and-From-email), [activation help](https://help.brevo.com/hc/en-us/articles/115000188150-Troubleshooting-Issues-with-Brevo-SMTP), [sender changes](https://help.brevo.com/hc/en-us/articles/16045394674066-Troubleshooting-Domain-authentication)

Brevo's free plan allows 300 emails a day without a credit card. [Free plan](https://help.brevo.com/hc/en-us/articles/208589409-About-Brevo-s-pricing-plans)

Mailjet is only a fallback and has not been implemented. It also needs a verified sender and may limit new accounts. [Mailjet API](https://dev.mailjet.com/docs/email-api/send-api-v31/send-basic-email), [account limits](https://documentation.mailjet.com/hc/en-us/articles/360042624754-Blocked-or-Limited-Account-Reasons-and-How-to-Restore-Full-Functionality)

For live email delivery, configure these four settings:

- `BOOKEASY_EMAIL_ENABLED`: set to `true` to enable delivery.
- `BOOKEASY_EMAIL_API_KEY`: required private Brevo key.
- `BOOKEASY_EMAIL_SENDER_EMAIL`: required verified sender mailbox.
- `BOOKEASY_EMAIL_SENDER_NAME`: defaults to `BookEasy`.

Disabled or missing settings give `UNAVAILABLE` without a request. The app can still start and accept bookings.

Sending allows five seconds to connect and ten seconds for the full response, with no redirects or automatic retry. One attempt does not guarantee exactly-once delivery.

HTTP 201 with a message ID means `ACCEPTED`; a clear refusal means `REJECTED`. Timeouts, connection problems, server errors or unclear responses mean `UNCERTAIN`, since Brevo may have accepted the message. Credentials and internal provider errors are not shown or logged.

## Local setup

Use JDK 21, which was used for verification. From the repository root, stop the running app and build the branch:

```sh
git fetch origin
git switch feature/booking-confirmation-email
git pull --ff-only
./mvnw clean package
```

The build includes the tests. Email tests use fake credentials, so they do not need the Brevo key. Booking and email sending run in the same app.

Create `env.local` beside `pom.xml` in Java properties format:

```properties
BOOKEASY_EMAIL_ENABLED=true
BOOKEASY_EMAIL_API_KEY=replace-with-your-private-key
BOOKEASY_EMAIL_SENDER_EMAIL=replace-with-your-verified-mailbox
BOOKEASY_EMAIL_SENDER_NAME=BookEasy
```

Ask me privately for the existing restricted Brevo key and verified sender address. Use those values in your local file; no new Brevo account is needed.

Start the built jar with the settings imported explicitly, since Spring does not load `env.local` automatically:

```sh
SPRING_CONFIG_IMPORT='optional:file:./env.local[.properties]' \
java -jar target/cmpt276-project-0.0.1-SNAPSHOT.jar
```

The app runs at `http://localhost:8080`. Restart after changing the settings. The current demo data resets on restart, so recreate the salon, service, opening hours and test accounts as needed. [Spring external configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)

## Quick check

1. Book an available slot as a customer whose registered email you can check. Use the salon's public slug, for example `http://localhost:8080/salons/cs-salon`.
2. Check for **Booking Confirmed!** and **Confirmation email submitted for delivery.** Compare My Appointments and the received email with the booking details.
3. Viewing the appointment should not resend the email. Booking the same occupied slot again should be rejected without another appointment or email.

Delivery can lag behind acceptance; one local test took about a minute. If email is unavailable, check the imported settings. The booking remains saved if sending fails.

## Render setup

After integration, add the same four settings to the Render service's private environment settings, keeping `BOOKEASY_EMAIL_ENABLED=true`. Use the key and verified sender address there, rather than uploading `env.local`.

Rebuild and redeploy the integrated branch, then verify a booking, My Appointments and inbox delivery on the Render URL. This deployed check is still pending.

## Tests and walkthrough

All eleven tests passed in a separate offline package build: three builder, three delivery, four booking integration and one startup test. Fake credentials and a local HTTP server keep automated tests from sending real emails.

- Story 1: a separate database connection sees the committed booking before sending, with no booking transaction active. Tests also cover rejected slots, save and injected commit-stage failures, and no resend from views or rejected repeats.
- Story 2: builder tests check saved details, separate customers, optional address and invalid data. The live email also matched the saved test booking.
- Story 3: delivery and integration tests check acceptance, rejection, timeout, missing settings, preserved appointments and truthful page feedback.

The November 1 local walkthrough covered registration/login, salon/services/hours setup, booking, My Appointments and details. A repeated booking was rejected. Brevo accepted one real email, and the inbox screenshot matched the subject, customer, booking reference, salon/address, service, Vancouver date/time, duration and account note. The packaged app also loaded the external settings successfully.

## Contribution and demo

My contribution covers the three stories, message builder, configurable HTTPS sender, booking integration, confirmation feedback and tests. Keeping message creation separate from delivery made failure cases easier to check. The live test showed why acceptance and inbox receipt are separate results.

After integration, the team demo can follow the customer flow:

1. Open the Render site with its URL visible, sign in as a test customer and book a slot.
2. Open My Appointments from the confirmation page and check the saved details.
3. Compare the received email with the booking, with private account information hidden.
4. Use the fake-test results to explain how a booking stays saved when email has a problem.

Render setup and sending still need checking after team integration. H2 accounts/bookings and in-memory salon data reset on restart, so this local check does not verify persistent deployment or complete the team's submission.
