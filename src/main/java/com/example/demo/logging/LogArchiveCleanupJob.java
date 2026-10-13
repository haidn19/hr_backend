package com.example.demo.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes only archived Logback files. The active application.log file is never deleted.
 */
@Component
public class LogArchiveCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(LogArchiveCleanupJob.class);
    private static final DateTimeFormatter ARCHIVE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern ARCHIVE_FILE =
            Pattern.compile("^application\\.log\\.(\\d{4}-\\d{2}-\\d{2})\\.\\d+\\.gz$");

    private final Path logDirectory;
    private final ZoneId zoneId;
    private final int retentionDays;

    public LogArchiveCleanupJob(
            @Value("${app.log-cleanup.directory:logs}") String logDirectory,
            @Value("${app.log-cleanup.zone:Asia/Ho_Chi_Minh}") String zone,
            @Value("${app.log-cleanup.retention-days:2}") int retentionDays) {
        this.logDirectory = Path.of(logDirectory);
        this.zoneId = ZoneId.of(zone);
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.log-cleanup.cron:0 0 1 * * *}",
            zone = "${app.log-cleanup.zone:Asia/Ho_Chi_Minh}")
    public void deleteExpiredArchives() {
        if (!Files.isDirectory(logDirectory)) {
            return;
        }

        LocalDate oldestDateToKeep = LocalDate.now(zoneId).minusDays(retentionDays - 1L);
        AtomicInteger deletedFiles = new AtomicInteger();

        try (Stream<Path> paths = Files.list(logDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isExpiredArchive)
                    .forEach(path -> delete(path, oldestDateToKeep, deletedFiles));
        } catch (IOException exception) {
            log.error("event=log_archive_cleanup_failed directory={}", logDirectory, exception);
            return;
        }

        log.info("event=log_archive_cleanup_completed directory={} retentionDays={} deletedFiles={}",
                logDirectory, retentionDays, deletedFiles.get());
    }

    private boolean isExpiredArchive(Path path) {
        Matcher matcher = ARCHIVE_FILE.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return false;
        }

        LocalDate archiveDate = LocalDate.parse(matcher.group(1), ARCHIVE_DATE);
        LocalDate oldestDateToKeep = LocalDate.now(zoneId).minusDays(retentionDays - 1L);
        return archiveDate.isBefore(oldestDateToKeep);
    }

    private void delete(Path path, LocalDate oldestDateToKeep, AtomicInteger deletedFiles) {
        try {
            if (Files.deleteIfExists(path)) {
                deletedFiles.incrementAndGet();
                log.info("event=log_archive_deleted file={} oldestDateToKeep={}", path, oldestDateToKeep);
            }
        } catch (IOException exception) {
            log.error("event=log_archive_delete_failed file={}", path, exception);
        }
    }
}
