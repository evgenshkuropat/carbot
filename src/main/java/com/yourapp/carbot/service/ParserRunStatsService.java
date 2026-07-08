package com.yourapp.carbot.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ParserRunStatsService {

    private LocalDateTime lastRunAt;
    private LocalDateTime lastFinishedAt;
    private boolean running;

    private int totalParsedUnique;
    private int totalSaved;

    private final Map<String, ParserStats> parserStats = new LinkedHashMap<>();

    public synchronized void reset() {
        lastRunAt = LocalDateTime.now();
        lastFinishedAt = null;
        running = true;
        totalParsedUnique = 0;
        totalSaved = 0;
        parserStats.clear();
    }

    public synchronized void finish() {
        lastFinishedAt = LocalDateTime.now();
        running = false;
    }

    public synchronized void recordParserResult(String source,
                                                int returned,
                                                int added,
                                                int duplicatesSkipped,
                                                int invalidSkipped) {

        parserStats.put(source,
                new ParserStats(
                        returned,
                        added,
                        duplicatesSkipped,
                        invalidSkipped,
                        false
                )
        );
    }

    public synchronized void recordParserFailed(String source) {

        parserStats.put(source,
                new ParserStats(
                        0,
                        0,
                        0,
                        0,
                        true
                )
        );
    }

    public synchronized void setTotalParsedUnique(int totalParsedUnique) {
        this.totalParsedUnique = totalParsedUnique;
    }

    public synchronized void setTotalSaved(int totalSaved) {
        this.totalSaved = totalSaved;
    }

    public synchronized LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public synchronized LocalDateTime getLastFinishedAt() {
        return lastFinishedAt;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized int getTotalParsedUnique() {
        return totalParsedUnique;
    }

    public synchronized int getTotalSaved() {
        return totalSaved;
    }

    public synchronized Map<String, ParserStats> getParserStats() {
        return new LinkedHashMap<>(parserStats);
    }

    public record ParserStats(
            int returned,
            int added,
            int duplicatesSkipped,
            int invalidSkipped,
            boolean failed
    ) {}
}
