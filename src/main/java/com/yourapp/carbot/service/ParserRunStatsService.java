package com.yourapp.carbot.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ParserRunStatsService {

    private LocalDateTime lastRunAt;

    private int totalParsedUnique;
    private int totalSaved;

    private final Map<String, ParserStats> parserStats = new LinkedHashMap<>();

    public void reset() {
        lastRunAt = LocalDateTime.now();
        totalParsedUnique = 0;
        totalSaved = 0;
        parserStats.clear();
    }

    public void recordParserResult(String source,
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

    public void recordParserFailed(String source) {

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

    public void setTotalParsedUnique(int totalParsedUnique) {
        this.totalParsedUnique = totalParsedUnique;
    }

    public void setTotalSaved(int totalSaved) {
        this.totalSaved = totalSaved;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public int getTotalParsedUnique() {
        return totalParsedUnique;
    }

    public int getTotalSaved() {
        return totalSaved;
    }

    public Map<String, ParserStats> getParserStats() {
        return parserStats;
    }

    public record ParserStats(
            int returned,
            int added,
            int duplicatesSkipped,
            int invalidSkipped,
            boolean failed
    ) {}
}