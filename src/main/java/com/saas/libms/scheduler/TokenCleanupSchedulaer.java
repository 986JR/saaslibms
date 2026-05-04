package com.saas.libms.scheduler;


import com.saas.libms.auth.blacklist.BlacklistedTokenRepository;
import com.saas.libms.auth.session.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupSchedulaer {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final RefreshSessionRepository refreshSessionRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredToken() {
        LocalDateTime now = LocalDateTime.now();

        int deletedBlacklisted = blacklistedTokenRepository.deleteAllExpiredBefore(now);
        int deletedSessions = refreshSessionRepository.deleteAllExpiredBefore(now);

        log.info("[TokenCleanup] Remove {} blacklisted token(s) and {} sessions(s)",
                deletedBlacklisted,deletedSessions);
    }

}
