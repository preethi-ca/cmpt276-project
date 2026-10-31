# CMPT 276 Project Proposal
Group 2: Maria Jiang, Preethi Chidambaram, Draco Dominus, Vaibhav Grover, and (Edwin) Junyu Chen
Project: Hair Salon Appointment Booking and Management System

## Project Introduction 
BookEasy is a web-based appointment booking and management system designed for hair salons and their customers. It provides customers with a convenient way to book appointments while helping salon owners keep their schedules organized. Each salon will have a dedicated page where customers can browse services and prices, check availability, and book appointments.

The system serves both sides of the booking process. Customers need an easy way to book, view, cancel, or reschedule appointments, while salon owners need a clear overview of upcoming bookings and control over their services, prices, and available hours. BookEasy will bring these tasks together in one application. Customers can manage bookings through their accounts, while salon owners can use a dashboard to update salon information and manage their schedules. The system will prevent double bookings by checking existing reservations before confirming new appointments. Its Google Calendar integration and notifications will also help customers and salon owners keep track of appointments and changes. By simplifying these everyday tasks, BookEasy aims to reduce unnecessary communication, improve organization, and make appointment scheduling more convenient for everyone.

## Existing Solutions and Competitive Analysis
Businesses currently manage appointments in several ways. Some use phone calls, text messages, email, spreadsheets, paper calendars, or general digital calendars. With these methods, customers have to contact a business and wait for staff to manually compare schedules to find availability, which requires a lot of back-and-forth communication and limits booking to business hours. If appointments are not tracked carefully, problems such as double bookings, missed appointments, and scheduling conflicts can also occur. Other businesses use existing online appointment-booking platforms, such as Square Appointments, Setmore, and Calendly. Therefore, BookEasy does not claim that online appointment booking is a new solution. Instead, BookEasy aims to make appointment scheduling simpler and more accessible for both businesses and customers, with convenient 24/7 online booking. It provides businesses and customers with a straightforward centralized system to manage their bookings and appointment information, rather than offering a wide range of business management features. By providing essential booking features for free, BookEasy aims to make scheduling more accessible.

## Proposed Solution and Project Scope
BookEasy will offer a web-based reservation and management system, with each registered salon having a dedicated homepage. Customers will register or log in, visit a salon’s page, browse its services and prices, and select an available appointment time. After a successful booking, the system will display confirmation and save the appointment details. Customers will also be able to view, cancel, or reschedule their appointments through their accounts. The initial version will use a fixed one-hour appointment system. No matter which service is chosen, a one-hour slot will be reserved for each reservation. Using a consistent appointment length will simplify scheduling and availability management. Before confirming a new or rescheduled appointment, the system will check for conflicting bookings to prevent double booking.
 
Salon owners will use a management dashboard to configure their salon information, services, and prices, and specify the days and times available for appointments. These settings will determine the services and available time slots shown on their salon’s page. Owners will also be able to review bookings and manage cancellations and scheduling changes. Each owner will have access only to their own salon’s settings and appointment records.
 
The project scope excludes managing multiple salon branches under a single business account, variable appointment durations, individual staff scheduling and selection, and online payment processing.

## Main Features and Development Epics (Epics 1-5)
### Epic 1: User Authentication and Account Management (Preethi)
This epic will include user registration, login, logout, account management, and role-based access which will differ for customers versus salon owners. 

### Epic 2: Salon Management, Services, and Availability (Edwin)
This epic will allow salon owners to register and manage their salons through a Salon Management Dashboard. Salon owners will be able to create a profile for their salon, add the services they offer, set service prices, and set their salon’s available days and hours for appointments. 

### Epic 3: Appointment Booking and Management (Maria)
This epic will allow customers to create, view, cancel, and reschedule their appointments based on available services and time slots. This epic will take care of double booking prevention by checking existing appointments and the salon’s availability internally. This epic will also build a My Bookings page for customers to view their appointments, as well as for salon owners to view and manage appointments on their end.

### Epic 4: Google Calendar Integration and Notifications (Vaibhav)
This epic will integrate the platform with the Google Calendar API, which will allow customers and salon owners to synchronize appointments with their personal calendars. Calendar events will be created when customers book, updated when customers reschedule, and removed when customers cancel their appointments. The system will also send booking confirmations, cancellation and rescheduling notifications, and appointment reminders 48 hours before scheduled appointments, either via email or SMS depending on what the customer chooses. The integration will use HTTP/HTTPS requests to the Google Calendar REST API, with Google OAuth authorization to access users' calendars.

### Epic 5: Salon Analytics and Business Reporting (Draco)
This epic will provide salon owners with an analytics dashboard to monitor appointment activity and better understand their business performance. Salon owners will be able to view statistics such as total bookings, popular hair services, cancellation rates, and booking trends over time. The dashboard will also display estimated service revenue based on appointment records and service prices. Owners will be able to filter statistics by date range, compare activity across different periods, and generate downloadable reports. These features will help salon owners identify busy periods, understand customer demand, and make more informed decisions about their services and availability.

## Technical Implementation
BookEasy will be developed using Java, Springboot, HTML, CSS, and JavaScript, with PostgreSQL as its database. Springboot will handle user authentication, appointment management, and availability checking, while PostgreSQL will store user accounts, salon information, services, and bookings. The application will also use the Google Calendar REST API to synchronize appointments with users’ calendars through HTTP/HTTPS requests and OAuth authorization.

## Group Meetings and Role Assignments
During our first group meeting on October 5, we introduced ourselves, discussed our individual strengths and weaknesses, and brainstormed potential project ideas. During our second meeting on October 15, we finalized our project idea after receiving feedback from the instructor, divided the project into five epics, and assigned responsibilities to each group member.


## Run the integrated owner branch

This branch integrates Edwin's salon management with Preethi's authentication.
Use Java 17+ and run `./mvnw spring-boot:run` (`.\mvnw.cmd spring-boot:run` on Windows).
Open http://localhost:8080, register as a Salon Owner, then sign in and create a salon.
The default demo profile uses H2 accounts and an in-memory salon store; restarting clears data.

Implemented: salon profile create/edit, service add/list with pagination, initial weekly
hours, owner isolation, guest-readable salon JSON and a demo preview. Appointment booking,
durable salon storage and analytics remain separate work. Preethi's shared login/register
pages are retained; the customer-booking branch is not merged here.

Run `./mvnw package` for tests/build. See [INTEGRATION.md](INTEGRATION.md) for endpoints,
Maria's data handoff, browser-test commands, remaining assumptions and deployment limits.
Owner UI and behavior contracts are in DESIGN.md, UX-CONTRACT.md and USER-STORIES.md.
