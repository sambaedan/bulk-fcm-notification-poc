
package com.fcm.fcm_demo.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class FcmClient {
    private static final Logger log = LoggerFactory.getLogger(FcmClient.class);

    public FcmClient() throws IOException {
        synchronized (FirebaseApp.class) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ClassPathResource("firebase-sdk.json").getInputStream()))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized for FCM Admin SDK");
            }
        }
    }

    /**
     * Send to a single device token using Admin SDK.
     */
    public SingleFcmResponse sendToken(String token, String title, String body, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder mb = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                mb.putAllData(data);
            }

            String messageId = FirebaseMessaging.getInstance().send(mb.build());
            log.info("FCM send success: token={} messageId={}", token, messageId);
            return new SingleFcmResponse(true, 200, messageId, null, null);

        } catch (FirebaseMessagingException e) {
            log.info("FCM send failed: token={} code={} msg={}", token, e.getMessagingErrorCode(), e.getMessage(), e);
            return new SingleFcmResponse(false, 0, null,
                    e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : null,
                    e.getMessage());
        }
    }

    /**
     * Send the same notification to multiple tokens using sendEachForMulticast (up to 500 tokens).
     */
    public BatchFcmResponse sendTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return new BatchFcmResponse(false, 0, 0, List.of());

        try {
            Notification notification = Notification.builder().setTitle(title).setBody(body).build();
            MulticastMessage.Builder builder = MulticastMessage.builder().addAllTokens(tokens).setNotification(notification);

            if (data != null && !data.isEmpty()) builder.putAllData(data);

            MulticastMessage multicast = builder.build();

            long startNs = System.nanoTime();

            BatchResponse batch = FirebaseMessaging.getInstance().sendEachForMulticast(multicast);

            List<PerTokenResult> results = getPerTokenResults(tokens, batch);

            long fcmTimeMs = (System.nanoTime() - startNs) / 1_000_000;

            log.info(
                    "FCM multicast call completed: tokens={} success={} failure={} fcmTimeMs={}",
                    tokens.size(),
                    batch.getSuccessCount(),
                    batch.getFailureCount(),
                    fcmTimeMs
            );
            return new BatchFcmResponse(batch.getFailureCount() == 0, batch.getSuccessCount(), batch.getFailureCount(), results);

        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast exception: code={} msg={}", e.getMessagingErrorCode(), e.getMessage(), e);
            return new BatchFcmResponse(false, 0, tokens.size(), tokens.stream()
                    .map(t -> new PerTokenResult(t, false, null,
                            e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : null,
                            e.getMessage()))
                    .toList());
        }
    }

    private static List<PerTokenResult> getPerTokenResults(List<String> tokens, BatchResponse batch) {
        List<PerTokenResult> results = new ArrayList<>(tokens.size());
        for (int i = 0; i < batch.getResponses().size(); i++) {
            SendResponse sr = batch.getResponses().get(i);
            String token = tokens.get(i);
            if (sr.isSuccessful()) results.add(new PerTokenResult(token, true, sr.getMessageId(), null, null));
            else {
                FirebaseMessagingException ex = sr.getException();
                results.add(new PerTokenResult(
                        token, false, null,
                        ex != null && ex.getMessagingErrorCode() != null ? ex.getMessagingErrorCode().name() : null,
                        ex != null ? ex.getMessage() : "Unknown error"));
            }
        }
        return results;
    }


    // Response types
    public record SingleFcmResponse(boolean success, int statusCode, String messageId, String errorCode,
                                    String errorMessage) {
    }

    public record BatchFcmResponse(boolean allSucceeded, int successCount, int failureCount,
                                   List<PerTokenResult> results) {
    }

    public record PerTokenResult(String token, boolean success, String messageId, String errorCode,
                                 String errorMessage) {
    }
}
