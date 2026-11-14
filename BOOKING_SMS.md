# Iteration 2: booking confirmation text

Vaibhav Grover - November 12, 2026

My Iteration 2 work lets customers get their booking confirmation as a text message, instead of the email or along with it. It's set up for the team's Render site at https://cmpt276-project-icc0.onrender.com.

## How it works

Customers pick a confirmation preference when they register. SMS and BOTH need a phone number. Any format registration accepts, like (604) 555-1234 or 604.555.1234, gets sent as a +1 number. The Twilio account only sends to Canadian numbers.

- EMAIL gets the email only. Older accounts with nothing saved count as EMAIL.
- SMS gets a text only.
- BOTH gets the email and the text. Each is tried on its own, so if one fails the other still goes.

The text is sent only after the booking is saved. A sending problem never cancels the booking. Failed bookings send nothing, and opening My Appointments or the appointment details later doesn't send anything again. A customer gets a text like this:

```text
BookEasy: Your Haircut at CS_Salon is confirmed for 2026-11-03 at 10:00 (Vancouver time). You can view it in My Appointments.
```

This version covers customer booking confirmation only. Owner texts, reminders, cancellation or rescheduling texts and retries are not part of it.

## User stories and tests

The manual checks below run on the Render site.

### 1. Get my booking confirmation by text

As a customer, I want a text after my booking succeeds so that I have the details on my phone.

- Success: Register with SMS and a phone you can check, then book an open slot. The page shows "Booking Confirmed!" and "Confirmation text submitted for delivery." The text matches the booking in My Appointments.
- Failure: Open the same slot's booking summary in two tabs and confirm both. The second one is refused and only one text comes.
- Other check: Reopen My Appointments and the appointment details. No second text comes.
- Automated tests: smsPreferenceSendsOnlyTheTextAfterTheBookingCommits, sendsTheTextToTheBookingCustomersOwnPhone, sendsNothingForARejectedBooking, sendsNoTextForFailedSavesOrFailedCommits, viewingPagesAfterABookingDoesNotSendAnotherText, includesTheRightDetailsForEachCustomer, normalizesEveryPhoneFormatRegistrationAccepts, refusesAMissingOrInvalidPhone, rejectsInvalidBookingDetails, fitsInOneTextForTypicalNames.
- Sub-tasks done: build the text from the saved booking, turn the stored phone into a +1 number, send it through Twilio after the booking commits, send nothing for failed bookings or page views.

### 2. Get it the way I chose

As a customer, I want my confirmation sent the way I picked at registration so that I get it where I'll actually see it.

- Success: Register one customer each for EMAIL, SMS and BOTH and book a slot with each. EMAIL shows only an email line and gets an email. SMS shows only a text line and gets a text. BOTH shows both lines and gets both messages.
- Failure: For BOTH, a problem with one channel must not stop the other. This can't be forced safely on the live site, so the automated tests cover it.
- Other check: Book twice as the BOTH customer. Each booking sends one email and one text, with no repeats.
- Automated tests: emailPreferenceSendsOnlyTheEmailIncludingLegacyAccounts, smsPreferenceSendsOnlyTheTextAfterTheBookingCommits, bothPreferenceSendsTheEmailAndTheTextOnce, bothPreferenceStillSendsTheTextWhenTheEmailFails, bothPreferenceStillSendsTheEmailWhenTheTextFails.
- Sub-tasks done: read the customer's saved preference after the booking is saved, treat a missing preference as EMAIL, try the email and the text separately, show a separate page line for each.

### 3. Understand the outcome when the text has a problem

As a customer, I want the confirmation page to say what happened to my text so that I don't book again by mistake.

- Success: When Twilio accepts the text, the page shows "Confirmation text submitted for delivery." That doesn't prove the phone got it.
- Failure: When Twilio refuses the text, the booking stays in My Appointments and the page shows "The text message service could not send the confirmation text. Your appointment is still booked."
- Other checks: A timeout or unclear reply shows "Text sending could not be confirmed. The text may still arrive. Your appointment is still booked." If SMS is off, a setting is missing or the customer has no usable phone, the page shows "Confirmation text is unavailable. Your appointment is still booked."
- Automated tests: showsTextFeedbackAndKeepsTheBookingWhenTheTextIsNotSent, showsUncertainFeedbackAndKeepsTheBookingWhenTwilioTimesOut, sendsTheEncodedFormAndRecognizesAcceptance, handlesRejectionAndAnUncertainTimeout, treatsServerErrorsAndMissingSidAsUncertain, doesNotSendWhenDisabledOrMissingSettings.
- Sub-tasks done: sort Twilio's reply into accepted, rejected, uncertain or unavailable, add the four page lines, keep the booking when sending fails, keep Twilio settings off the page.

## SMS setup

The app calls Twilio's Messages API over HTTPS with Java's HTTP client. No new dependency was needed. [Twilio Messages API](https://www.twilio.com/docs/messaging/api/message-resource)

We use a Twilio account with a Canadian phone number, a Standard API key and messaging geo permissions limited to Canada. Real texts go out from that number. SMS is off by default and stays off until all five settings are set.

- `BOOKEASY_SMS_ENABLED`: `true` to send texts. Default is `false`.
- `BOOKEASY_SMS_ACCOUNT_SID`: the Account SID, starts with AC.
- `BOOKEASY_SMS_API_KEY_SID`: the API key SID, starts with SK.
- `BOOKEASY_SMS_API_KEY_SECRET`: the API key secret.
- `BOOKEASY_SMS_FROM_NUMBER`: the Canadian Twilio number in +1 form.

Each booking makes one attempt. HTTP 201 with a message id means accepted. A clear 4xx refusal means rejected. Timeouts, server errors and unclear replies mean uncertain, since Twilio may have sent the text anyway. Missing settings mean unavailable, and no request is made. Secrets are never shown on the page or logged.

## Render setup

Add the five `BOOKEASY_SMS_*` settings to the Render service's environment settings with `BOOKEASY_SMS_ENABLED=true`, then redeploy. The Twilio account owner shares the values privately. They never go in the repo or Discord. To check it on Render, book as a customer with SMS or BOTH and a phone you can check. Compare the page, the phone and My Appointments. If a failure line shows, look in the Twilio console under Monitor, then Logs.

## Tests and walkthrough

All 20 automated SMS tests pass. They use fake credentials and a local fake server, so no real texts are sent.

- BookingSmsBuilderTests (5): the text has the right customer's details, every phone format registration accepts becomes +1, bad phones and booking details are refused, and a typical text fits in 160 characters.
- TwilioSmsSenderTests (4): the request Twilio receives and how accepted, rejected, uncertain and unavailable replies are read.
- BookingSmsIntegrationTests (11): full bookings through the controller for each preference, page feedback, failed bookings, page views and timeouts.

The manual walkthrough on the Render site:

1. Register an SMS customer and a BOTH customer, each with a Canadian phone you can check.
2. Book an open slot as each one and read the lines on the confirmation page.
3. Compare each text with the booking in My Appointments, and check the BOTH customer's email too.
4. Reopen My Appointments and the details. No second text should come.

On November 12 I ran the full flow by hand. I set up a salon as an owner, registered a customer with BOTH and booked an open slot. The confirmation email came in and the text came to my phone. The booking showed in My Appointments. The case where Twilio refuses a text is covered by the automated tests.

## Contribution and demo

I built the SMS settings, the Twilio sender, the text builder, the channel choice in the booking controller, the text feedback on the confirmation page, the 20 tests and this guide. Keeping the text builder apart from the sender let me test each failure case on its own.

The team demo can follow the customer flow:

1. Open https://cmpt276-project-icc0.onrender.com, sign in as an SMS or BOTH customer and book a slot.
2. Show the text line on the confirmation page, then the text on the phone next to My Appointments, with private details hidden.
3. Use the automated test results to explain how the booking stays saved when a text has a problem.
