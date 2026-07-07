package com.ntnn;

import com.ntnn.database.DatabaseClient;
import com.ntnn.messaging.ActiveMqClient;
import com.ntnn.pipeline.PipelineDispatcher;
import com.ntnn.testutil.StdfTestWriter;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.Connection;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StepDefinitions {
    private final String jdbcUrl = "jdbc:oracle:thin:@localhost:1521/FREEPDB1";
    private final String fallbackJdbcUrl = "jdbc:oracle:thin:@localhost:1521/FREE";
    private final String dbUser = "SYSTEM";
    private final String dbPass = "password123";

    private final String brokerUrl = "tcp://localhost:61616";
    private final String mqUser = "admin";
    private final String mqPass = "admin";

    private String actualJdbcUrl;
    private DatabaseClient dbClient;
    private ActiveMqClient activeMqClient;
    private PipelineDispatcher dispatcher;
    private File testStdfFile;

    @Given("the OracleDB and ActiveMQ services are running")
    public void verifyingServicesAreRunning() throws Exception {
        System.out.println("Checking and waiting for ActiveMQ and OracleDB...");

        // Wait for ActiveMQ
        boolean mqReady = false;
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setUserName(mqUser);
        factory.setPassword(mqPass);
        
        for (int i = 0; i < 90; i++) {
            try (Connection conn = factory.createConnection()) {
                mqReady = true;
                break;
            } catch (Exception e) {
                System.out.println("Waiting for ActiveMQ (" + i + "/90)...");
                Thread.sleep(2000);
            }
        }
        Assertions.assertTrue(mqReady, "ActiveMQ is not ready");

        // Wait for OracleDB
        boolean dbReady = false;
        String chosenUrl = jdbcUrl;
        for (int i = 0; i < 90; i++) {
            try (java.sql.Connection conn = DriverManager.getConnection(chosenUrl, dbUser, dbPass)) {
                dbReady = true;
                actualJdbcUrl = chosenUrl;
                break;
            } catch (Exception e) {
                // Try fallback URL if the first fails
                try (java.sql.Connection conn = DriverManager.getConnection(fallbackJdbcUrl, dbUser, dbPass)) {
                    dbReady = true;
                    actualJdbcUrl = fallbackJdbcUrl;
                    break;
                } catch (Exception e2) {
                    System.out.println("Waiting for OracleDB (" + i + "/90): " + e.getMessage());
                    Thread.sleep(2000);
                }
            }
        }
        Assertions.assertTrue(dbReady, "OracleDB is not ready");

        System.out.println("Services are ready. Initializing DB schema...");
        dbClient = new DatabaseClient(actualJdbcUrl, dbUser, dbPass);
        
        // Clean up schema for a fresh test run
        try (java.sql.Connection conn = dbClient.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("DROP TABLE TEST_RESULTS");
            } catch (Exception ignored) {}
            try {
                stmt.execute("DROP TABLE STDF_LOTS");
            } catch (Exception ignored) {}
        }
        
        dbClient.initializeSchema();
        activeMqClient = new ActiveMqClient(brokerUrl, mqUser, mqPass);

        // Drain queues before starting the test
        drainQueue("stdf.test.results");
        drainQueue("stdf.lot.meta");
        drainQueue("stdf.lot.summary");
    }

    @Given("a mock STDF binary file {string} exists with lot {string} and {int} parametric tests")
    public void generateMockStdfFile(String filename, String lotId, int numTests) throws Exception {
        testStdfFile = new File(System.getProperty("java.io.tmpdir"), filename);
        System.out.println("Generating mock STDF binary at: " + testStdfFile.getAbsolutePath());
        
        try (StdfTestWriter writer = new StdfTestWriter(testStdfFile.getAbsolutePath())) {
            // Write FAR (CPU 2 = VAX, version 4)
            writer.writeFar(2, 4);
            // Write MIR
            writer.writeMir(lotId, "DEMO_DEVICE", "JOB_Ingestion", "ANTIGRAVITY", 1780000000L, 1780000000L);
            // Write PTRs
            for (int i = 1; i <= numTests; i++) {
                writer.writePtr(i, 1, 1, 1.23f + i, "PARAM_TEST_" + i, 0.0f, 1000.0f);
            }
            // Write MRR
            writer.writeMrr(1780000000L + 3600, "P", "Passed without issues", "");
        }
    }

    @When("the STDF pipeline ingests {string}")
    public void ingestStdfFile(String filename) throws Exception {
        dispatcher = new PipelineDispatcher(dbClient, activeMqClient, 10);
        dispatcher.ingest(testStdfFile.getAbsolutePath());
    }

    @Then("{int} lot record for {string} should be present in OracleDB")
    public void verifyLotRecordInDb(int expectedCount, String lotId) throws Exception {
        try (java.sql.Connection conn = dbClient.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM STDF_LOTS WHERE LOT_ID = '" + lotId + "'")) {
            rs.next();
            int actualCount = rs.getInt(1);
            Assertions.assertEquals(expectedCount, actualCount);
        }
    }

    @Then("{int} test results for lot {string} should be present in OracleDB")
    public void verifyTestResultsInDb(int expectedCount, String lotId) throws Exception {
        try (java.sql.Connection conn = dbClient.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM TEST_RESULTS WHERE LOT_ID = '" + lotId + "'")) {
            rs.next();
            int actualCount = rs.getInt(1);
            Assertions.assertEquals(expectedCount, actualCount);
        }
    }

    @Then("{int} JSON message(s) should be published to ActiveMQ queue {string}")
    public void verifyActiveMqMessageCount(int expectedCount, String queueName) throws Exception {
        List<String> messages = fetchMessagesFromQueue(queueName);
        Assertions.assertEquals(expectedCount, messages.size(), "Message count mismatch for queue: " + queueName);
        System.out.println("Verified queue " + queueName + " has " + messages.size() + " messages.");
    }

    private void drainQueue(String queueName) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setUserName(mqUser);
        factory.setPassword(mqPass);
        try (Connection conn = factory.createConnection()) {
            conn.start();
            try (Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                jakarta.jms.Destination dest = session.createQueue(queueName);
                try (MessageConsumer consumer = session.createConsumer(dest)) {
                    while (consumer.receive(200) != null) {
                        // Keep reading
                    }
                }
            }
        }
    }

    private List<String> fetchMessagesFromQueue(String queueName) throws Exception {
        List<String> messages = new ArrayList<>();
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setUserName(mqUser);
        factory.setPassword(mqPass);
        try (Connection conn = factory.createConnection()) {
            conn.start();
            try (Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                jakarta.jms.Destination dest = session.createQueue(queueName);
                try (MessageConsumer consumer = session.createConsumer(dest)) {
                    while (true) {
                        Message msg = consumer.receive(1000);
                        if (msg == null) {
                            break;
                        }
                        if (msg instanceof jakarta.jms.TextMessage textMessage) {
                            messages.add(textMessage.getText());
                        }
                    }
                }
            }
        }
        return messages;
    }
}
