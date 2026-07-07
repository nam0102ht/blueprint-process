package com.ntnn.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntnn.stdf.StdfRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActiveMqClient {
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ActiveMqClient(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publishRecord(String queueName, StdfRecord record) throws Exception {
        String json = objectMapper.writeValueAsString(record);
        // JmsTemplate is thread-safe and manages session/producer creation under the hood
        jmsTemplate.send(queueName, session -> session.createTextMessage(json));
    }

    public void publishFilePath(String queueName, String filePath) {
        jmsTemplate.send(queueName, session -> session.createTextMessage(filePath));
    }
}
