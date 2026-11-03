# BookEasy user stories: Iteration 1

These are the stories we delivered in Iteration 1, grouped by epic. Each one is written as "As a [persona], I want [action], so that [result]". It's followed by the checks that show it works (success) and how it handles mistakes (failure). These checks are the acceptance criteria and the test cases.

To try them, open https://cmpt276-project-icc0.onrender.com/ and register a Customer or Salon Owner account.

## Who uses BookEasy

- **Customer:** books appointments at a salon. Persona: *Maya Chen*, who wants to book a haircut without calling during business hours.
- **Salon owner:** runs one salon and manages its profile, services and hours. Persona: *Morgan Smith*, who wants online bookings to keep the schedule organized.
- **Guest:** anyone not signed in. Guests can view salon pages but can't book.

Every appointment is one hour long.

## Epic 1: Accounts and roles

### 1.1 Register
As a new visitor, I want to create a customer or salon owner account, so that I can book appointments or manage my salon.
- Success: Name, a new email, a password of 8+ characters and an account type → the login page says "Account created successfully! Please log in."
- Failure: An email that's already registered → "This email is already registered. Please sign in."
- Failure: A badly formed email or a short password → the form shows what to fix, and no account is created.

### 1.2 Sign in
As a registered user, I want to sign in with my email and password, so that I land on the page for my role.
- Success: Maya signs in → customer dashboard. Morgan signs in → salon setup page (or the owner dashboard once the salon exists).
- Failure: A wrong password or an unknown email → "Invalid email or password. Please try again."

The login is our own (not Google sign-in), and passwords are stored only as hashes.

### 1.3 Log out
As a signed-in user, I want to log out, so that nobody else can use my account on a shared computer.
- Success: **Log Out** (customer) or **Sign out** (owner) → the login page says "You have successfully logged out."
- Failure: Opening My Appointments after logging out → sent back to the login page.

### 1.4 See only my own pages
As a customer or salon owner, I want to see only the pages for my role, so that my account stays simple and other people's data stays private.
- Success: Customers see the customer dashboard and booking pages. Owners see the owner workspace for their own salon only.
- Failure: A customer opening an owner page, or an owner opening a customer page → "Access was denied" (403).
- Failure: A guest opening any account page → sent to the login page.

## Epic 2: Salon setup

### 2.1 Create my salon
As a salon owner, I want to create my salon's profile, so that customers have a page where they can book me.
- Success: Salon name (plus an optional address and description) → "Your salon is created." The salon gets a public link such as `/salons/sally-salon`.
- Failure: A blank name, or one longer than 80 characters → an error, and nothing is saved.
- Failure: An owner who already has a salon can't create a second one.

### 2.2 Edit my salon
As a salon owner, I want to update my salon's name, address and description, so that customers see correct information.
- Success: Save changes → "Salon profile saved." and the public page updates. The public link stays the same, so shared links keep working.
- Failure: Clearing the name → "Enter a name." and the old details are kept.

### 2.3 Add services and prices
As a salon owner, I want to list my services with prices, so that customers know what they can book and what it costs.
- Success: "Haircut", 30.00 → "Service added to your menu." It shows on my list and on the public page.
- Failure: A name that already exists, even with different capital letters → "A service with this name already exists."
- Failure: A price that's negative, not a number or over 99999.99 → an error asking for a valid price.

### 2.4 Set opening hours
As a salon owner, I want to set the days and hours I'm open, so that customers can only book when I'm available.
- Success: Mon–Fri 09:00–18:00, Sunday closed → "Weekly opening hours saved." Monday then offers start times from 09:00 to 17:00.
- Failure: Closing less than an hour after opening → "Closing time must be at least one hour after opening."
- Failure: Trying to change hours after saving → not allowed in this version.

### 2.5 View a salon's page
As a customer, I want to see a salon's services, prices and hours, so that I can decide what to book.
- Success: Anyone can open `/salons/sally-salon` and see services, prices, hours and a **Book this service** button.
- Failure: A guest who selects **Book this service** is asked to sign in first.
- Failure: A link to a salon that doesn't exist → "Page not found."

## Epic 3: Booking appointments

### 3.1 Pick a date and time
As a customer, I want to select an appointment date and time, so that I can choose when to visit the salon.
- Success: An open day lists the free one-hour start times. Times already booked aren't shown.
- Failure: A closed day → "Sorry, salon is closed on the selected date".
- Failure: A fully booked day → "Sorry, there are no available appointment times for this date".

### 3.2 Review before confirming
As a customer, I want to review my booking details before confirming, so that I can verify my appointment information.
- Success: The summary shows the salon, service, date, time and price.
- Failure: Leaving the summary without pressing **Confirm Booking** → nothing is saved.

### 3.3 Confirm my booking
As a customer, I want to confirm my booking, so that my time is reserved.
- Success: **Confirm Booking** → "Booking Confirmed!" The appointment appears in My Appointments, and that time disappears for other customers.
- Failure: Someone else booked the same time first → "Sorry, this appointment time is no longer available". The customer picks another time.

### 3.4 See my appointments
As a customer, I want to see my appointments and their details, so that I know where and when I'm going.
- Success: My Appointments lists each booking. **View Details** shows the salon address, service, date, time and price.
- Success: With no bookings, the page says "You currently have no appointments".
- Failure: A customer can't see another customer's appointments, even by changing the link.

### 3.5 Cancel an appointment
As a customer, I want to cancel an appointment I can't make, so that the time is free for someone else.
- Success: **Cancel Appointment** → "Your appointment has been cancelled", and the time can be booked again.
- Failure: A customer can't cancel someone else's appointment.

## Epic 4: Notifications

After every successful booking, the customer gets a confirmation email with their booking details.

### 4.1 Get a confirmation email
As a customer, I want an email after my booking succeeds, so that I have a record of it.
- Success: A saved booking sends one email.
- Failure: A booking that fails sends nothing. Viewing an appointment doesn't send another email.

### 4.2 See my visit details in the email
As a customer, I want my appointment details in the email, so that I know where to go and when.
- Success: The email shows the booking reference, salon, service, date and time from the saved booking.
- Failure: One customer's email never contains another customer's details.

### 4.3 Know my booking is safe if email fails
As a customer, I want the confirmation page to tell me my appointment is booked even if the email has a problem, so that I don't book twice.
- Success: When the email is accepted, the page says "Confirmation email submitted for delivery."
- Failure: When the email is rejected, times out or isn't set up, the booking stays saved and the page says so.

## Automated tests

Automated tests cover registration (1.1), sign-in (1.2), the salon page (2.5), date and time (3.1), the summary and confirmation (3.2, 3.3), My Appointments (3.4) and all three email stories (4.1–4.3). The other checks above are tested by hand.

## Future work

Planned for later iterations:

- **Booking:** reschedule, full double-booking prevention, block past dates, a past appointments page, keep choices on refresh.
- **Salons:** a salon list on the customer dashboard, edit or delete services, change opening hours, a photo gallery, an owner view of appointments.
- **Accounts:** change password, phone number.
- **Calendar and notifications:** Google Calendar sync, cancellation and reschedule notices, reminders 48 hours before by email or SMS.
- **Analytics:** booking and revenue reports for owners.
- **Other ideas:** reviews and ratings, employee management.