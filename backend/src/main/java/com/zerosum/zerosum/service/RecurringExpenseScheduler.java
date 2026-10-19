package com.zerosum.zerosum.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringExpenseScheduler {
    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseScheduler.class);
    private final RecurringExpenseService service;

    public RecurringExpenseScheduler(RecurringExpenseService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.recurring.cron:0 0 0 * * *}")
    public void generateDueExpenses() {
        int count = service.generateDueExpenses();
        if (count > 0) log.info("Generated {} recurring expenses", count);
    }
}
