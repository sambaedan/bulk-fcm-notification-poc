package com.fcm.fcm_demo.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.fcm_demo.fcm.FcmRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public void publish(FcmRequest request) {
        try {
            String payload = mapper.writeValueAsString(request);
            kafkaTemplate.send("fcm-bulk-topic", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize FCM request", e);
        }
    }
}