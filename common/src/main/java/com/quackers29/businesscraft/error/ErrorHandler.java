package com.quackers29.businesscraft.error;

import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

public class ErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);
    
    private static ErrorHandler instance;
    
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, ErrorMetrics> errorMetrics = new ConcurrentHashMap<>();
    
    private final Map<String, Function<Exception, Object>> recoveryStrategies = new ConcurrentHashMap<>();
    
    private boolean enableMetrics = true;
    private boolean enableDetailedLogging = true;
    private boolean enableStackTraceLogging = true;
    private int maxStackTraceDepth = 10;
    
    private ErrorHandler() {
        initializeDefaultRecoveryStrategies();
    }
    
    public static synchronized ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }
    
    public <T> Result<T, BCError.Error> handleException(Exception exception, String context, String componentName) {
        return handleException(exception, context, componentName, null);
    }
    
    @SuppressWarnings("unchecked")
    public <T> Result<T, BCError.Error> handleException(Exception exception, String context, String componentName, 
                                                       Function<Exception, T> recoveryStrategy) {
        
        ErrorContext errorContext = new ErrorContext(exception, context, componentName, Instant.now());
        
        logError(errorContext);
        
        if (enableMetrics) {
            updateErrorMetrics(errorContext);
        }
        
        if (recoveryStrategy != null) {
            try {
                T recoveredValue = recoveryStrategy.apply(exception);
                if (recoveredValue != null) {
                    LOGGER.info("Successfully recovered from error in {}: {}", componentName, context);
                    return Result.success(recoveredValue);
                }
            } catch (Exception recoveryException) {
                LOGGER.error("Recovery strategy failed for error in {}: {}", componentName, context, recoveryException);
            }
        }
        
        String errorType = exception.getClass().getSimpleName();
        Function<Exception, Object> defaultRecovery = recoveryStrategies.get(errorType);
        if (defaultRecovery != null) {
            try {
                T recoveredValue = (T) defaultRecovery.apply(exception);
                if (recoveredValue != null) {
                    LOGGER.info("Applied default recovery strategy for {} in {}", errorType, componentName);
                    return Result.success(recoveredValue);
                }
            } catch (Exception recoveryException) {
                LOGGER.error("Default recovery strategy failed for {} in {}", errorType, componentName, recoveryException);
            }
        }
        
        BCError.Error standardizedError = categorizeError(errorContext);
        return Result.failure(standardizedError);
    }
    
    public <T> Result<T, BCError.Error> tryExecute(Supplier<T> operation, String context, String componentName) {
        return tryExecute(operation, context, componentName, null);
    }
    
    public <T> Result<T, BCError.Error> tryExecute(Supplier<T> operation, String context, String componentName,
                                                  Function<Exception, T> recoveryStrategy) {
        try {
            T result = operation.get();
            return Result.success(result);
        } catch (Exception e) {
            return handleException(e, context, componentName, recoveryStrategy);
        }
    }
    
    private void logError(ErrorContext context) {
        String message = formatErrorMessage(context);
        
        if (isCriticalError(context.exception)) {
            LOGGER.error("CRITICAL ERROR [{}]: {}", context.componentName, message, context.exception);
        } else if (isRecoverableError(context.exception)) {
            LOGGER.warn("RECOVERABLE ERROR [{}]: {}", context.componentName, message);
            if (enableDetailedLogging) {
                LOGGER.debug("Error details for {}", context.componentName, context.exception);
            }
        } else {
            LOGGER.error("ERROR [{}]: {}", context.componentName, message);
            if (enableStackTraceLogging) {
                LOGGER.error("Stack trace for error in {}", context.componentName, context.exception);
            }
        }
        
        if (enableDetailedLogging && context.context != null && !context.context.isEmpty()) {
            LOGGER.debug("Error context [{}]: {}", context.componentName, context.context);
        }
    }
    
    private String formatErrorMessage(ErrorContext context) {
        StringBuilder message = new StringBuilder();
        message.append(context.exception.getClass().getSimpleName());
        
        if (context.exception.getMessage() != null) {
            message.append(": ").append(context.exception.getMessage());
        }
        
        if (context.context != null && !context.context.isEmpty()) {
            message.append(" (Context: ").append(context.context).append(")");
        }
        
        return message.toString();
    }
    
    private void updateErrorMetrics(ErrorContext context) {
        String errorType = context.exception.getClass().getSimpleName();
        String componentName = context.componentName;
        
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        errorCounts.computeIfAbsent(componentName + "." + errorType, k -> new AtomicLong(0)).incrementAndGet();
        
        String metricsKey = componentName + "." + errorType;
        errorMetrics.computeIfAbsent(metricsKey, k -> new ErrorMetrics())
                   .recordError(context.timestamp, context.exception);
    }
    
    private BCError.Error categorizeError(ErrorContext context) {
        Exception exception = context.exception;
        String componentName = context.componentName;
        String contextInfo = context.context;
        
        if (isNetworkError(exception)) {
            return new BCError.NetworkError("NETWORK_ERROR", 
                String.format("Network error in %s: %s", componentName, exception.getMessage()));
        }
        
        if (isDataError(exception)) {
            return new BCError.DataError("DATA_ERROR",
                String.format("Data error in %s: %s", componentName, exception.getMessage()));
        }
        
        if (isConfigurationError(exception)) {
            return new BCError.ConfigError("CONFIG_ERROR",
                String.format("Configuration error in %s: %s", componentName, exception.getMessage()));
        }
        
        if (isUIError(exception, componentName)) {
            return new BCError.UIError("UI_ERROR",
                String.format("UI error in %s: %s", componentName, exception.getMessage()));
        }
        
        if (isTownError(exception, componentName)) {
            return new BCError.TownError("TOWN_ERROR",
                String.format("Town operation error in %s: %s", componentName, exception.getMessage()));
        }
        
        if (isValidationError(exception)) {
            return new BCError.ValidationError("VALIDATION_ERROR",
                String.format("Validation error in %s: %s", componentName, exception.getMessage()));
        }
        
        return new BCError.UnexpectedError(
            String.format("Unexpected %s in %s: %s", 
                         exception.getClass().getSimpleName(), componentName, exception.getMessage()));
    }
    
    private void initializeDefaultRecoveryStrategies() {
        recoveryStrategies.put("NullPointerException", e -> {
            LOGGER.debug("Applying default recovery for NullPointerException");
            return null;
        });
        
        recoveryStrategies.put("NumberFormatException", e -> {
            LOGGER.debug("Applying default recovery for NumberFormatException");
            return 0;
        });
        
        recoveryStrategies.put("IndexOutOfBoundsException", e -> {
            LOGGER.debug("Applying default recovery for IndexOutOfBoundsException");
            return null;
        });
        
        recoveryStrategies.put("IOException", e -> {
            LOGGER.debug("Applying default recovery for IOException");
            return false;
        });
    }
    
    private boolean isCriticalError(Exception exception) {
        Throwable cause = exception.getCause();
        return (cause instanceof OutOfMemoryError) ||
               (cause instanceof StackOverflowError) ||
               (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("critical")) ||
               (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("outofmemory")) ||
               (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("stackoverflow"));
    }
    
    private boolean isRecoverableError(Exception exception) {
        return exception instanceof IllegalArgumentException ||
               exception instanceof NumberFormatException ||
               exception instanceof NullPointerException;
    }
    
    private boolean isNetworkError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        return className.contains("Network") ||
               className.contains("Connection") ||
               className.contains("Socket") ||
               className.contains("Packet");
    }
    
    private boolean isDataError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        return className.contains("NBT") ||
               className.contains("Data") ||
               className.contains("Storage") ||
               className.contains("Serialization") ||
               className.contains("Json");
    }
    
    private boolean isConfigurationError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        return className.contains("Config") ||
               message.contains("config") ||
               message.contains("property") ||
               message.contains("setting");
    }
    
    private boolean isUIError(Exception exception, String componentName) {
        String className = exception.getClass().getSimpleName();
        return className.contains("UI") ||
               className.contains("Screen") ||
               className.contains("Component") ||
               className.contains("Render") ||
               componentName.toLowerCase().contains("ui") ||
               componentName.toLowerCase().contains("screen");
    }
    
    private boolean isTownError(Exception exception, String componentName) {
        return componentName.toLowerCase().contains("town") ||
               componentName.toLowerCase().contains("tourist") ||
               componentName.toLowerCase().contains("population");
    }
    
    private boolean isValidationError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        return className.contains("Validation") ||
               className.contains("IllegalArgument") ||
               message.contains("invalid") ||
               message.contains("validation");
    }
    
    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public void setEnableDetailedLogging(boolean enableDetailedLogging) {
        this.enableDetailedLogging = enableDetailedLogging;
    }
    
    public void setEnableStackTraceLogging(boolean enableStackTraceLogging) {
        this.enableStackTraceLogging = enableStackTraceLogging;
    }
    
    public void setMaxStackTraceDepth(int maxStackTraceDepth) {
        this.maxStackTraceDepth = maxStackTraceDepth;
    }
    
    public void registerRecoveryStrategy(String exceptionType, Function<Exception, Object> strategy) {
        recoveryStrategies.put(exceptionType, strategy);
        LOGGER.debug("Registered recovery strategy for exception type: {}", exceptionType);
    }
    
    public Map<String, Long> getErrorCounts() {
        Map<String, Long> counts = new ConcurrentHashMap<>();
        errorCounts.forEach((key, value) -> counts.put(key, value.get()));
        return counts;
    }
    
    public Map<String, ErrorMetrics> getErrorMetrics() {
        return new ConcurrentHashMap<>(errorMetrics);
    }
    
    public void resetMetrics() {
        errorCounts.clear();
        errorMetrics.clear();
        LOGGER.info("Error metrics have been reset");
    }
    
    private static class ErrorContext {
        final Exception exception;
        final String context;
        final String componentName;
        final Instant timestamp;
        
        ErrorContext(Exception exception, String context, String componentName, Instant timestamp) {
            this.exception = exception;
            this.context = context;
            this.componentName = componentName;
            this.timestamp = timestamp;
        }
    }
}
