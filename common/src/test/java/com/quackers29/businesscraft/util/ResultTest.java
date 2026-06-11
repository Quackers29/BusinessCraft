package com.quackers29.businesscraft.util;

import com.quackers29.businesscraft.util.BCError;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-018: Result Type (Test + Docs Loop).
 *
 * Covers the pure functional Result<T, E> monad in Result.java:
 *   - success / failure factories + predicates + getters + optionals (incl. null handling)
 *   - map (success only), mapError (failure only), flatMap (success only, can flip to fail)
 *   - onSuccess / onFailure (side effects, fluent, correct branch only)
 *   - getOrElse (value or func, only fallback on fail)
 *   - fromOperation (happy path + exception captured as failure(Exception))
 *   - toString, equals (distinguishes success/failure even with "same" payload), hashCode
 *
 * All tests are pure logic. No Minecraft bootstrap, no platform, no config, no FS.
 * Documentation: vault/Core/Util/Result Type.md
 */
class ResultTest {

    // --- construction & basic access ---

    @Test
    void success_basic_isSuccessTrueAndValuePresent() {
        Result<String, String> r = Result.success("ok");
        assertTrue(r.isSuccess());
        assertFalse(r.isFailure());
        assertEquals("ok", r.getValue());
        assertNull(r.getError());
    }

    @Test
    void failure_basic_isFailureTrueAndErrorPresent() {
        Result<String, String> r = Result.failure("boom");
        assertFalse(r.isSuccess());
        assertTrue(r.isFailure());
        assertNull(r.getValue());
        assertEquals("boom", r.getError());
    }

    @Test
    void success_nullValue_isSuccessWithNull() {
        Result<String, String> r = Result.success(null);
        assertTrue(r.isSuccess());
        assertNull(r.getValue());
        assertEquals(Optional.empty(), r.getValueOptional());
    }

    @Test
    void failure_nullError_isFailureWithNull() {
        Result<String, String> r = Result.failure(null);
        assertTrue(r.isFailure());
        assertNull(r.getError());
        assertEquals(Optional.empty(), r.getErrorOptional());
    }

    // --- optional access ---

    @Test
    void getValueOptional_successYieldsPresent_successNullYieldsEmpty() {
        assertEquals(Optional.of("x"), Result.success("x").getValueOptional());
        assertEquals(Optional.empty(), Result.success((String)null).getValueOptional());
    }

    @Test
    void getErrorOptional_failureYieldsPresent_failureNullYieldsEmpty() {
        assertEquals(Optional.of("e"), Result.failure("e").getErrorOptional());
        assertEquals(Optional.empty(), Result.failure((String)null).getErrorOptional());
        assertEquals(Optional.empty(), Result.success("x").getErrorOptional());
    }

    // --- map (success path only) ---

    @Test
    void map_success_appliesAndWraps() {
        // "hello" -> length 5
        Result<Integer, String> r = Result.<String, String>success("hello").map(s -> s.length());
        assertTrue(r.isSuccess());
        assertEquals(5, r.getValue());
    }

    @Test
    void map_failure_isNoOpPreservesError() {
        Result<Integer, String> r = Result.<Integer, String>failure("err").map(i -> i + 1);
        assertTrue(r.isFailure());
        assertEquals("err", r.getError());
    }

    @Test
    void map_chainedOnSuccess_transformsStepwise() {
        // 10 -> "10" -> length 2
        Result<Integer, String> r = Result.<Integer, String>success(10)
                .map(Object::toString)
                .map(s -> s.length());
        assertEquals(2, r.getValue());
    }

    // --- mapError (failure path only) ---

    @Test
    void mapError_failure_appliesAndWrapsNewError() {
        Result<String, Integer> r = Result.<String, Integer>failure(42).mapError(e -> e * 2);
        assertTrue(r.isFailure());
        assertEquals(84, r.getError());
    }

    @Test
    void mapError_success_isNoOpPreservesValue() {
        Result<String, Integer> r = Result.<String, Integer>success("val").mapError(e -> (Integer) e + 1);
        assertTrue(r.isSuccess());
        assertEquals("val", r.getValue());
    }

    // --- flatMap (success path only; mapper decides next result) ---

    @Test
    void flatMap_success_mapperReturnsSuccess() {
        Result<Integer, String> r = Result.<Integer, String>success(3).flatMap(v -> Result.<Integer, String>success(v * 10));
        assertEquals(30, r.getValue());
    }

    @Test
    void flatMap_success_mapperReturnsFailure_propagatesFailure() {
        Result<Integer, String> r = Result.<Integer, String>success(3).flatMap(v -> Result.<Integer, String>failure("bad"));
        assertTrue(r.isFailure());
        assertEquals("bad", r.getError());
    }

    @Test
    void flatMap_failure_mapperNotCalled_preservesError() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<Integer, String> r = Result.<Integer, String>failure("orig")
                .flatMap(v -> { called.set(true); return Result.success(99); });
        assertTrue(r.isFailure());
        assertEquals("orig", r.getError());
        assertFalse(called.get());
    }

    // --- onSuccess / onFailure (side effects, fluent return this) ---

    @Test
    void onSuccess_success_callsConsumer_andReturnsSameInstance() {
        AtomicReference<String> seen = new AtomicReference<>();
        Result<String, String> orig = Result.success("hi");
        Result<String, String> ret = orig.onSuccess(seen::set);
        assertSame(orig, ret);
        assertEquals("hi", seen.get());
    }

    @Test
    void onSuccess_failure_doesNotCall_andReturnsSame() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<String, String> orig = Result.failure("no");
        Result<String, String> ret = orig.onSuccess(v -> called.set(true));
        assertSame(orig, ret);
        assertFalse(called.get());
    }

    @Test
    void onFailure_failure_callsConsumer_andReturnsSame() {
        AtomicReference<String> seen = new AtomicReference<>();
        Result<String, String> orig = Result.failure("oops");
        Result<String, String> ret = orig.onFailure(seen::set);
        assertSame(orig, ret);
        assertEquals("oops", seen.get());
    }

    @Test
    void onFailure_success_doesNotCall_andReturnsSame() {
        AtomicBoolean called = new AtomicBoolean(false);
        Result<String, String> orig = Result.success("ok");
        Result<String, String> ret = orig.onFailure(e -> called.set(true));
        assertSame(orig, ret);
        assertFalse(called.get());
    }

    // --- getOrElse (value or from error) ---

    @Test
    void getOrElse_default_successReturnsValue_ignoresDefault() {
        assertEquals("real", Result.success("real").getOrElse("def"));
        assertEquals("real", Result.success("real").getOrElse(err -> "fromErr"));
    }

    @Test
    void getOrElse_default_failureReturnsDefault() {
        assertEquals("def", Result.<String, String>failure("err").getOrElse("def"));
    }

    @Test
    void getOrElse_supplier_failure_callsSupplierWithError() {
        // "err" -> "ERR!"
        String out = Result.<String, String>failure("err")
                .getOrElse(e -> e.toUpperCase() + "!");
        assertEquals("ERR!", out);
    }

    @Test
    void getOrElse_supplier_success_doesNotCallSupplier() {
        AtomicBoolean called = new AtomicBoolean(false);
        String out = Result.success("val")
                .getOrElse(e -> { called.set(true); return "no"; });
        assertEquals("val", out);
        assertFalse(called.get());
    }

    // --- fromOperation ---

    @Test
    void fromOperation_happyPath_returnsSuccessWithResult() throws Exception {
        // 2 + 3 = 5
        Result<Integer, Exception> r = Result.fromOperation(() -> 2 + 3);
        assertTrue(r.isSuccess());
        assertEquals(5, r.getValue());
        assertTrue(r.getError() instanceof Exception == false); // no error
    }

    @Test
    void fromOperation_throws_returnsFailureWithTheException() {
        Exception ex = new IllegalStateException("boom");
        Result<String, Exception> r = Result.fromOperation(() -> { throw ex; });
        assertTrue(r.isFailure());
        assertSame(ex, r.getError()); // exact exception instance
    }

    @Test
    void fromOperation_returnsNullValue_isSuccessWithNull() {
        Result<String, Exception> r = Result.fromOperation(() -> null);
        assertTrue(r.isSuccess());
        assertNull(r.getValue());
    }

    // --- toString ---

    @Test
    void toString_success_andFailure_formatsAsSpecified() {
        assertEquals("Success(42)", Result.success(42).toString());
        assertEquals("Failure(oops)", Result.failure("oops").toString());
        assertEquals("Success(null)", Result.success(null).toString());
    }

    // --- equals / hashCode ---

    @Test
    void equals_sameSuccessValue_areEqual() {
        assertEquals(Result.success("a"), Result.success("a"));
        assertEquals(Result.success(7), Result.success(7));
    }

    @Test
    void equals_successVsFailure_evenWithMatchingPayload_areNotEqual() {
        // QUIRK (pinned): "a" success != "a" failure. This is correct for a Result type
        // (tag is part of identity) but worth noting because some call sites might
        // expect payload-only equality.
        assertNotEquals(Result.success("a"), Result.failure("a"));
    }

    @Test
    void equals_differentValues_orDifferentErrors_areNotEqual() {
        assertNotEquals(Result.success(1), Result.success(2));
        assertNotEquals(Result.failure("x"), Result.failure("y"));
    }

    @Test
    void equals_nullHandling_inValueAndError() {
        assertEquals(Result.success((String) null), Result.success((String) null));
        assertEquals(Result.failure((String) null), Result.failure((String) null));
        assertNotEquals(Result.success(null), Result.failure(null));
    }

    @Test
    void hashCode_consistentWithEquals_andVariesByBranch() {
        Result<String, String> s1 = Result.success("x");
        Result<String, String> s2 = Result.success("x");
        Result<String, String> f = Result.failure("x");
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1.hashCode(), f.hashCode());
    }

    // --- BCError integration (realistic E type) ---

    @Test
    void withBCError_successAndFailure_roundtripTypes() {
        BCError.Error err = BCError.ValidationError.required("name");
        Result<Void, BCError.Error> ok = Result.success(null);
        Result<Void, BCError.Error> bad = Result.failure(err);

        assertTrue(ok.isSuccess());
        assertTrue(bad.isFailure());
        assertSame(err, bad.getError());
        // mapError can change error type
        Result<Void, String> mapped = bad.mapError(BCError.Error::getMessage);
        assertEquals("name is required", mapped.getError());
    }
}
