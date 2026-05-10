package com.saas.libms.scheduler;

import com.saas.libms.loan.LoanRepository;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanOverdueScheduler {

    private final LoanRepository loanRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueLoans() {
        int updated = loanRepository.markOverdueLoanAsLate(LocalDate.now());
        if (updated > 0) {
            log.info("[LoanOverdueScheduler] Marked {} overdue loan(s) as LATE.", updated);
        }
    }
}
