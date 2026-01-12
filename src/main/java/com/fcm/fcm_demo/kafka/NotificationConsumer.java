package com.fcm.fcm_demo.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.fcm_demo.fcm.FcmRequest;
import com.fcm.fcm_demo.service.NotificationBatchService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);
    private final NotificationBatchService batchService;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "fcm-bulk-topic", containerFactory = "batchFactory")
    public void handleBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                FcmRequest req = mapper.readValue(record.value(), FcmRequest.class);
                batchService.process(req);
            } catch (Exception e) {
                logger.error("Failed to process record offset={} partition={}: {}",
                        record.offset(), record.partition(), e.getMessage(), e);
            }
        }
        ack.acknowledge();
    }
}