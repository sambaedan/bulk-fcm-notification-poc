
package com.fcm.fcm_demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.fcm_demo.config.RateLimiterConfig;
import com.fcm.fcm_demo.fcm.FcmClient;
import com.fcm.fcm_demo.fcm.FcmRequest;
import com.fcm.fcm_demo.util.ChunkingUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends notifications using Firebase Admin SDK's multicast API (sendEachForMulticast)
 * via FcmClient.sendTokens(...). Retries only retryable errors per token.
 */
@Service
@RequiredArgsConstructor
public class NotificationBatchService {
    private static final Logger log = LoggerFactory.getLogger(NotificationBatchService.class);

    private final ExecutorService executor;
    private final FcmClient fcmClient;
    private final RateLimiterConfig.SimpleRateLimiter rateLimiter;
    private final LogService logService;

    // RetryPolicy(maxAttempts, minBackoffMs, maxBackoffMs, exponential)
    private final RetryPolicy retryPolicy = new RetryPolicy(5, 500, 10_000, true);

    public void process(FcmRequest request) {
        final int batchSize = 500;
        final List<List<String>> chunks = ChunkingUtils.chunks(request.tokens(), batchSize);
        final String batchId = request.data().get("rand");
        logService.saveFcmLog(
                batchId,
                request.username(),
                request.tokens().size(),
                0,
                0,
                "IN_PROGRESS",
                null,
                "{\"message\":\"Batch started\"}"
        );

        log.info("Processing bulk request: batchId={} user={} totalTokens={} chunks={}",
                batchId, request.username(), request.tokens().size(), chunks.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (final List<String> tokensChunk : chunks) {
            executor.submit(() -> {
                try {
                    rateLimiter.awaitAcquire(tokensChunk.size());
                    ChunkResult result = sendChunkWithRetry(tokensChunk, request, batchId);
                    successCount.addAndGet(result.success);
                    failedCount.addAndGet(result.failed);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted: batchId={} user={} chunkSize={}", batchId, request.username(), tokensChunk.size(), ie);
                    failedCount.addAndGet(tokensChunk.size());
                    logService.updateFcmLog(
                            request.username(),
                            batchId,
                            successCount.get(),
                            failedCount.get(),
                            "FAILED",
                            "500",
                            "{\"message\":\"Thread interrupted\"}"
                    );
                } catch (RuntimeException re) {
                    log.error("Runtime error: batchId={} user={} chunkSize={} err={}", batchId, request.username(), tokensChunk.size(), re.getMessage(), re);
                    failedCount.addAndGet(tokensChunk.size());
                    logService.updateFcmLog(
                            request.username(),
                            batchId,
                            successCount.get(),
                            failedCount.get(),
                            "FAILED",
                            "500",
                            "{\"message\":\"Runtime exception\"}"
                    );
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        log.info("Submitted all chunks: batchId={} user={} submittedTokens={}", batchId, request.username(), request.tokens().size());
    }

    private ChunkResult sendChunkWithRetry(
            List<String> initialTokens,
            FcmRequest request,
            String batchId
    ) throws JsonProcessingException {

        Set<String> pending = new LinkedHashSet<>(initialTokens);
        Set<String> permanentlyFailed = new LinkedHashSet<>();
        int totalSuccess = 0;

        for (int attempt = 1;
             attempt <= retryPolicy.maxAttempts() && !pending.isEmpty();
             attempt++) {

            FcmClient.BatchFcmResponse batchResp = fcmClient.sendTokens(
                    new ArrayList<>(pending),
                    request.title(),
                    request.body(),
                    request.data()
            );

            Set<String> nextPending = new LinkedHashSet<>();

            if (batchResp.results() != null) {
                for (FcmClient.PerTokenResult r : batchResp.results()) {

                    if (r.success()) {
                        totalSuccess++;
                        log.info("FCM sent successfully: user={} token={} messageId={}",
                                request.username(), r.token(), r.messageId());

                        logService.updateFcmLog(
                                request.username(),
                                batchId,
                                totalSuccess,
                                permanentlyFailed.size(),
                                "SUCCESS",
                                null,
                                "{\"message\":\"FCM sent successfully\"}"
                        );
                    } else {
                        final String errCode = r.errorCode();
                        final String errMsg = r.errorMessage();

                        if (isRetryable(errCode)) {
                            nextPending.add(r.token());
                        } else {
                            permanentlyFailed.add(r.token());
                        }

                        logService.updateFcmLog(
                                request.username(),
                                batchId,
                                totalSuccess,
                                permanentlyFailed.size(),
                                "FAILED",
                                errCode + errMsg,
                                "{\"message\":\"FCM send failed\"}"
                        );

                        log.info(
                                "FCM failed: user={} token={} attempt={} errorCode={} message={}",
                                request.username(), r.token(), attempt, errCode, errMsg
                        );
                    }
                }
            }

            if (nextPending.isEmpty()) {
                pending.clear();
                break;
            }

            long delay = retryPolicy.backoff(attempt);
            log.info(
                    "Retrying {} tokens after {} ms: batchId={} user={} attempt={}",
                    nextPending.size(), delay, batchId, request.username(), attempt
            );

            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                permanentlyFailed.addAll(nextPending);

                logService.updateFcmLog(
                        request.username(),
                        batchId,
                        totalSuccess,
                        permanentlyFailed.size(),
                        "FAILED",
                        "CHUNK_INTERRUPTED",
                        "{\"message\":\"Thread interrupted\"}"
                );
                break;
            }

            pending.clear();
            pending.addAll(nextPending);
        }

        permanentlyFailed.addAll(pending);

        log.info(
                "Chunk summary: batchId={} user={} success={} failed={}",
                batchId, request.username(), totalSuccess, permanentlyFailed.size()
        );

        return new ChunkResult(totalSuccess, permanentlyFailed.size());
    }


    /**
     * Decide retryability based on Admin SDK MessagingErrorCode.
     * Retry on transient/server-side conditions; don't retry on token/client errors.
     */
    private boolean isRetryable(String messagingErrorCode) {
        if (messagingErrorCode == null) return true;
        // unknown -> conservative retry
        // Transient/server-side: INTERNAL, UNAVAILABLE, QUOTA_EXCEEDED
        // Not retryable: UNREGISTERED, SENDER_ID_MISMATCH, INVALID_ARGUMENT, THIRD_PARTY_AUTH_ERROR
        return switch (messagingErrorCode) {
            case "INTERNAL", "UNAVAILABLE", "QUOTA_EXCEEDED" -> true;
            case "UNREGISTERED", "SENDER_ID_MISMATCH", "INVALID_ARGUMENT", "THIRD_PARTY_AUTH_ERROR" -> false;
            default -> true; // e.g., generic "DEADLINE_EXCEEDED" from underlying transport
        };
    }

    private record ChunkResult(int success, int failed) {
    }
}
