package com.saas.libms.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestMetricsWriterService {
    private final ApiRequestLogRepository apiRequestLogRepository;

    /**
     * Write a request log entry on a background thread.
     * Exceptions are caught internally — a failed write must never
     * propagate back or affect the HTTP response already sent.
     */
    @Async
    @Transactional
    public void write(String endpoint, int statusCode, long durationMs,
                      UUID institutionId, String clientIp) {
        try {
            ApiRequestLog log = ApiRequestLog.builder()
                    .endpoint(endpoint)
                    .statusCode(statusCode)
                    .durationMs(durationMs)
                    .institutionId(institutionId)
                    .clientIp(clientIp)
                    .build();

            apiRequestLogRepository.save(log);
        } catch (Exception e) {
            log.warn("Failed to write request metrics log for endpoint {}: {}",
                    endpoint, e.getMessage());
        }
    }
}
