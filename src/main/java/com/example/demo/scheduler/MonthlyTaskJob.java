package com.example.demo.scheduler;

import com.example.demo.service.MonthlyAttendanceReportEmailService;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonthlyTaskJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyTaskJob.class);
    private final MonthlyAttendanceReportEmailService reportEmailService;

    public MonthlyTaskJob(MonthlyAttendanceReportEmailService reportEmailService) {
        this.reportEmailService = reportEmailService;
    }

    @Scheduled(
            cron = "${app.monthly-task.cron:0 0 10 5 * *}",
            zone = "${app.monthly-task.zone:Asia/Ho_Chi_Minh}"
    )
    public void run() {
        YearMonth reportMonth = YearMonth.now().minusMonths(1);
        log.info("event=monthly_attendance_email_started month={}", reportMonth);
        reportEmailService.sendReports(reportMonth);
    }
}
