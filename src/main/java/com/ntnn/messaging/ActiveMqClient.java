package com.ntnn.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntnn.stdf.StdfRecord;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMqClient implements AutoCloseable {
    private final Connection connection;
    private final ObjectMapper objectMapper;

    public ActiveMqClient(String brokerUrl, String username, String password) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);
        this.connection = factory.createConnection();
        this.connection.start();
        this.objectMapper = new ObjectMapper();
    }

    public void publishRecord(String queueName, StdfRecord record) throws Exception {
        // Create session and producer inside the thread context (JMS Sessions are not thread-safe)
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Destination destination = session.createQueue(queueName);
            try (MessageProducer producer = session.createProducer(destination)) {
                String json = objectMapper.writeValueAsString(record);
                TextMessage message = session.createTextMessage(json);
                producer.send(message);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
