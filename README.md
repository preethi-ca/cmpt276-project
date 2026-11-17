# BookEasy CMPT 276 Project
Group 2: Maria Jiang, Preethi Chidambaram, Draco Dominus, Vaibhav Grover, and (Edwin) Junyu Chen
Project: Hair Salon Appointment Booking and Management System

## Project Overview 
BookEasy is a web-based appointment booking and management system designed for beauty salons and their customers. It provides customers with a convenient way to book appointments while helping salon owners keep their schedules organized. 

Each salon has a dedicated page where customers can browse services and prices, check availability, and start a booking. Customers can select a service, date, and time before logging in to review and confirm their appointment.

Live application: https://cmpt276-project-icc0.onrender.com
The application can also be run locally at http://localhost:8080 after completing the installation steps below.

## Iteration 1
Our main focus for Iteration 1 was building a basic login/logout for customers and salon owners, a customer dashboard, and salon owner dashboard, both with basic functionalities. Salon owners can manage their services, prices, and availability, and customers can book, view, and cancel appointments.

Key work completed included:
1. **Authentication:** Customer and salon owner account creation and login and logout functionality.
2. **Salon owner dashboard:** A dashboard for owners to manage their services, avaliability, salon settings
3. **Customer dashboard:** A dashboard for customers to view and manage their appointments.
4. **Public salon page:** A page where customers can browse salon information and access booking options.
5. **Testing:** Controller tests for authentication, booking, appointments, and public salon pages.
6. **Styling:** CSS and integrated Bootstrap to improve the appearance and consistency of the pages.
7. **Notification:** Send email confirmation messages to customers after successfully booking an appointment.

## Iteration 2

Our main focus for Iteration 2 was improving the customer booking flow, expanding account and appointment management, and adding analytics to the salon owner dashboard.

Key work completed included:

1. **Guest booking flow:** Customers can select a service, date, and time as guests. Their selections are kept when they log in to review and confirm the booking.
2. **Appointment management:** Customers can reschedule appointments. Booking and rescheduling check the full 60-minute slot for overlaps, including after a salon changes its opening hours.
3. **Account settings:** Customers and salon owners can update their name, phone number, and notification preferences, and change their password.
4. **Booking confirmations:** Customers can choose to receive booking confirmations by email, SMS, or both.
5. **Google Calendar integration:** Customers can connect Google Calendar to keep their calendar events in sync when they book, reschedule, or cancel an appointment.
6. **Salon analytics:** Salon owners can view their total number of appointments, most popular service, busiest appointment time, and estimated revenue.

## Application Features

### Authentication Features
- Customer and salon owner registration.
- Login and logout functionality.
- Account access based on user roles.
- Customers and salon owners can update their name, phone number, and notification preferences from their dashboard.
- Users can change their password by entering their current password and a new password.

### Customer Features
- Browse public salon pages or select a salon from the customer dashboard.
- Choose a service, date, and time as a guest, then log in to review and confirm the booking.
- View appointment details, change the date and time of an existing appointment, or cancel it.
- Book or reschedule into an available 60-minute slot. Back-to-back appointments are allowed.
- Connect Google Calendar from the dashboard when calendar sync is enabled.

### Salon Features
- Manage salon information and profiles.
- Add, edit, and delete salon services and manage their prices.
- Set opening hours and appointment availability.
- View a paginated list of today's and upcoming appointments, with counts for today, this week, and appointments after today.
- View salon analytics, including the total number of appointments, the most popular service, the busiest appointment time, and estimated revenue.

### Notification Features
- Customers can choose email, SMS, or both for booking confirmations. SMS requires a phone number.
- After a booking is saved, BookEasy attempts delivery through the customer's selected channels when those channels are enabled. A delivery failure does not cancel the appointment.
- For customers who connect Google Calendar, booking creates a calendar event, rescheduling updates it, and cancelling removes it.

## Installation and Setup
This project requires the following to be dowlnloaded and setup Java JDK, Git, PostgreSQL, A code editor (Visual Studio Code)

#### Clone the repository using:
```sh
git clone https://github.com/preethi-ca/cmpt276-project.git
cd cmpt276-project
```

To start the application use:./mvnw spring-boot:run
Open the application in your browser:  
```sh
http://localhost:8080
```

## Deployment
BookEasy is deployed using Render
Live Application: https://cmpt276-project-icc0.onrender.com  
The Docker build uses Java 17 and packages the application with tests skipped. Run the test suite separately using the commands below.

## Testing
The project uses Maven to run automated tests.
On macOS or Linux:

```sh
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```
- Controller tests cover areas: authentication, appointment management, booking functionality, public salon pages, guest booking, and appointment management.
- Service tests cover account and password management, booking rules, and salon analytics.
- Integration tests cover email and SMS confirmations, Google Calendar connections, and calendar updates when appointments change.
  
## How to Book an Appointment
Customers can book an appointment through the BookEasy website by following these steps:
1. **Open the Application:** Visit the BookEasy live application: https://cmpt276-project-icc0.onrender.com
2. **Create an Account or Log In:** Register as a customer or log in to an existing customer account.
3. **Browse Salons:** View the available salon pages and select the salon you want to visit. (Example [if salon name is Sally Salon]: /salons/sally-salon)
4. **Select an Appointment:** Choose your preferred service, date, and available time slot. You can make these selections as a guest.
5. **Confirm Booking:** Submit your appointment request to complete the booking.
6. **Check Your Confirmation:** The confirmation page shows the booking result. Depending on your preferences and the channels enabled, BookEasy may also send an email, a text message, or both. If you have connected Google Calendar, BookEasy also attempts to add the appointment to your calendar.
7. **Manage Appointments:** Open **My Appointments** to view appointment details, change the date and time, or cancel an appointment.

## System Architecture
BookEasy follows the MVC architecture using Spring Boot
- **Model:** Manages application data, including customer accounts, salon information, services, and appointments.
- **View:** Provides the user interface through HTML, CSS, JavaScript, Bootstrap, and Thymeleaf templates.
- **Controller:** Handles user requests and connects the frontend with the backend
- **Database:** PostgreSQL stores application information like customer accounts, salon details, and appointments.
- **Booking Notifications:** BookEasy uses Brevo for email confirmations and Twilio for SMS confirmations. Delivery is attempted after the booking is saved.
- **Google Calendar:** Customers can authorize BookEasy to create, update, and remove calendar events for their bookings. If calendar sync fails, the appointment change remains saved in BookEasy.

## Iteration 3 Next Steps

The following improvements are planned for Iteration 3:

- **Salon Pages:** Add images to public salon pages to make them more visually appealing and help customers learn more about each salon.
- **Service Browsing:** Let customers sort salon services by price, highest rating, or alphabetical order.
- **Customer Account and Booking Management:** Allow customers to save favourite salons, revisit recently viewed salons, view their past appointment history, and quickly rebook a previous service.
- **Appointment Availability:** When customers book for the current day, show only time slots that start after the current time. For example, at 2:00 PM, only slots starting after 2:00 PM should be available.
- **Notifications:** Further notification improvements are planned. Details are still to be decided.
- **Salon Analytics:** Add date range filters, booking and revenue trends, and more detailed statistics to help owners understand their salon's performance over time.

## Project Repository
GitHub Repository: https://github.com/preethi-ca/cmpt276-project
