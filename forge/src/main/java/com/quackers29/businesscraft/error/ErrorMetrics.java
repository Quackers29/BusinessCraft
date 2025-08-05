package com.quackers29.businesscraft.error;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks detailed metrics for error analysis and monitoring.
 * Provides statistics for error frequency, patterns, and trends.
 */
public class ErrorMetrics {
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong recentErrors = new AtomicLong(0);
    private final ConcurrentLinkedQueue<ErrorOccurrence> errorHistory = new ConcurrentLinkedQueue<>();
    private final Map<String, AtomicLong> errorMessageCounts = new ConcurrentHashMap<>();
    
    private volatile Instant firstErrorTime;
    private volatile Instant lastErrorTime;
    private volatile Instant lastResetTime = Instant.now();
    
    // Configuration
    private static final int MAX_HISTORY_SIZE = 100;
    private static final long RECENT_WINDOW_MINUTES = 60; // 1 hour window for "recent" errors
    
    /**
     * Records a new error occurrence.
     */
    public void recordError(Instant timestamp, Exception exception) {
        totalErrors.incrementAndGet();
        
        // Update timestamps
        if (firstErrorTime == null) {
            firstErrorTime = timestamp;
        }
        lastErrorTime = timestamp;
        
        // Add to history
        ErrorOccurrence occurrence = new ErrorOccurrence(timestamp, exception);
        errorHistory.offer(occurrence);
        
        // Trim history to max size
        while (errorHistory.size() > MAX_HISTORY_SIZE) {
            errorHistory.poll();
        }
        
        // Update message counts
        String message = exception.getMessage() != null ? exception.getMessage() : "No message";
        errorMessageCounts.computeIfAbsent(message, k -> new AtomicLong(0)).incrementAndGet();
        
        // Update recent errors count
        updateRecentErrorsCount();
    }
    
    /**
     * Updates the count of recent errors within the time window.
     */
    private void updateRecentErrorsCount() {
        Instant cutoff = Instant.now().minus(RECENT_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long recentCount = errorHistory.stream()
            .mapToLong(occurrence -> occurrence.timestamp.isAfter(cutoff) ? 1 : 0)
            .sum();
        recentErrors.set(recentCount);
    }
    
    /**
     * Gets the total number of errors recorded.
     */
    public long getTotalErrors() {
        return totalErrors.get();
    }
    
    /**
     * Gets the number of recent errors within the time window.
     */
    public long getRecentErrors() {
        updateRecentErrorsCount();
        return recentErrors.get();
    }
    
    /**
     * Gets the error rate per minute over the recent window.
     */
    public double getErrorRatePerMinute() {
        updateRecentErrorsCount();
        return (double) recentErrors.get() / RECENT_WINDOW_MINUTES;
    }
    
    /**
     * Gets the error rate per minute since the first error.
     */
    public double getOverallErrorRatePerMinute() {
        if (firstErrorTime == null || lastErrorTime == null) {
            return 0.0;
        }
        
        long totalMinutes = ChronoUnit.MINUTES.between(firstErrorTime, lastErrorTime);
        if (totalMinutes == 0) {
            return totalErrors.get(); // All errors happened in less than a minute
        }
        
        return (double) totalErrors.get() / totalMinutes;
    }
    
    /**
     * Gets the time of the first recorded error.
     */
    public Instant getFirstErrorTime() {
        return firstErrorTime;
    }
    
    /**
     * Gets the time of the most recent error.
     */
    public Instant getLastErrorTime() {
        return lastErrorTime;
    }
    
    /**
     * Gets the duration since the first error.
     */
    public long getTotalDurationMinutes() {
        if (firstErrorTime == null) {
            return 0;
        }
        
        Instant endTime = lastErrorTime != null ? lastErrorTime : Instant.now();
        return ChronoUnit.MINUTES.between(firstErrorTime, endTime);
    }
    
    /**
     * Gets the most common error messages and their frequencies.
     */
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
    
    /**
     * Gets error frequency in different time buckets.
     */
    public ErrorFrequency getErrorFrequency() {
        updateRecentErrorsCount();
        
        Instant now = Instant.now();
        long last5Minutes = countErrorsSince(now.minus(5, ChronoUnit.MINUTES));
        long last15Minutes = countErrorsSince(now.minus(15, ChronoUnit.MINUTES));
        long last30Minutes = countErrorsSince(now.minus(30, ChronoUnit.MINUTES));
        long lastHour = recentErrors.get();
        
        return new ErrorFrequency(last5Minutes, last15Minutes, last30Minutes, lastHour);
    }
    
    /**
     * Counts errors since a specific timestamp.
     */
    private long countErrorsSince(Instant since) {
        return errorHistory.stream()
            .mapToLong(occurrence -> occurrence.timestamp.isAfter(since) ? 1 : 0)
            .sum();
    }
    
    /**
     * Checks if there's an error spike in recent activity.
     */
    public boolean hasErrorSpike() {
        ErrorFrequency frequency = getErrorFrequency();
        
        // Define spike as 3x more errors in last 5 minutes compared to average
        double averagePerMinute = (double) frequency.lastHour / 60;
        double recentPerMinute = (double) frequency.last5Minutes / 5;
        
        return recentPerMinute > (averagePerMinute * 3) && frequency.last5Minutes > 5;
    }
    
    /**
     * Gets a summary of the error metrics.
     */
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
    
    /**
     * Resets all metrics.
     */
    public void reset() {
        totalErrors.set(0);
        recentErrors.set(0);
        errorHistory.clear();
        errorMessageCounts.clear();
        firstErrorTime = null;
        lastErrorTime = null;
        lastResetTime = Instant.now();
    }
    
    /**
     * Gets the time when metrics were last reset.
     */
    public Instant getLastResetTime() {
        return lastResetTime;
    }
    
    /**
     * Internal class to track individual error occurrences.
     */
    private static class ErrorOccurrence {
        final Instant timestamp;
        final String exceptionType;
        final String message;
        
        ErrorOccurrence(Instant timestamp, Exception exception) {
            this.timestamp = timestamp;
            this.exceptionType = exception.getClass().getSimpleName();
            this.message = exception.getMessage();
        }
    }
    
    /**
     * Error frequency data structure.
     */
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
    
    /**
     * Comprehensive error summary data structure.
     */
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