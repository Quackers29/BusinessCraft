package com.quackers29.businesscraft.error;

import com.quackers29.businesscraft.util.BCError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Comprehensive error reporting and analysis utility.
 * Generates detailed error reports for debugging, monitoring, and operational insights.
 * Provides both human-readable and machine-parseable output formats.
 */
public class ErrorReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorReporter.class);
    
    private static final ErrorHandler errorHandler = ErrorHandler.getInstance();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Generates a comprehensive error report for debugging purposes.
     */
    public static String generateComprehensiveReport() {
        StringBuilder report = new StringBuilder();
        
        try {
            report.append("=== BusinessCraft Error Analysis Report ===\n");
            report.append("Generated at: ").append(TIMESTAMP_FORMAT.format(Instant.now())).append("\n\n");
            
            // Error counts section
            appendErrorCountsSection(report);
            
            // Error metrics section
            appendErrorMetricsSection(report);
            
            // System health section
            appendSystemHealthSection(report);
            
            // Recommendations section
            appendRecommendationsSection(report);
            
            report.append("=== End of Report ===\n");
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate comprehensive error report", e);
            report.append("ERROR: Failed to generate report - ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Generates a summary error report for quick overview.
     */
    public static String generateSummaryReport() {
        StringBuilder report = new StringBuilder();
        
        try {
            var errorCounts = errorHandler.getErrorCounts();
            
            if (errorCounts.isEmpty()) {
                return "System Status: All systems operational, no errors recorded.";
            }
            
            long totalErrors = errorCounts.values().stream().mapToLong(Long::longValue).sum();
            
            report.append("Error Summary Report\n");
            report.append("-------------------\n");
            report.append(String.format("Total Errors: %d\n", totalErrors));
            
            // Top 3 most frequent errors
            var topErrors = errorCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .collect(Collectors.toList());
            
            if (!topErrors.isEmpty()) {
                report.append("Most Frequent Errors:\n");
                for (int i = 0; i < topErrors.size(); i++) {
                    var entry = topErrors.get(i);
                    report.append(String.format("  %d. %s (%d occurrences)\n", 
                        i + 1, entry.getKey(), entry.getValue()));
                }
            }
            
            // System health assessment
            String healthStatus = assessSystemHealth(errorCounts);
            report.append(String.format("System Health: %s\n", healthStatus));
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate summary error report", e);
            report.append("ERROR: Failed to generate summary report - ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Generates a JSON-formatted report for machine consumption.
     */
    public static String generateJsonReport() {
        try {
            var errorCounts = errorHandler.getErrorCounts();
            var errorMetrics = errorHandler.getErrorMetrics();
            
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"timestamp\": \"").append(TIMESTAMP_FORMAT.format(Instant.now())).append("\",\n");
            json.append("  \"totalErrors\": ").append(errorCounts.values().stream().mapToLong(Long::longValue).sum()).append(",\n");
            
            // Error counts
            json.append("  \"errorCounts\": {\n");
            StringJoiner countJoiner = new StringJoiner(",\n    ", "    ", "\n");
            errorCounts.forEach((key, value) -> 
                countJoiner.add(String.format("\"%s\": %d", key, value)));
            json.append(countJoiner.toString());
            json.append("  },\n");
            
            // Error metrics summary
            json.append("  \"errorMetrics\": {\n");
            StringJoiner metricsJoiner = new StringJoiner(",\n    ", "    ", "\n");
            errorMetrics.forEach((key, metrics) -> {
                var summary = metrics.getSummary();
                metricsJoiner.add(String.format("\"%s\": {\"recent\": %d, \"rate\": %.2f, \"spike\": %s}", 
                    key, summary.recentErrors, summary.recentErrorRate, summary.hasSpike));
            });
            json.append(metricsJoiner.toString());
            json.append("  },\n");
            
            // System health
            json.append("  \"systemHealth\": \"").append(assessSystemHealth(errorCounts)).append("\"\n");
            json.append("}\n");
            
            return json.toString();
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate JSON error report", e);
            return String.format("{\"error\": \"Failed to generate report: %s\"}", e.getMessage());
        }
    }
    
    /**
     * Appends error counts section to the report.
     */
    private static void appendErrorCountsSection(StringBuilder report) {
        report.append("## Error Counts by Type\n");
        
        var errorCounts = errorHandler.getErrorCounts();
        
        if (errorCounts.isEmpty()) {
            report.append("No errors recorded.\n\n");
            return;
        }
        
        long totalErrors = errorCounts.values().stream().mapToLong(Long::longValue).sum();
        report.append(String.format("Total errors recorded: %d\n\n", totalErrors));
        
        // Sort by frequency
        errorCounts.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .forEach(entry -> {
                double percentage = (double) entry.getValue() / totalErrors * 100;
                report.append(String.format("  %-40s: %6d (%5.1f%%)\n", 
                    entry.getKey(), entry.getValue(), percentage));
            });
        
        report.append("\n");
    }
    
    /**
     * Appends error metrics section to the report.
     */
    private static void appendErrorMetricsSection(StringBuilder report) {
        report.append("## Detailed Error Metrics\n");
        
        var errorMetrics = errorHandler.getErrorMetrics();
        
        if (errorMetrics.isEmpty()) {
            report.append("No detailed metrics available.\n\n");
            return;
        }
        
        errorMetrics.forEach((key, metrics) -> {
            var summary = metrics.getSummary();
            var frequency = metrics.getErrorFrequency();
            
            report.append(String.format("### %s\n", key));
            report.append(String.format("  Total Errors: %d\n", summary.totalErrors));
            report.append(String.format("  Recent Errors (1h): %d\n", summary.recentErrors));
            report.append(String.format("  Error Rate: %.2f/min (recent), %.2f/min (overall)\n", 
                summary.recentErrorRate, summary.overallErrorRate));
            
            if (summary.firstError != null) {
                report.append(String.format("  First Error: %s\n", TIMESTAMP_FORMAT.format(summary.firstError)));
            }
            if (summary.lastError != null) {
                report.append(String.format("  Last Error: %s\n", TIMESTAMP_FORMAT.format(summary.lastError)));
            }
            
            report.append(String.format("  Error Spike Detected: %s\n", summary.hasSpike ? "YES" : "No"));
            
            // Frequency breakdown
            report.append(String.format("  Frequency: Last 5min: %d, 15min: %d, 30min: %d, 1h: %d\n", 
                frequency.last5Minutes, frequency.last15Minutes, frequency.last30Minutes, frequency.lastHour));
            
            // Top error messages
            if (!summary.topMessages.isEmpty()) {
                report.append("  Most Common Messages:\n");
                summary.topMessages.entrySet().stream()
                    .limit(3)
                    .forEach(entry -> report.append(String.format("    - \"%s\" (%d times)\n", 
                        entry.getKey(), entry.getValue())));
            }
            
            report.append("\n");
        });
    }
    
    /**
     * Appends system health section to the report.
     */
    private static void appendSystemHealthSection(StringBuilder report) {
        report.append("## System Health Assessment\n");
        
        var errorCounts = errorHandler.getErrorCounts();
        var errorMetrics = errorHandler.getErrorMetrics();
        
        String overallHealth = assessSystemHealth(errorCounts);
        report.append(String.format("Overall System Health: %s\n\n", overallHealth));
        
        // Component-specific health
        Map<String, String> componentHealth = new ConcurrentHashMap<>();
        
        errorCounts.keySet().forEach(errorType -> {
            if (errorType.contains(".")) {
                String component = errorType.split("\\.")[0];
                long componentErrors = errorCounts.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(component + "."))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
                
                String health = componentErrors < 5 ? "Healthy" : 
                               componentErrors < 20 ? "Warning" : "Critical";
                componentHealth.put(component, health);
            }
        });
        
        if (!componentHealth.isEmpty()) {
            report.append("Component Health Status:\n");
            componentHealth.forEach((component, health) -> 
                report.append(String.format("  %-20s: %s\n", component, health)));
            report.append("\n");
        }
        
        // Error spike detection
        boolean hasAnySpikes = errorMetrics.values().stream()
            .anyMatch(metrics -> metrics.getSummary().hasSpike);
        
        if (hasAnySpikes) {
            report.append("⚠️  ERROR SPIKES DETECTED:\n");
            errorMetrics.entrySet().stream()
                .filter(entry -> entry.getValue().getSummary().hasSpike)
                .forEach(entry -> report.append(String.format("  - %s (spike in recent activity)\n", 
                    entry.getKey())));
            report.append("\n");
        }
    }
    
    /**
     * Appends recommendations section to the report.
     */
    private static void appendRecommendationsSection(StringBuilder report) {
        report.append("## Recommendations\n");
        
        var errorCounts = errorHandler.getErrorCounts();
        var errorMetrics = errorHandler.getErrorMetrics();
        
        if (errorCounts.isEmpty()) {
            report.append("✅ No issues detected. System is operating normally.\n\n");
            return;
        }
        
        // High frequency error recommendations
        var highFrequencyErrors = errorCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 10)
            .collect(Collectors.toList());
        
        if (!highFrequencyErrors.isEmpty()) {
            report.append("🔍 High Frequency Errors (>10 occurrences):\n");
            highFrequencyErrors.forEach(entry -> {
                report.append(String.format("  - Investigate '%s' (%d occurrences)\n", 
                    entry.getKey(), entry.getValue()));
                
                // Provide specific recommendations based on error type
                String recommendation = getErrorRecommendation(entry.getKey());
                if (recommendation != null) {
                    report.append(String.format("    Recommendation: %s\n", recommendation));
                }
            });
            report.append("\n");
        }
        
        // Error spike recommendations
        boolean hasSpikes = errorMetrics.values().stream()
            .anyMatch(metrics -> metrics.getSummary().hasSpike);
        
        if (hasSpikes) {
            report.append("⚡ Error Spike Mitigation:\n");
            report.append("  - Monitor system resources and performance\n");
            report.append("  - Check for recent configuration changes\n");
            report.append("  - Review application logs for patterns\n");
            report.append("  - Consider temporary rate limiting if needed\n\n");
        }
        
        // General recommendations
        long totalErrors = errorCounts.values().stream().mapToLong(Long::longValue).sum();
        if (totalErrors > 50) {
            report.append("🛠️  General System Maintenance:\n");
            report.append("  - Consider error rate reduction strategies\n");
            report.append("  - Review error handling and recovery mechanisms\n");
            report.append("  - Implement additional validation where needed\n");
            report.append("  - Monitor error trends over time\n\n");
        }
    }
    
    /**
     * Assesses overall system health based on error patterns.
     */
    private static String assessSystemHealth(Map<String, Long> errorCounts) {
        if (errorCounts.isEmpty()) {
            return "EXCELLENT";
        }
        
        long totalErrors = errorCounts.values().stream().mapToLong(Long::longValue).sum();
        long criticalErrors = errorCounts.entrySet().stream()
            .filter(entry -> entry.getKey().toLowerCase().contains("critical") || 
                           entry.getKey().toLowerCase().contains("fatal"))
            .mapToLong(Map.Entry::getValue)
            .sum();
        
        if (criticalErrors > 0) {
            return "CRITICAL";
        } else if (totalErrors > 100) {
            return "POOR";
        } else if (totalErrors > 50) {
            return "WARNING";
        } else if (totalErrors > 10) {
            return "FAIR";
        } else {
            return "GOOD";
        }
    }
    
    /**
     * Provides specific recommendations based on error type.
     */
    private static String getErrorRecommendation(String errorType) {
        String lowerErrorType = errorType.toLowerCase();
        
        if (lowerErrorType.contains("nullpointer")) {
            return "Add null checks and defensive programming practices";
        } else if (lowerErrorType.contains("network") || lowerErrorType.contains("connection")) {
            return "Check network connectivity and timeout configurations";
        } else if (lowerErrorType.contains("data") || lowerErrorType.contains("nbt")) {
            return "Validate data integrity and serialization/deserialization logic";
        } else if (lowerErrorType.contains("ui") || lowerErrorType.contains("screen")) {
            return "Review UI initialization and state management";
        } else if (lowerErrorType.contains("config")) {
            return "Validate configuration files and default value handling";
        } else if (lowerErrorType.contains("town")) {
            return "Review town state management and validation logic";
        } else if (lowerErrorType.contains("platform")) {
            return "Check platform validation and coordinate calculations";
        } else {
            return "Review error context and implement appropriate handling";
        }
    }
    
    /**
     * Exports error report to a formatted string suitable for file output.
     */
    public static String exportToFile(String reportType) {
        String content;
        
        switch (reportType.toLowerCase()) {
            case "comprehensive":
                content = generateComprehensiveReport();
                break;
            case "summary":
                content = generateSummaryReport();
                break;
            case "json":
                content = generateJsonReport();
                break;
            default:
                content = "Unknown report type: " + reportType;
        }
        
        return content;
    }
}
