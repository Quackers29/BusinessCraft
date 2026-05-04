package com.quackers29.businesscraft.error;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ErrorMetrics {
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong recentErrors = new AtomicLong(0);
    private final ConcurrentLinkedQueue<ErrorOccurrence> errorHistory = new ConcurrentLinkedQueue<>();
    private final Map<String, AtomicLong> errorMessageCounts = new ConcurrentHashMap<>();
    
    private volatile Instant firstErrorTime;
    private volatile Instant lastErrorTime;
    private volatile Instant lastResetTime = Instant.now();
    
    private static final int MAX_HISTORY_SIZE = 100;
    private static final long RECENT_WINDOW_MINUTES = 60;

    public void recordError(Instant timestamp, Exception exception) {
        totalErrors.incrementAndGet();

        if (firstErrorTime == null) {
            firstErrorTime = timestamp;
        }
        lastErrorTime = timestamp;

        ErrorOccurrence occurrence = new ErrorOccurrence(timestamp);
        errorHistory.offer(occurrence);

        while (errorHistory.size() > MAX_HISTORY_SIZE) {
            errorHistory.poll();
        }

        String message = exception.getMessage() != null ? exception.getMessage() : "No message";
        errorMessageCounts.computeIfAbsent(message, k -> new AtomicLong(0)).incrementAndGet();

        updateRecentErrorsCount();
    }

    private void updateRecentErrorsCount() {
        Instant cutoff = Instant.now().minus(RECENT_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long recentCount = errorHistory.stream()
            .mapToLong(occurrence -> occurrence.timestamp.isAfter(cutoff) ? 1 : 0)
            .sum();
        recentErrors.set(recentCount);
    }
    
    public long getTotalErrors() {
        return totalErrors.get();
    }

    public long getRecentErrors() {
        updateRecentErrorsCount();
        return recentErrors.get();
    }

    public double getErrorRatePerMinute() {
        updateRecentErrorsCount();
        return (double) recentErrors.get() / RECENT_WINDOW_MINUTES;
    }

    public double getOverallErrorRatePerMinute() {
        if (firstErrorTime == null || lastErrorTime == null) {
            return 0.0;
        }
        
        long totalMinutes = ChronoUnit.MINUTES.between(firstErrorTime, lastErrorTime);
        if (totalMinutes == 0) {
            return totalErrors.get();
        }

        return (double) totalErrors.get() / totalMinutes;
    }

    public Instant getFirstErrorTime() {
        return firstErrorTime;
    }

    public Instant getLastErrorTime() {
        return lastErrorTime;
    }

    public long getTotalDurationMinutes() {
        if (firstErrorTime == null) {
            return 0;
        }

        Instant endTime = lastErrorTime != null ? lastErrorTime : Instant.now();
        return ChronoUnit.MINUTES.between(firstErrorTime, endTime);
    }

    public Map<String, Long> getTopErrorMessages(int limit) {
        return errorMessageCounts.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get(),
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }

    public ErrorFrequency getErrorFrequency() {
        updateRecentErrorsCount();

        Instant now = Instant.now();
        long last5Minutes = countErrorsSince(now.minus(5, ChronoUnit.MINUTES));
        long last15Minutes = countErrorsSince(now.minus(15, ChronoUnit.MINUTES));
        long last30Minutes = countErrorsSince(now.minus(30, ChronoUnit.MINUTES));
        long lastHour = recentErrors.get();
        
        return new ErrorFrequency(last5Minutes, last15Minutes, last30Minutes, lastHour);
    }
    
    private long countErrorsSince(Instant since) {
        return errorHistory.stream()
            .mapToLong(occurrence -> occurrence.timestamp.isAfter(since) ? 1 : 0)
            .sum();
    }

    public boolean hasErrorSpike() {
        ErrorFrequency frequency = getErrorFrequency();

        double averagePerMinute = (double) frequency.lastHour / 60;
        double recentPerMinute = (double) frequency.last5Minutes / 5;

        return recentPerMinute > (averagePerMinute * 3) && frequency.last5Minutes > 5;
    }

    public ErrorSummary getSummary() {
        updateRecentErrorsCount();
        return new ErrorSummary(
            totalErrors.get(),
            recentErrors.get(),
            getErrorRatePerMinute(),
            getOverallErrorRatePerMinute(),
            firstErrorTime,
            lastErrorTime,
            hasErrorSpike(),
            getTopErrorMessages(5)
        );
    }

    public void reset() {
        totalErrors.set(0);
        recentErrors.set(0);
        errorHistory.clear();
        errorMessageCounts.clear();
        firstErrorTime = null;
        lastErrorTime = null;
        lastResetTime = Instant.now();
    }

    public Instant getLastResetTime() {
        return lastResetTime;
    }

    private static class ErrorOccurrence {
        final Instant timestamp;

        ErrorOccurrence(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class ErrorFrequency {
        public final long last5Minutes;
        public final long last15Minutes;
        public final long last30Minutes;
        public final long lastHour;

        ErrorFrequency(long last5Minutes, long last15Minutes, long last30Minutes, long lastHour) {
            this.last5Minutes = last5Minutes;
            this.last15Minutes = last15Minutes;
            this.last30Minutes = last30Minutes;
            this.lastHour = lastHour;
        }

        @Override
        public String toString() {
            return String.format("ErrorFrequency{5min=%d, 15min=%d, 30min=%d, 1hr=%d}",
                               last5Minutes, last15Minutes, last30Minutes, lastHour);
        }
    }

    public static class ErrorSummary {
        public final long totalErrors;
        public final long recentErrors;
        public final double recentErrorRate;
        public final double overallErrorRate;
        public final Instant firstError;
        public final Instant lastError;
        public final boolean hasSpike;
        public final Map<String, Long> topMessages;

        ErrorSummary(long totalErrors, long recentErrors, double recentErrorRate, double overallErrorRate,
                    Instant firstError, Instant lastError, boolean hasSpike, Map<String, Long> topMessages) {
            this.totalErrors = totalErrors;
            this.recentErrors = recentErrors;
            this.recentErrorRate = recentErrorRate;
            this.overallErrorRate = overallErrorRate;
            this.firstError = firstError;
            this.lastError = lastError;
            this.hasSpike = hasSpike;
            this.topMessages = topMessages;
        }

        @Override
        public String toString() {
            return String.format("ErrorSummary{total=%d, recent=%d, recentRate=%.2f/min, overallRate=%.2f/min, spike=%s}",
                               totalErrors, recentErrors, recentErrorRate, overallErrorRate, hasSpike);
        }
    }
}
