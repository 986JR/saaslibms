package com.saas.libms.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsCleanupScheduler {
    private static final int RETENTION_DAYS = 90;

    private final ApiRequestLogRepository  apiRequestLogRepository;
    private final BookViewEventRepository  bookViewEventRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanOldAnalyticsData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        int deletedRequests = apiRequestLogRepository.deleteOlderThan(cutoff);
        int deletedViews    = bookViewEventRepository.deleteOlderThan(cutoff);

        log.info("Analytics cleanup complete — deleted {} request logs and {} view events older than {} days",
                deletedRequests, deletedViews, RETENTION_DAYS);
    }
}
