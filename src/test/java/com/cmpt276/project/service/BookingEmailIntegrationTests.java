package com.cmpt276.project.service;

import com.cmpt276.project.model.*;
import com.cmpt276.project.owner.model.OpeningDay;
import com.cmpt276.project.owner.model.Salon;
import com.cmpt276.project.owner.model.SalonService;
import com.cmpt276.project.owner.store.SalonStore;
import com.cmpt276.project.repository.AccountRepository;
import com.cmpt276.project.repository.AppointmentRepository;
import com.sun.net.httpserver.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-email-integration;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false"
})
@AutoConfigureMockMvc
@Import(BookingEmailIntegrationTests.EmailTestConfiguration.class)
class BookingEmailIntegrationTests {
    private static final String DATABASE_URL = "jdbc:h2:mem:booking-email-integration;DB_CLOSE_DELAY=-1";
    private static final String CUSTOMER_EMAIL = "alex@example.invalid";
    private static final String DATE = "2026-11-03";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SalonStore salonStore;

    @Autowired
    private FakeEmailServer emailServer;

    @Autowired
    private TestEmailSender emailSender;

    @Autowired
    private CommitFailureTransactionManager transactionManager;

    private Salon salon;

    @BeforeEach
    void prepareBooking() {
        transactionManager.failNextCommit = false;
        transactionManager.commitFailureTriggered = false;
        appointmentRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(new Account("Alex Lee", CUSTOMER_EMAIL, "fake-password-hash", RoleType.CUSTOMER));

        salon = new Salon();
        salon.setId("email-test-salon");
        salon.setOwnerId("email-test-owner");
        salon.setSlug("email-test-salon");
        salon.setName("CS_Salon");
        salon.setAddress("123 Main Street");
        SalonService service = new SalonService();
        service.setId("email-test-service");
        service.setName("Haircut");
        service.setPrice(new BigDecimal("25.00"));
        salon.getServices().add(service);
        OpeningDay hours = new OpeningDay();
        hours.setDayOfWeek(LocalDate.parse(DATE).getDayOfWeek().getValue());
        hours.setOpen(true);
        hours.setStartTime("09:00");
        hours.setEndTime("17:00");
        salon.getOpeningHours().add(hours);
        salonStore.save(salon);

        emailServer.requestCount = 0;
        emailServer.committedAppointments = 0;
        emailServer.status = 201;
        emailServer.delayBody = false;
        emailServer.releaseBody = new CountDownLatch(1);
        emailServer.bodyFinished = new CountDownLatch(1);
        emailSender.settings.ready = true;
        emailSender.attempts = 0;
        emailSender.transactionActive = false;
        emailSender.failSending = false;
    }

    @AfterEach
    void finishDelayedResponse() throws InterruptedException {
        emailServer.releaseBody.countDown();
        if (emailServer.delayBody) {
            assertTrue(emailServer.bodyFinished.await(2, TimeUnit.SECONDS),
                    "Finish the delayed response before the next test uses the server");
        }
    }

    @Test
    void sendsAfterCommitAndDoesNotResendForViewsOrARejectedRepeat() throws Exception {
        MvcResult result = book("09:00");
        assertConfirmation(result, BookingEmailResult.ACCEPTED, "submitted for delivery");
        Appointment saved = appointmentRepository.findAll().get(0);
        assertEquals(1, emailServer.requestCount);
        assertEquals(1, emailServer.committedAppointments,
                "An independent database connection must see the booking before the email request");
        assertFalse(emailSender.transactionActive, "The email request must run outside the booking transaction");
        JsonNode request = new JsonMapper().readTree(emailServer.requestBody);
        assertEquals(saved.getCustomer().getEmail(), request.get("to").get(0).get("email").asString());
        String body = request.get("textContent").asString();
        assertTrue(body.contains("Booking reference: " + saved.getId() + "\n"));
        assertTrue(body.contains("Salon: CS_Salon\nAddress: 123 Main Street\n"));
        assertTrue(body.contains("Service: " + saved.getServiceName() + "\nDate: "
                + saved.getAppointmentDate() + "\nTime: " + saved.getAppointmentTime() + " (Vancouver time)"));

        mvc.perform(get("/customer/appointments").with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk());
        mvc.perform(get("/customer/appointments/details").param("id", String.valueOf(saved.getId()))
                .with(user(CUSTOMER_EMAIL).roles("CUSTOMER"))).andExpect(status().isOk());
        mvc.perform(get("/customer/booking/confirmation")
                .param("service", "email-test-service").param("salon", salon.getSlug())
                .param("appointmentDate", DATE).param("appointmentTime", "09:00")
                .with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/booking-summary"));
        assertEquals("customer/select-time", book("09:00").getModelAndView().getViewName());
        assertEquals(1, appointmentRepository.count());
        assertEquals(1, emailSender.attempts);
        assertEquals(1, emailServer.requestCount);
    }

    @Test
    void sendsNothingForUnavailableSlotsFailedSavesOrFailedCommits() throws Exception {
        assertEquals("customer/select-time", book("08:00").getModelAndView().getViewName());
        assertEquals(0, appointmentRepository.count());

        // H2 rejects this value during the real repository insert.
        salon.getServices().get(0).setName("x".repeat(300));
        assertBookingFails("09:00");
        assertEquals(0, appointmentRepository.count());
        salon.getServices().get(0).setName("Haircut");

        // Fail in the transaction manager's commit path, after save has returned.
        transactionManager.failNextCommit = true;
        assertBookingFails("10:00");
        assertTrue(transactionManager.commitFailureTriggered);
        assertEquals(0, appointmentRepository.count());
        assertEquals(0, emailSender.attempts);
        assertEquals(0, emailServer.requestCount);
    }

    @Test
    void keepsBookingsAndShowsRejectionUnavailableAndTimeoutFeedback() throws Exception {
        emailServer.status = 401;
        assertConfirmation(book("09:00"), BookingEmailResult.REJECTED, "provider rejected");
        assertEquals(1, appointmentRepository.count());

        emailSender.settings.ready = false;
        assertConfirmation(book("10:00"), BookingEmailResult.UNAVAILABLE, "email is unavailable");
        assertEquals(2, appointmentRepository.count());
        assertEquals(1, emailServer.requestCount);

        emailSender.settings.ready = true;
        emailServer.status = 201;
        emailServer.delayBody = true;
        assertConfirmation(book("11:00"), BookingEmailResult.UNCERTAIN, "email may still arrive");
        assertEquals(3, appointmentRepository.count());
        assertEquals(3, emailServer.committedAppointments);
        assertEquals(2, emailServer.requestCount);
    }

    @Test
    void keepsBookingsWhenMessageDetailsOrSendingHaveAnUnexpectedProblem() throws Exception {
        accountRepository.deleteAll();
        accountRepository.save(new Account("", CUSTOMER_EMAIL, "fake-password-hash", RoleType.CUSTOMER));
        assertConfirmation(book("09:00"), BookingEmailResult.UNAVAILABLE, "email is unavailable");
        assertEquals(1, appointmentRepository.count());
        assertEquals(0, emailSender.attempts);
        assertEquals(0, emailServer.requestCount);

        Account secondCustomer = accountRepository.save(new Account("Sam Chen", "sam@example.invalid",
                "fake-password-hash", RoleType.CUSTOMER));
        emailSender.failSending = true;
        assertConfirmation(book("10:00", secondCustomer.getEmail()), BookingEmailResult.UNCERTAIN, "email may still arrive");
        assertEquals(2, appointmentRepository.count());
        assertEquals(1, emailSender.attempts);
        assertEquals(0, emailServer.requestCount);
    }

    private MvcResult book(String time) throws Exception {
        return book(time, CUSTOMER_EMAIL);
    }

    private MvcResult book(String time, String customerEmail) throws Exception {
        return mvc.perform(post("/customer/booking/confirm")
                .param("service", "email-test-service").param("salon", salon.getSlug())
                .param("appointmentDate", DATE).param("appointmentTime", time)
                .with(user(customerEmail).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isOk()).andReturn();
    }

    private void assertConfirmation(MvcResult result, BookingEmailResult expected, String feedback) throws Exception {
        assertEquals("customer/booking-confirmation", result.getModelAndView().getViewName());
        assertEquals(expected, result.getModelAndView().getModel().get("emailResult"));
        String page = result.getResponse().getContentAsString();
        assertTrue(page.contains("Booking Confirmed!"));
        assertTrue(page.contains(feedback));
        assertTrue(page.contains("href=\"/customer/appointments\""));
        assertFalse(page.contains("fake-test-key"));
    }

    private void assertBookingFails(String time) throws Exception {
        boolean failed = false;
        try {
            book(time);
        } catch (ServletException exception) {
            failed = true;
        }
        assertTrue(failed, "A persistence failure must leave the successful-booking path");
    }

    @TestConfiguration
    static class EmailTestConfiguration {
        @Bean(destroyMethod = "stop")
        FakeEmailServer fakeEmailServer() throws IOException {
            return new FakeEmailServer();
        }

        @Bean
        @Primary
        TestEmailSender testEmailSender(FakeEmailServer server) {
            return new TestEmailSender(new TestEmailSettings(), server.endpoint);
        }

        @Bean
        CommitFailureTransactionManager transactionManager(EntityManagerFactory factory) {
            return new CommitFailureTransactionManager(factory);
        }
    }

    private static class TestEmailSettings extends BookingEmailSettings {
        private boolean ready = true;

        TestEmailSettings() {
            super(true, "fake-test-key", "sender@example.invalid", "BookEasy");
        }

        @Override
        public boolean isReady() {
            return ready;
        }
    }

    private static class TestEmailSender extends BrevoEmailSender {
        private final TestEmailSettings settings;
        private int attempts;
        private boolean transactionActive;
        private boolean failSending;

        TestEmailSender(TestEmailSettings settings, URI endpoint) {
            super(settings, endpoint, Duration.ofMillis(500));
            this.settings = settings;
        }

        @Override
        public BookingEmailResult send(BookingConfirmationMessage message) {
            attempts++;
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            if (failSending) {
                throw new IllegalStateException("Test sending failure");
            }
            return super.send(message);
        }
    }

    private static class CommitFailureTransactionManager extends JpaTransactionManager {
        private boolean failNextCommit;
        private boolean commitFailureTriggered;

        CommitFailureTransactionManager(EntityManagerFactory factory) {
            super(factory);
            setRollbackOnCommitFailure(true);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            if (failNextCommit) {
                failNextCommit = false;
                commitFailureTriggered = true;
                throw new TransactionSystemException("Test commit-stage failure");
            }
            super.doCommit(status);
        }
    }

    private static class FakeEmailServer implements HttpHandler {
        private final HttpServer server;
        private final URI endpoint;
        private volatile int status = 201;
        private volatile boolean delayBody;
        private volatile int requestCount;
        private volatile int committedAppointments;
        private String requestBody;
        private CountDownLatch releaseBody = new CountDownLatch(1);
        private CountDownLatch bodyFinished = new CountDownLatch(1);

        FakeEmailServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v3/smtp/email", this);
            server.start();
            endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "");
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("select count(*) from appointment")) {
                result.next();
                committedAppointments = result.getInt(1);
            } catch (SQLException exception) {
                throw new IOException("Could not check committed test data", exception);
            }
            byte[] body = "{\"messageId\":\"fake-message-id\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try {
                if (delayBody) {
                    exchange.getResponseBody().write(body, 0, 1);
                    exchange.getResponseBody().flush();
                    releaseBody.await(3, TimeUnit.SECONDS);
                    exchange.getResponseBody().write(body, 1, body.length - 1);
                } else {
                    exchange.getResponseBody().write(body);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
                if (delayBody) {
                    bodyFinished.countDown();
                }
            }
        }

        void stop() {
            server.stop(0);
        }
    }
}
