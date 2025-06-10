package com.yourdomain.businesscraft.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Optional;

/**
 * A Result type for explicit error handling without exceptions.
 * Provides a type-safe way to handle operations that may succeed or fail.
 * 
 * @param <T> The type of the success value
 * @param <E> The type of the error value
 */
public class Result<T, E> {
    private final T value;
    private final E error;
    private final boolean isSuccess;
    
    private Result(T value, E error, boolean isSuccess) {
        this.value = value;
        this.error = error;
        this.isSuccess = isSuccess;
    }
    
    /**
     * Creates a successful result with the given value.
     * 
     * @param value The success value
     * @param <T> The type of the success value
     * @param <E> The type of the error value
     * @return A successful Result
     */
    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null, true);
    }
    
    /**
     * Creates a failed result with the given error.
     * 
     * @param error The error value
     * @param <T> The type of the success value
     * @param <E> The type of the error value
     * @return A failed Result
     */
    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(null, error, false);
    }
    
    /**
     * Creates a Result from an operation that might throw an exception.
     * 
     * @param operation The operation to execute
     * @param <T> The type of the success value
     * @return A Result containing either the operation result or the exception
     */
    public static <T> Result<T, Exception> fromOperation(OperationSupplier<T> operation) {
        try {
            return success(operation.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Checks if this result represents a success.
     * 
     * @return true if successful, false if failed
     */
    public boolean isSuccess() {
        return isSuccess;
    }
    
    /**
     * Checks if this result represents a failure.
     * 
     * @return true if failed, false if successful
     */
    public boolean isFailure() {
        return !isSuccess;
    }
    
    /**
     * Gets the success value.
     * 
     * @return The success value, or null if this is a failure
     */
    public T getValue() {
        return value;
    }
    
    /**
     * Gets the error value.
     * 
     * @return The error value, or null if this is a success
     */
    public E getError() {
        return error;
    }
    
    /**
     * Gets the success value wrapped in an Optional.
     * 
     * @return Optional containing the value if successful, empty if failed
     */
    public Optional<T> getValueOptional() {
        return isSuccess ? Optional.ofNullable(value) : Optional.empty();
    }
    
    /**
     * Gets the error value wrapped in an Optional.
     * 
     * @return Optional containing the error if failed, empty if successful
     */
    public Optional<E> getErrorOptional() {
        return isFailure() ? Optional.ofNullable(error) : Optional.empty();
    }
    
    /**
     * Maps the success value to a new type.
     * 
     * @param mapper Function to transform the success value
     * @param <U> The new success value type
     * @return A new Result with the mapped value, or the original error
     */
    public <U> Result<U, E> map(Function<T, U> mapper) {
        if (isSuccess()) {
            return success(mapper.apply(value));
        } else {
            return failure(error);
        }
    }
    
    /**
     * Maps the error value to a new type.
     * 
     * @param mapper Function to transform the error value
     * @param <F> The new error value type
     * @return A new Result with the mapped error, or the original success value
     */
    public <F> Result<T, F> mapError(Function<E, F> mapper) {
        if (isFailure()) {
            return failure(mapper.apply(error));
        } else {
            return success(value);
        }
    }
    
    /**
     * Flat maps the success value to a new Result.
     * 
     * @param mapper Function to transform the success value to a new Result
     * @param <U> The new success value type
     * @return The mapped Result, or the original error
     */
    public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        if (isSuccess()) {
            return mapper.apply(value);
        } else {
            return failure(error);
        }
    }
    
    /**
     * Executes a consumer if the result is successful.
     * 
     * @param consumer The consumer to execute with the success value
     * @return This Result for chaining
     */
    public Result<T, E> onSuccess(Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(value);
        }
        return this;
    }
    
    /**
     * Executes a consumer if the result is a failure.
     * 
     * @param consumer The consumer to execute with the error value
     * @return This Result for chaining
     */
    public Result<T, E> onFailure(Consumer<E> consumer) {
        if (isFailure()) {
            consumer.accept(error);
        }
        return this;
    }
    
    /**
     * Gets the success value or returns a default value if failed.
     * 
     * @param defaultValue The default value to return on failure
     * @return The success value or the default value
     */
    public T getOrElse(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }
    
    /**
     * Gets the success value or computes a default value from the error.
     * 
     * @param defaultSupplier Function to compute default value from error
     * @return The success value or the computed default value
     */
    public T getOrElse(Function<E, T> defaultSupplier) {
        return isSuccess() ? value : defaultSupplier.apply(error);
    }
    
    @Override
    public String toString() {
        if (isSuccess()) {
            return "Success(" + value + ")";
        } else {
            return "Failure(" + error + ")";
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Result<?, ?> result = (Result<?, ?>) obj;
        return isSuccess == result.isSuccess &&
               java.util.Objects.equals(value, result.value) &&
               java.util.Objects.equals(error, result.error);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(value, error, isSuccess);
    }
    
    /**
     * Functional interface for operations that might throw exceptions.
     * 
     * @param <T> The return type
     */
    @FunctionalInterface
    public interface OperationSupplier<T> {
        T get() throws Exception;
    }
}