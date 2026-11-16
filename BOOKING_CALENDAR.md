# Iteration 2: Google Calendar sync

Vaibhav Grover - November 14, 2026

## How it works

Customers connect their Google Calendar from the dashboard. BookEasy asks only for `calendar.events.owned`, so it can add, change and delete events but never reads other events. Booking adds a one hour event in Vancouver time, editing moves it and cancelling removes it. Disconnect revokes BookEasy's access, and events already added stay. A Google problem never undoes a booking, it only changes the message. Only the refresh token, connection time and event IDs are stored.

## User stories and tests

### 1. Connect my Google Calendar

As a customer, I want to connect my Google Calendar so that my bookings show up there.

- Manual test: Click Connect Google Calendar and allow access to see "Google Calendar connected." Pressing Cancel on Google's screen shows "Google Calendar was not connected."
- Automated tests: GoogleCalendarControllerTests, GoogleCalendarStorageTests, PublicInformationControllerTests
- Sub-tasks: calendar settings, one connection per customer, random state check, disconnect and revoke, About, Privacy and Terms pages

### 2. Add each booking to my calendar

As a customer, I want each booking added to my Google Calendar automatically so that I don't have to type it in myself.

- Manual test: Book an open slot while connected. The page shows "Added to your Google Calendar." and reopening My Appointments adds no second event.
- Automated tests: GoogleCalendarBookingTests, GoogleCalendarSyncTests, GoogleCalendarEventBuilderTests, GoogleCalendarClientTests
- Sub-tasks: build the event, add it after the booking is saved, save the event ID, skip duplicates, show the confirmation line

### 3. Keep my calendar in step with my changes

As a customer, I want my Google Calendar to follow my changes so that it never shows an old time.

- Manual test: Editing the date or time shows "Your Google Calendar event was updated." and cancelling shows "Removed from your Google Calendar." If access is removed in Google first, editing shows "Couldn't update Google Calendar. Your appointment change is still saved."
- Automated tests: GoogleCalendarAppointmentTests, GoogleCalendarSyncTests, GoogleCalendarBookingTests
- Sub-tasks: update the same event, remove it before deleting the appointment, catch every Google problem, show a line for each result

## Setup

Sync stays off until all four settings are set: `BOOKEASY_CALENDAR_ENABLED`, `BOOKEASY_CALENDAR_CLIENT_ID`, `BOOKEASY_CALENDAR_CLIENT_SECRET` and `BOOKEASY_CALENDAR_REDIRECT_URI`. On Render the redirect URI is https://cmpt276-project-icc0.onrender.com/calendar/google/callback. The values go only in Render's environment settings. The Google Cloud project has the Calendar API on, a consent screen with About, Privacy and Terms links and a Web OAuth client with the Render callback.

## Tests

All 69 automated calendar tests pass. No test calls Google. On November 14 I ran the full flow by hand with my own Google account. Booking added the event at the right time, editing moved it, cancelling removed it and disconnecting cleared the connection.
