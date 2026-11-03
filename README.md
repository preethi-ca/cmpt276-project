# BookEasy CMPT 276 Project
Group 2: Maria Jiang, Preethi Chidambaram, Draco Dominus, Vaibhav Grover, and (Edwin) Junyu Chen
Project: Hair Salon Appointment Booking and Management System

## Project Overview 
BookEasy is a web-based appointment booking and management system designed for beauty salons and their customers. It provides customers with a convenient way to book appointments while helping salon owners keep their schedules organized. 

Each salon will have a dedicated page where customers can browse services and prices, check availability, and book appointments.

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
6. **tyling:** CSS and integrated Bootstrap to improve the appearance and consistency of the pages.
7. **Notification:** Send email confirmation messages to customers after successfully booking an appointment.

## Application Features
### Authentication Features
- Customer and salon owner registration.
- Login and logout functionality.
- Account access based on user roles.

### Customer Features
- Browse public salon pages.
- Book and cancel appointments.
- View current appointments.

### Salon Features
- Manage salon information and profiles.
- Add and manage salon services and prices.
- Manage appointment availability.

### Notification Features
- Send email confirmation messages to customers after successfully booking an appointment.

## Installation and Setup
This project requires the following to be dowlnloaded and setup Java JDK, Git, PostgreSQL, A code editor (Visual Studio Code)

#### Clone the repository using:
```git clone https://github.com/preethi-ca/cmpt276-project.git```
```cd cmpt276-project```

To start the application use:./mvnw spring-boot:run
Open the application in your browser: http://localhost:8080

## Deployment
BookEasy is deployed using Render
Live Application: https://cmpt276-project-icc0.onrender.com

## Testing
The project uses Maven to run automated tests.
To run the tests, use:./mvnw test
- Controller tests cover areas: authentication, appointment management, booking functionality, public salon pages

## How to Book an Appointment
Customers can book an appointment through the BookEasy website by following these steps:
1. **Open the Application:** Visit the BookEasy live application: https://cmpt276-project-icc0.onrender.com
2. **Create an Account or Log In:** Register as a customer or log in to an existing customer account.
3. **Browse Salons:** View the available salon pages and select the salon you want to visit. (Example [if salon name is Sally Salon]: /salons/sally-salon)
4. **Select an Appointment:** Choose your preferred service, date, and available time slot.
5. **Confirm Booking:** Submit your appointment request to complete the booking.
6. **Email Confirmation:** After successfully booking, you will receive an email confirmation with your appointment details.
7. **Manage Appointments:** Visit your customer dashboard to view or cancel your appointments.

## System Architecture
BookEasy follows the MVC architecture using Spring Boot
- **Model:** Manages application data, including customer accounts, salon information, services, and appointments.
- **View:** Provides the user interface through HTML, CSS, JavaScript, Bootstrap, and Thymeleaf templates.
- **Controller:** Handles user requests and connects the frontend with the backend
- **Database:** PostgreSQL stores application information like customer accounts, salon details, and appointments.
- **Email Notifications:** The application sends email confirmation messages to customers after successful bookings.

## Known Issues/Limitations and Future Improvements
- **Search and Discoverability:** Customers may have difficulty finding salons if they do not already know the salon's name or that it exists. More advanced search, filtering, and salon discovery features are needed.
- **User Flow:** Some parts of the application could have more intuitive navigation. The steps customers follow to browse salons, select services, and complete bookings could be simplified.
- **Appointment Editing:** Customers currently cannot edit existing appointments.
- **Appointment Organization:** Upcoming and past appointments could be displayed separately to make appointment management easier.
- **Booking Restrictions:** The system should prevent customers from booking appointments for dates that have already passed.
- **Account Management:** Customers and salon owners could have additional account management options, such as changing passwords and adding phone numbers.
- **Notifications:** phone number confirmations and appointment reminders could be added.
- **Google Calendar Integration:** Integrating the Google Calendar API could allow customers to add their appointments to their personal calendars.
- **User Interface:** Further improvements could be made to the overall appearance and user experience.


## Project Repository
GitHub Repository: https://github.com/preethi-ca/cmpt276-project
