---
tags:
  - detail
  - core
---
# Error Types

**Breadcrumb**: Core > Util > Error Types
**TL;DR**: Standardized hierarchy of error types (Validation, Town, UI, Network, Data, Config, Unexpected) whose static factories build consistent "CODE: human-readable message" strings; used as the error payload in Result<T, E> returns and for diagnostics so failures are categorized and never rely on ad-hoc strings or exceptions for control flow.

## What it does
BusinessCraft operations (town placement, resource checks, config loading, UI actions, network packets, saves) need to report precise failures without throwing exceptions for expected cases or returning magic nulls. BCError provides a small taxonomy of error kinds plus convenience factories that assemble a stable machine code (for switching/logging) together with a ready-to-show message. These errors are the conventional E type for the Result monad used across the mod.

## How it works (process view)
- Code that detects a problem calls the appropriate static factory, e.g. `TownError.invalidDistance(calculated, minimum)` or `ValidationError.required("name")` or `ConfigError.fileNotFound(path)`.
- The factory returns a concrete Error subclass instance carrying the chosen code and the formatted message.
- The caller typically wraps it: `Result.failure( theError )`.
- Consumers inspect `error.getCode()`, `error.getMessage()`, or `error.toString()` (which is always "CODE: message").
- **Worked example**: A town placement validator computes distance 42 blocks when the configured minimum is 100. It does `return Result.failure(BCError.TownError.invalidDistance(42, 100));`. The message becomes exactly "Town too close: distance 42, minimum required 100". A UI layer can display the message; a logger can record the code for metrics.
- All construction is pure string formatting; no side effects, no registry lookups, no config reads at error creation time.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `BCError.Error` (abstract) | `common/src/main/java/com/quackers29/businesscraft/util/BCError.java` | Base carrier: protected ctor(code, message), getCode(), getMessage(), toString() == "CODE: message" |
| `BCError.ValidationError` | same | Validation problems; default code VALIDATION_ERROR; factories required(field), invalid(field, reason) |
| `BCError.TownError` | same | Town lifecycle / placement / economy problems; factories notFound(id), alreadyExists(name), insufficientResources(resource, req, avail), invalidDistance(dist, min) |
| `BCError.UIError` | same | Screen and modal failures; factories modalCreationFailed(type, reason), screenInitializationFailed(type), invalidScreenState(expected, actual) |
| `BCError.NetworkError` | same | Packet issues; factories packetSerializationFailed(type), invalidPacketData(type, reason) |
| `BCError.DataError` | same | Persistence problems; factories saveFailure(type, reason), loadFailure(type, reason), corruptedData(type) |
| `BCError.ConfigError` | same | TOML / config problems; factories fileNotFound(path), invalidFormat(path, reason), reloadFailed(name, reason) |
| `BCError.UnexpectedError` | same | Catch-all; ctor(message) + fromException(e) which prefixes "Unexpected error: " + e.getMessage() |

## Rules & formulas (exact)
All messages are built exactly as written in the factories (no i18n, no extra trimming):

- ValidationError.required(field) → message = field + " is required" (code defaults to VALIDATION_ERROR)
- ValidationError.invalid(field, reason) → message = field + " is invalid: " + reason
- TownError.notFound(townId) → "TOWN_NOT_FOUND: Town not found: " + townId
- TownError.alreadyExists(townName) → "TOWN_ALREADY_EXISTS: Town already exists: " + townName
- TownError.insufficientResources(resource, required, available) → "INSUFFICIENT_RESOURCES: Insufficient " + resource + ": required " + required + ", available " + available   (via String.format)
- TownError.invalidDistance(distance, minimum) → "INVALID_DISTANCE: Town too close: distance " + distance + ", minimum required " + minimum   (via String.format)
- UIError.modalCreationFailed(modalType, reason) → "MODAL_CREATION_FAILED: Failed to create " + modalType + " modal: " + reason   (via String.format)
- UIError.screenInitializationFailed(screenType) → "SCREEN_INIT_FAILED: Failed to initialize " + screenType + " screen"
- UIError.invalidScreenState(expected, actual) → "INVALID_SCREEN_STATE: Invalid screen state: expected " + expected + ", got " + actual   (via String.format)
- NetworkError.packetSerializationFailed(packetType) → "PACKET_SERIALIZATION_FAILED: Failed to serialize packet: " + packetType
- NetworkError.invalidPacketData(packetType, reason) → "INVALID_PACKET_DATA: Invalid " + packetType + " packet data: " + reason   (via String.format)
- DataError.saveFailure(dataType, reason) → "SAVE_FAILURE: Failed to save " + dataType + ": " + reason   (via String.format)
- DataError.loadFailure(dataType, reason) → "LOAD_FAILURE: Failed to load " + dataType + ": " + reason   (via String.format)
- DataError.corruptedData(dataType) → "CORRUPTED_DATA: Data corruption detected in " + dataType
- ConfigError.fileNotFound(filePath) → "CONFIG_FILE_NOT_FOUND: Configuration file not found: " + filePath
- ConfigError.invalidFormat(filePath, reason) → "INVALID_CONFIG_FORMAT: Invalid configuration format in " + filePath + ": " + reason   (via String.format)
- ConfigError.reloadFailed(configName, reason) → "CONFIG_RELOAD_FAILED: Failed to reload configuration " + configName + ": " + reason   (via String.format)
- UnexpectedError(message) → "UNEXPECTED_ERROR: " + message
- UnexpectedError.fromException(e) → "UNEXPECTED_ERROR: Unexpected error: " + e.getMessage()

toString() on any error is always code + ": " + message. Base ctor is protected; most subclasses expose both a (code, message) ctor and the typed factory.

## Edge cases & behaviors
- Null arguments: concatenation and String.format happily embed the literal "null". Example: TownError.notFound(null) yields message "Town not found: null".
- Empty strings: " is required", "Town already exists: ", "Invalid configuration format in : ..." are all possible and produced verbatim.
- Numeric edges: negative or zero distances/required/available counts are formatted as-is (e.g. "distance -5, minimum required 100" or "required 0, available 10").
- fromException with null message or null exception: "Unexpected error: null" (when e.getMessage() == null) or NPE on the factory call itself if e is null (call site responsibility).
- All errors are immutable (final fields set in ctor). No defensive copying of the message/code strings.
- No equals or hashCode overrides on Error or subclasses: two errors with identical code+message are not .equals unless they are the same instance (standard Object identity).
- The code is intended to be stable for logging/metrics; the message is intended for player/developer display. No runtime parsing of messages is performed by the error classes themselves.

## Test coverage
- Test file: `common/src/test/java/com/quackers29/businesscraft/util/BCErrorTest.java`
- Covered: every static factory across all seven categories (26 tests), both the (code,message) and convenience constructors where present, exact message text (hand-computed in comments), toString format, getCode/getMessage, null/empty/negative/zero edge values in messages, fromException happy and null-message cases (null-message quirk pinned). Pure logic only; no bootstrap.
- Not covered: actual usage sites (ErrorHandler, validators, config service, contract/town flows), internationalization of messages, custom subclasses outside the provided ones.

## Open questions
- Absence of equals/hashCode means error instances are not value-equal even when they carry identical payloads. Callers that need to compare errors must either use reference identity, compare code+message manually, or the Result layer's own equals (which does deep value compare of its E). This is a potential source of subtle bugs if someone writes `if (err1.equals(err2))`; pinned by tests if we add any.
- Messages are English-only string templates with no Component/translatable wrapper. This matches current practice but will need attention if/when a full localization pass happens.
- fromException always forces the generic "UNEXPECTED_ERROR" code even when the exception could be mapped to a more specific ConfigError or DataError; higher layers (ErrorHandler) are expected to do that categorization.
- Several factories accept free-form "reason" or "type" strings from callers; there is no validation or sanitization of those strings inside BCError.

## Related
- [[Core/Core Overview]]
- [[Core/Util/Result Type]] (T-018 — the main consumer; BCError.* types are the conventional E)
- [[Config/Configuration Loading]] (T-014 — returns Result with ConfigError)
- [[Town/Boundaries/Town Distance Validation]] (T-008 — returns Result carrying TownError.invalidDistance and ValidationError)
