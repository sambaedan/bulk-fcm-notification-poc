package com.fcm.fcm_demo.service;

import com.fcm.fcm_demo.entity.Logs;
import com.fcm.fcm_demo.repository.LogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogsRepository logsRepository;

    // Existing save method
    public void saveFcmLog(
            String trackingId,
            String username,
            int tokenCount,
            int successCount,
            int failureCount,
            String status,
            String errorCode,
            String extraData
    ) {
        Logs log = new Logs();
        log.setTrackingId(trackingId);
        log.setUsername(username);
        log.setTokenCount(tokenCount);
        log.setSuccessCount(successCount);
        log.setFailureCount(failureCount);
        log.setStatus(status);
        log.setErrorCode(errorCode);
        log.setExtraData(extraData);

        logsRepository.save(log);
    }

    public void updateFcmLog(
            String username,
            String trackingId,
            int successCount,
            int failureCount,
            String status,
            String errorCode,
            String extraData
    ) {
        Optional<Logs> optionalLog = logsRepository.findTopByTrackingIdAndUsernameOrderByCreatedAtDesc(trackingId, username);

        if (optionalLog.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking ID not found: " + trackingId);
        }

        Logs log = optionalLog.get();
        log.setSuccessCount(successCount);
        log.setFailureCount(failureCount);

        if (status != null) {
            log.setStatus(status);
        }

        if (errorCode != null) {
            log.setErrorCode(errorCode);
        }

        if (extraData != null) {
            log.setExtraData(extraData);
        }

        logsRepository.save(log);
    }

    public List<Logs> findAll(String trackingId) {
        return logsRepository.findAllByTrackingId((trackingId));
    }
}
