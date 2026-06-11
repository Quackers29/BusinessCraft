---
tags:
  - detail
  - core
---
# Result Type

**Breadcrumb**: Core > Util > Result Type
**TL;DR**: Generic `Result<T, E>` explicit success/failure carrier (no exceptions for control flow): `success(value)` or `failure(error)`, with `map`/`flatMap`/`mapError` transforms, side-effect `onSuccess`/`onFailure`, dual `getOrElse`, `fromOperation` wrapper, and value-based equals/hash/toString.

## What it does
Provides a type-safe way for methods to return either a successful value or a typed error without using exceptions for normal control flow or nullable "magic" returns. Callers must explicitly handle both cases via isSuccess/isFailure or the transform methods. Used throughout config loading, town validation, contract flows, and error handling layers so failures are visible in the type system and can be transformed or recovered without try/catch sprawl.

## How it works (process view)
- A producer calls `Result.success(theValue)` on happy path or `Result.failure(someError)` on failure (often a BCError.Error subtype like ValidationError or ConfigError).
- Consumers inspect with `isSuccess()` / `isFailure()`, pull with `getValue()`/`getError()`, or (preferred) chain transforms: `map` to change the success payload, `flatMap` to sequence another fallible step, `mapError` to adapt the error type, `getOrElse(default)` or `getOrElse(err -> computeDefault(err))` to extract a value.
- `fromOperation(op)` runs a lambda that may throw and turns the outcome into success(result) or failure(theException).
- Side effects via `onSuccess(v -> ...)` / `onFailure(e -> ...)` still return the original Result for further chaining.
- **Worked example**: A config registration method returns `Result.success(null)` when the watcher is set up, or `Result.failure(new BCError.ConfigError("FILE_NOT_FOUND", "no such path"))` when the file is missing. The caller can do `result.mapError(e -> new UIError(...)).onFailure(e -> log(e))` without a try block at the call site.
- All operations that stay on the failure branch never invoke success mappers (and vice versa); null values and null errors are preserved (wrapped in Optional via ofNullable).

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `Result<T, E>` | `common/src/main/java/com/quackers29/businesscraft/util/Result.java` | Generic carrier. Private ctor; public static success/failure/fromOperation factories; instance map/flatMap/mapError/on*/getOrElse accessors + equals/hash/toString. |
| `Result.OperationSupplier<T>` | same (inner @FunctionalInterface) | Throwing supplier used only by fromOperation: `T get() throws Exception`. |
| `BCError.*` (ValidationError, TownError, ConfigError, ...) | `common/src/main/java/com/quackers29/businesscraft/util/BCError.java` | Common concrete E types (code + message); many static factories. Frequently used as the error type parameter. |

## Rules & formulas (exact)
Exact behavior as implemented in code (no reliance on comments):

```java
// Construction
Result<T, E> ok = Result.success(value);   // isSuccess=true, value=the arg (may be null), error=null
Result<T, E> bad = Result.failure(error);  // isSuccess=false, value=null, error=the arg (may be null)

// Predicates
ok.isSuccess() == true;  ok.isFailure() == false;
bad.isSuccess() == false; bad.isFailure() == true;

// Direct access (may return null)
ok.getValue() == value;   ok.getError() == null;
bad.getValue() == null;   bad.getError() == error;

// Optional access (nulls become empty)
ok.getValueOptional() == Optional.ofNullable(value);
bad.getValueOptional() == Optional.empty();
bad.getErrorOptional() == Optional.ofNullable(error);

// Transform only on success path (failure is identity for the error)
ok.map(mapper)  -> success(mapper.apply(value))   // mapper never called on failure
bad.map(mapper) -> failure(error)                  // mapper NOT invoked

// Error transform only on failure path
bad.mapError(mapper) -> failure(mapper.apply(error))
ok.mapError(mapper)  -> success(value)             // mapper NOT invoked

// Flat chain only on success; mapper itself returns a Result (can succeed or fail)
ok.flatMap(f) -> f.apply(value)   // can be success or failure result
bad.flatMap(f) -> failure(error)  // f NOT invoked

// Side-effect hooks (always return this for chaining)
ok.onSuccess(c).onFailure(c2) -> same ok instance; c was called, c2 was not
bad.onSuccess(c).onFailure(c2) -> same bad instance; c was not called, c2 was called

// Extraction with fallback
ok.getOrElse(defaultVal) -> value          // default ignored
bad.getOrElse(defaultVal) -> defaultVal
bad.getOrElse(err -> compute(err)) -> compute(error)  // supplier func only on fail
ok.getOrElse(err -> ...) -> value                     // func NOT invoked

// Wrapper that turns throwing code into Result (catches Exception only)
Result.fromOperation(() -> risky()) -> success(riskyResult) or failure(theThrownException)
```

- Order of operations and short-circuiting are exactly as the if (isSuccess) branches show; no exceptions are thrown by the Result methods themselves for normal use (NPE only if you pass null mapper etc. to map — standard Java).
- toString: "Success(" + value + ")" or "Failure(" + error + ")"
- equals: type match + isSuccess flag + Objects.equals on both value and error fields.
- hashCode: Objects.hash(value, error, isSuccess)

## Edge cases & behaviors
- `success(null)` is valid: isSuccess true, getValue()==null, getValueOptional().isEmpty()==true, map etc. still work (mapper receives null).
- `failure(null)` is valid: isFailure true, getError()==null, toString "Failure(null)".
- Chaining a success into flatMap that returns failure propagates the failure; subsequent maps on the chain see failure.
- `map`/`flatMap` on failure are no-ops for the mapper and preserve the exact original error object (reference).
- `mapError` on success is no-op and preserves the original success value.
- `getOrElse(default)` on success returns the success value even if default is null or expensive; the Function variant is never called on success path.
- `fromOperation`: any Exception (including Runtime) becomes `failure(e)`; the returned Result's error type is always `Exception` (not a narrower type). If the op itself returns null, you get `success(null)`.
- equals distinguishes success vs failure even if value and error happen to be equal objects: `success("x").equals(failure("x")) == false`.
- equals and hash are consistent with each other (standard contract).
- No defensive copies; the contained value/error references are returned as-is (callers must not mutate if they want immutability).
- OperationSupplier is only a marker for fromOperation; direct use of the interface outside fromOperation is not required by the class.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/util/ResultTest.java`
- Covered: all factories (success, failure, fromOperation happy+throw paths), predicates, getters + optionals (including null cases), map / mapError / flatMap (happy, fail, chaining that flips to fail), onSuccess/onFailure (called or not, fluent return), both getOrElse forms, toString, equals (same/diff success/fail, nulls, cross-type), hashCode consistency.
- Not covered: actual callers' recovery strategies (those live in ErrorHandler/ConfigurationService), concurrent use, subclassing.
- Pure logic only; no Minecraft bootstrap, no platform, no filesystem, no config mutation required.

## Open questions
- `fromOperation` always boxes the raw `Exception`; callers that want domain errors must mapError afterward (or use the higher-level ErrorHandler.tryExecute which does categorization). This is by design for a low-level utility.
- `success(null)` and `failure(null)` are allowed and tested; whether any production site relies on distinguishing "success with absent value" vs "no result" is left to callers (Optional inside is the escape hatch).
- No `fold`, `orElseThrow`, or `stream` convenience methods (unlike some Result libs); current API is the minimal set that satisfies all existing call sites.
- Equals treats two successes with equal (via Objects.equals) values as equal even if the values are mutable objects that later change — standard for value objects.

## Related
- [[Core/Core Overview]]
- [[Core/Time/Time Display Formatting]] (T-017 — sibling pure utility)
- [[Config/Configuration Loading]] (T-014 — heavy user of `Result<Void, BCError.ConfigError>` for registration + reload)
- [[Town/Boundaries/Town Distance Validation]] (T-008 — validators return `Result` carrying `BCError.TownError` / `ValidationError`)
- [[Trade/Contracts/Auction Resolution]] (mentions future pure Result extraction)
