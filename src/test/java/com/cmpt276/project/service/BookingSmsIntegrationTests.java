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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-sms-integration;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "bookeasy.email.enabled=false",
        "bookeasy.sms.enabled=false"
})
@AutoConfigureMockMvc
@Import(BookingSmsIntegrationTests.SmsTestConfiguration.class)
class BookingSmsIntegrationTests {
    private static final String DATABASE_URL = "jdbc:h2:mem:booking-sms-integration;DB_CLOSE_DELAY=-1";
    private static final String CUSTOMER_EMAIL = "alex@example.invalid";
    private static final String DATE = LocalDate.now().plusDays(2).toString();
    private static final String ACCOUNT_SID = "AC11111111111111111111111111111111";
    private static final String API_KEY_SID = "SK22222222222222222222222222222222";
    private static final String API_KEY_SECRET = "fake-test-secret";

    private static final String TEXT_ACCEPTED = "Confirmation text submitted for delivery.";
    private static final String TEXT_REJECTED = "The text message service could not send the confirmation text.";
    private static final String TEXT_UNCERTAIN = "Text sending could not be confirmed. The text may still arrive.";
    private static final String TEXT_UNAVAILABLE = "Confirmation text is unavailable.";
    private static final String STILL_BOOKED = "Your appointment is still booked.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SalonStore salonStore;

    @Autowired
    private FakeProviderServer providers;

    @Autowired
    private TestEmailSender emailSender;

    @Autowired
    private TestSmsSender smsSender;

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
        salon.setId("sms-test-salon");
        salon.setOwnerId("sms-test-owner");
        salon.setSlug("sms-test-salon");
        salon.setName("CS_Salon");
        salon.setAddress("123 Main Street");
        SalonService service = new SalonService();
        service.setId("sms-test-service");
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

        providers.email.reset("{\"messageId\":\"fake-message-id\"}");
        providers.sms.reset("{\"sid\":\"SM33333333333333333333333333333333\",\"status\":\"queued\"}");
        emailSender.attempts = 0;
        emailSender.failSending = false;
        smsSender.settings.ready = true;
        smsSender.attempts = 0;
        smsSender.transactionActive = true;
        smsSender.failSending = false;
    }

    @AfterEach
    void finishDelayedResponse() throws InterruptedException {
        providers.sms.releaseBody.countDown();
        if (providers.sms.delayBody) {
            assertTrue(providers.sms.bodyFinished.await(2, TimeUnit.SECONDS),
                    "Finish the delayed response before the next test uses the server");
        }
    }

    @Test
    void emailPreferenceSendsOnlyTheEmailIncludingLegacyAccounts() throws Exception {
        useCustomer(NotificationPreference.EMAIL, "604-555-1234");
        MvcResult result = book("09:00");
        assertEmailOnlyConfirmation(result);
        assertEquals(1, emailSender.attempts);
        assertEquals(1, providers.email.requestCount);

        // An account saved before the preference existed has no stored choice.
        useCustomer(null, "604-555-1234");
        assertEquals(1, countAccountsWithoutPreference());
        assertEmailOnlyConfirmation(book("10:00"));
        assertEquals(2, emailSender.attempts);
        assertEquals(2, providers.email.requestCount);
        assertEquals(0, smsSender.attempts);
        assertEquals(0, providers.sms.requestCount);
        assertEquals(2, appointmentRepository.count());
    }

    @Test
    void smsPreferenceSendsOnlyTheTextAfterTheBookingCommits() throws Exception {
        useCustomer(NotificationPreference.SMS, "(604) 555-1234");
        MvcResult result = book("09:00");

        assertConfirmation(result);
        assertEquals(BookingSmsResult.ACCEPTED, model(result, "smsResult"));
        assertNull(model(result, "emailResult"));
        String page = page(result);
        assertTrue(page.contains(TEXT_ACCEPTED));
        assertFalse(page.contains("Confirmation email"));

        assertEquals(1, smsSender.attempts);
        assertEquals(1, providers.sms.requestCount);
        assertEquals(1, providers.sms.committedAppointments,
                "An independent database connection must see the booking before the text request");
        assertFalse(smsSender.transactionActive, "The text request must run outside the booking transaction");
        assertEquals("/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json", providers.sms.requestPath);
        Map<String, String> form = readForm(providers.sms.requestBody);
        assertEquals("+16045551234", form.get("To"));
        assertEquals("+16045550100", form.get("From"));
        assertEquals("BookEasy: Your Haircut at CS_Salon is confirmed for " + DATE
                + " at 09:00 (Vancouver time). You can view it in My Appointments.", form.get("Body"));

        assertEquals(0, emailSender.attempts);
        assertEquals(0, providers.email.requestCount);
        assertEquals(1, appointmentRepository.count());
    }

    @Test
    void bothPreferenceSendsTheEmailAndTheTextOnce() throws Exception {
        useCustomer(NotificationPreference.BOTH, "6045551234");
        MvcResult result = book("09:00");

        assertConfirmation(result);
        assertEquals(BookingEmailResult.ACCEPTED, model(result, "emailResult"));
        assertEquals(BookingSmsResult.ACCEPTED, model(result, "smsResult"));
        String page = page(result);
        assertTrue(page.contains("Confirmation email submitted"));
        assertTrue(page.contains(TEXT_ACCEPTED));
        assertEquals(1, emailSender.attempts);
        assertEquals(1, providers.email.requestCount);
        assertEquals(1, smsSender.attempts);
        assertEquals(1, providers.sms.requestCount);
    }

    @Test
    void bothPreferenceStillSendsTheTextWhenTheEmailFails() throws Exception {
        useCustomer(NotificationPreference.BOTH, "6045551234");

        providers.email.status = 401;
        MvcResult rejected = book("09:00");
        assertEquals(BookingEmailResult.REJECTED, model(rejected, "emailResult"));
        assertEquals(BookingSmsResult.ACCEPTED, model(rejected, "smsResult"));
        assertTrue(page(rejected).contains(TEXT_ACCEPTED));

        emailSender.failSending = true;
        MvcResult failed = book("10:00");
        assertEquals(BookingEmailResult.UNCERTAIN, model(failed, "emailResult"));
        assertEquals(BookingSmsResult.ACCEPTED, model(failed, "smsResult"));
        assertTrue(page(failed).contains(TEXT_ACCEPTED));

        assertEquals(2, emailSender.attempts);
        assertEquals(2, smsSender.attempts);
        assertEquals(2, providers.sms.requestCount);
        assertEquals(2, appointmentRepository.count());
    }

    @Test
    void bothPreferenceStillSendsTheEmailWhenTheTextFails() throws Exception {
        useCustomer(NotificationPreference.BOTH, "6045551234");

        providers.sms.status = 400;
        MvcResult rejected = book("09:00");
        assertEquals(BookingSmsResult.REJECTED, model(rejected, "smsResult"));
        assertEquals(BookingEmailResult.ACCEPTED, model(rejected, "emailResult"));
        assertTrue(page(rejected).contains("Confirmation email submitted"));

        smsSender.failSending = true;
        MvcResult failed = book("10:00");
        assertEquals(BookingSmsResult.UNCERTAIN, model(failed, "smsResult"));
        assertEquals(BookingEmailResult.ACCEPTED, model(failed, "emailResult"));
        assertTrue(page(failed).contains("Confirmation email submitted"));

        assertEquals(2, smsSender.attempts);
        assertEquals(2, emailSender.attempts);
        assertEquals(2, providers.email.requestCount);
        assertEquals(2, appointmentRepository.count());
    }

    @Test
    void showsTextFeedbackAndKeepsTheBookingWhenTheTextIsNotSent() throws Exception {
        useCustomer(NotificationPreference.SMS, "6045551234");

        // Twilio also answers 429 when it is busy, which lands here as well.
        providers.sms.status = 400;
        assertTextFeedback(book("09:00"), BookingSmsResult.REJECTED, TEXT_REJECTED);
        assertEquals(1, appointmentRepository.count());

        providers.sms.status = 500;
        assertTextFeedback(book("10:00"), BookingSmsResult.UNCERTAIN, TEXT_UNCERTAIN);
        assertEquals(2, appointmentRepository.count());

        smsSender.settings.ready = false;
        assertTextFeedback(book("11:00"), BookingSmsResult.UNAVAILABLE, TEXT_UNAVAILABLE);
        assertEquals(3, appointmentRepository.count());
        assertEquals(2, providers.sms.requestCount);

        // A phone that cannot be normalized is refused before the sender is called.
        smsSender.settings.ready = true;
        useCustomer(NotificationPreference.SMS, null);
        assertTextFeedback(book("12:00"), BookingSmsResult.UNAVAILABLE, TEXT_UNAVAILABLE);
        assertEquals(4, appointmentRepository.count());
        assertEquals(3, smsSender.attempts);
        assertEquals(2, providers.sms.requestCount);
        assertEquals(0, emailSender.attempts);
    }

    @Test
    void sendsNothingForARejectedBooking() throws Exception {
        useCustomer(NotificationPreference.BOTH, "6045551234");

        // The salon opens at 09:00, so 08:00 is never offered.
        assertEquals("customer/select-time", book("08:00").getModelAndView().getViewName());
        assertEquals(0, appointmentRepository.count());
        assertEquals(0, smsSender.attempts);
        assertEquals(0, emailSender.attempts);

        assertConfirmation(book("09:00"));
        assertEquals("customer/select-time", book("09:00").getModelAndView().getViewName());
        assertEquals(1, appointmentRepository.count());
        assertEquals(1, smsSender.attempts);
        assertEquals(1, providers.sms.requestCount);
        assertEquals(1, emailSender.attempts);
    }

    @Test
    void sendsNoTextForFailedSavesOrFailedCommits() throws Exception {
        useCustomer(NotificationPreference.BOTH, "6045551234");

        // H2 rejects this value during the real repository insert.
        salon.getServices().get(0).setName("x".repeat(300));
        assertBookingFails("09:00");
        assertEquals(0, appointmentRepository.count());
        salon.getServices().get(0).setName("Haircut");

        // Fail in the transaction manager's commit path, after save has returned.
        useCustomer(NotificationPreference.SMS, "6045551234");
        transactionManager.failNextCommit = true;
        assertBookingFails("10:00");
        assertTrue(transactionManager.commitFailureTriggered);
        assertEquals(0, appointmentRepository.count());

        assertEquals(0, smsSender.attempts);
        assertEquals(0, providers.sms.requestCount);
        assertEquals(0, emailSender.attempts);
        assertEquals(0, providers.email.requestCount);
    }

    @Test
    void viewingPagesAfterABookingDoesNotSendAnotherText() throws Exception {
        useCustomer(NotificationPreference.SMS, "6045551234");
        assertConfirmation(book("09:00"));
        Appointment saved = appointmentRepository.findAll().get(0);
        assertEquals(1, providers.sms.requestCount);

        mvc.perform(get("/customer/booking").param("service", "sms-test-service").param("salon", salon.getSlug())
                .with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/select-date"));
        mvc.perform(get("/customer/booking/time").param("service", "sms-test-service").param("salon", salon.getSlug())
                .param("appointmentDate", DATE).with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/select-time"));
        mvc.perform(get("/customer/booking/confirmation")
                .param("service", "sms-test-service").param("salon", salon.getSlug())
                .param("appointmentDate", DATE).param("appointmentTime", "09:00")
                .with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/booking-summary"));
        mvc.perform(get("/customer/appointments").with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/my-appointments"));
        mvc.perform(get("/customer/appointments/details").param("id", String.valueOf(saved.getId()))
                .with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(view().name("customer/appointment-details"));

        assertEquals(1, appointmentRepository.count());
        assertEquals(1, smsSender.attempts);
        assertEquals(1, providers.sms.requestCount);
        assertEquals(0, emailSender.attempts);
    }

    @Test
    void showsUncertainFeedbackAndKeepsTheBookingWhenTwilioTimesOut() throws Exception {
        useCustomer(NotificationPreference.SMS, "6045551234");

        // The fake server holds the reply longer than the sender's 500 ms limit.
        providers.sms.delayBody = true;
        assertTextFeedback(book("09:00"), BookingSmsResult.UNCERTAIN, TEXT_UNCERTAIN);
        assertEquals(1, appointmentRepository.count());
        assertEquals(1, providers.sms.committedAppointments);
        assertEquals(1, smsSender.attempts);
        assertEquals(1, providers.sms.requestCount);
    }

    @Test
    void sendsTheTextToTheBookingCustomersOwnPhone() throws Exception {
        useCustomer(NotificationPreference.SMS, "(604) 555-1234");
        Account sam = new Account("Sam Chen", "sam@example.invalid", "fake-password-hash", RoleType.CUSTOMER);
        sam.setNotificationPreference(NotificationPreference.SMS);
        sam.setPhoneNumber("778-555-0199");
        accountRepository.save(sam);

        assertConfirmation(book("09:00"));
        Map<String, String> first = readForm(providers.sms.requestBody);
        assertEquals("+16045551234", first.get("To"));
        assertTrue(first.get("Body").contains(" at 09:00 "));

        assertConfirmation(book("10:00", sam.getEmail()));
        Map<String, String> second = readForm(providers.sms.requestBody);
        assertEquals("+17785550199", second.get("To"));
        assertTrue(second.get("Body").contains(" at 10:00 "));
        assertFalse(second.get("Body").contains("09:00"));

        assertEquals(2, providers.sms.requestCount);
        assertEquals(0, emailSender.attempts);
    }

    private void useCustomer(NotificationPreference preference, String phoneNumber) {
        Account customer = accountRepository.findByEmail(CUSTOMER_EMAIL).orElseThrow();
        customer.setNotificationPreference(preference);
        customer.setPhoneNumber(phoneNumber);
        accountRepository.save(customer);
    }

    private int countAccountsWithoutPreference() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "select count(*) from account where notification_preference is null")) {
            result.next();
            return result.getInt(1);
        }
    }

    private MvcResult book(String time) throws Exception {
        return book(time, CUSTOMER_EMAIL);
    }

    private MvcResult book(String time, String customerEmail) throws Exception {
        return mvc.perform(post("/customer/booking/confirm")
                .param("service", "sms-test-service").param("salon", salon.getSlug())
                .param("appointmentDate", DATE).param("appointmentTime", time)
                .with(user(customerEmail).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isOk()).andReturn();
    }

    private void assertBookingFails(String time) throws Exception {
        boolean failed = false;
        try {
            book(time);
        } catch (ServletException exception) {
            failed = true;
        }
        assertTrue(failed, "A persistence failure must leave the successful booking path");
    }

    private void assertConfirmation(MvcResult result) throws Exception {
        assertEquals("customer/booking-confirmation", result.getModelAndView().getViewName());
        String page = page(result);
        assertTrue(page.contains("Booking Confirmed!"));
        assertTrue(page.contains("href=\"/customer/appointments\""));
        // Twilio settings never reach the page, whatever the outcome.
        assertFalse(page.contains(API_KEY_SECRET));
        assertFalse(page.contains(API_KEY_SID));
        assertFalse(page.contains(ACCOUNT_SID));
    }

    private void assertEmailOnlyConfirmation(MvcResult result) throws Exception {
        assertConfirmation(result);
        assertEquals(BookingEmailResult.ACCEPTED, model(result, "emailResult"));
        assertNull(model(result, "smsResult"));
        assertNull(model(result, "smsMessage"));
        String page = page(result);
        assertTrue(page.contains("Confirmation email submitted"));
        assertFalse(page.contains(TEXT_ACCEPTED));
        assertFalse(page.contains(TEXT_UNAVAILABLE));
    }

    private void assertTextFeedback(MvcResult result, BookingSmsResult expected, String feedback) throws Exception {
        assertConfirmation(result);
        assertEquals(expected, model(result, "smsResult"));
        assertNull(model(result, "emailResult"));
        String page = page(result);
        assertTrue(page.contains(feedback + " " + STILL_BOOKED), "Expected feedback: " + feedback);
        assertFalse(page.contains("delivered"));
    }

    private Object model(MvcResult result, String name) {
        return result.getModelAndView().getModel().get(name);
    }

    private String page(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    private Map<String, String> readForm(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            form.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return form;
    }

    @TestConfiguration
    static class SmsTestConfiguration {
        @Bean(destroyMethod = "stop")
        FakeProviderServer fakeProviderServer() throws IOException {
            return new FakeProviderServer();
        }

        @Bean
        @Primary
        TestEmailSender testEmailSender(FakeProviderServer server) {
            return new TestEmailSender(server.baseUrl.resolve("/v3/smtp/email"));
        }

        @Bean
        @Primary
        TestSmsSender testSmsSender(FakeProviderServer server) {
            return new TestSmsSender(new TestSmsSettings(), server.baseUrl);
        }

        @Bean
        CommitFailureTransactionManager transactionManager(EntityManagerFactory factory) {
            return new CommitFailureTransactionManager(factory);
        }
    }

    private static class TestSmsSettings extends BookingSmsSettings {
        private boolean ready = true;

        TestSmsSettings() {
            super(true, ACCOUNT_SID, API_KEY_SID, API_KEY_SECRET, "+16045550100");
        }

        @Override
        public boolean isReady() {
            return ready;
        }
    }

    private static class TestSmsSender extends TwilioSmsSender {
        private final TestSmsSettings settings;
        private int attempts;
        private boolean transactionActive;
        private boolean failSending;

        TestSmsSender(TestSmsSettings settings, URI baseUrl) {
            super(settings, baseUrl, Duration.ofMillis(500));
            this.settings = settings;
        }

        @Override
        public BookingSmsResult send(String toPhone, String text) {
            attempts++;
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            if (failSending) {
                throw new IllegalStateException("Test sending failure");
            }
            return super.send(toPhone, text);
        }
    }

    private static class TestEmailSender extends BrevoEmailSender {
        private int attempts;
        private boolean failSending;

        TestEmailSender(URI endpoint) {
            super(new BookingEmailSettings(true, "fake-test-key", "sender@example.invalid", "BookEasy"),
                    endpoint, Duration.ofMillis(500));
        }

        @Override
        public BookingEmailResult send(BookingConfirmationMessage message) {
            attempts++;
            if (failSending) {
                throw new IllegalStateException("Test sending failure");
            }
            return super.send(message);
        }
    }

    // Same commit failure as the email test, so a save that never commits can be checked too.
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

    // One local server stands in for both Brevo and Twilio, so nothing leaves the machine.
    private static class FakeProviderServer {
        private final HttpServer server;
        private final URI baseUrl;
        private final FakeProvider email = new FakeProvider();
        private final FakeProvider sms = new FakeProvider();

        FakeProviderServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v3/smtp/email", email);
            server.createContext("/2010-04-01/Accounts/", sms);
            server.start();
            baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        }

        void stop() {
            server.stop(0);
        }
    }

    private static class FakeProvider implements HttpHandler {
        private volatile int status;
        private volatile String responseBody;
        private volatile int requestCount;
        private volatile int committedAppointments;
        private volatile String requestPath;
        private volatile String requestBody;
        private volatile boolean delayBody;
        private CountDownLatch releaseBody = new CountDownLatch(1);
        private CountDownLatch bodyFinished = new CountDownLatch(1);

        void reset(String acceptedBody) {
            status = 201;
            responseBody = acceptedBody;
            requestCount = 0;
            committedAppointments = 0;
            requestPath = null;
            requestBody = null;
            delayBody = false;
            releaseBody = new CountDownLatch(1);
            bodyFinished = new CountDownLatch(1);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;
            requestPath = exchange.getRequestURI().getPath();
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "");
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("select count(*) from appointment")) {
                result.next();
                committedAppointments = result.getInt(1);
            } catch (SQLException exception) {
                throw new IOException("Could not check committed test data", exception);
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
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
    }
}
