package com.quackers29.businesscraft.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-026: Error Types (Test + Docs Loop).
 *
 * Covers the pure error taxonomy in BCError.java:
 *   - Base Error code + message + toString format
 *   - ValidationError: default code, required(), invalid()
 *   - TownError: notFound, alreadyExists, insufficientResources (with numbers), invalidDistance
 *   - UIError: modalCreationFailed, screenInitializationFailed, invalidScreenState
 *   - NetworkError, DataError, ConfigError factories with exact message templates
 *   - UnexpectedError + fromException (incl. null message case)
 *
 * All construction and formatting is pure Java string logic. No Minecraft bootstrap,
 * no platform access, no config mutation, no filesystem.
 * Documentation: vault/Core/Util/Error Types.md
 */
class BCErrorTest {

    // --- base behavior ---

    @Test
    void base_toString_isAlwaysCodeColonMessage() {
        BCError.ValidationError e = new BCError.ValidationError("CUSTOM_CODE", "something went wrong");
        // code + ": " + message
        assertEquals("CUSTOM_CODE: something went wrong", e.toString());
    }

    @Test
    void base_getters_returnExactlyWhatWasPassed() {
        BCError.TownError e = new BCError.TownError("TOWN_X", "detail here");
        assertEquals("TOWN_X", e.getCode());
        assertEquals("detail here", e.getMessage());
    }

    // --- ValidationError ---

    @Test
    void validation_required_formatsFieldIsRequired() {
        // "name is required"
        BCError.ValidationError e = BCError.ValidationError.required("name");
        assertEquals("VALIDATION_ERROR", e.getCode());
        assertEquals("name is required", e.getMessage());
    }

    @Test
    void validation_invalid_formatsFieldIsInvalidReason() {
        // "age is invalid: must be positive"
        BCError.ValidationError e = BCError.ValidationError.invalid("age", "must be positive");
        assertEquals("VALIDATION_ERROR", e.getCode());
        assertEquals("age is invalid: must be positive", e.getMessage());
    }

    @Test
    void validation_nullField_embedsLiteralNull() {
        // null concat produces "null is required"
        BCError.ValidationError e = BCError.ValidationError.required(null);
        assertEquals("null is required", e.getMessage());
    }

    // --- TownError ---

    @Test
    void town_notFound_formatsWithId() {
        // "TOWN_NOT_FOUND: Town not found: 11111111-1111-1111-1111-111111111111"
        BCError.TownError e = BCError.TownError.notFound("11111111-1111-1111-1111-111111111111");
        assertEquals("TOWN_NOT_FOUND", e.getCode());
        assertEquals("Town not found: 11111111-1111-1111-1111-111111111111", e.getMessage());
    }

    @Test
    void town_alreadyExists_formatsWithName() {
        // "TOWN_ALREADY_EXISTS: Town already exists: Riverside"
        BCError.TownError e = BCError.TownError.alreadyExists("Riverside");
        assertEquals("TOWN_ALREADY_EXISTS", e.getCode());
        assertEquals("Town already exists: Riverside", e.getMessage());
    }

    @Test
    void town_insufficientResources_formatsWithNumbers() {
        // "INSUFFICIENT_RESOURCES: Insufficient emerald: required 50, available 12"
        BCError.TownError e = BCError.TownError.insufficientResources("emerald", 50, 12);
        assertEquals("INSUFFICIENT_RESOURCES", e.getCode());
        assertEquals("Insufficient emerald: required 50, available 12", e.getMessage());
    }

    @Test
    void town_insufficientResources_negativeAndZero_numbersAppearVerbatim() {
        // negative required/available are allowed by the factory and appear as-is
        BCError.TownError e = BCError.TownError.insufficientResources("wood", -3, 0);
        assertEquals("Insufficient wood: required -3, available 0", e.getMessage());
    }

    @Test
    void town_invalidDistance_formatsWithNumbers() {
        // "INVALID_DISTANCE: Town too close: distance 42, minimum required 100"
        BCError.TownError e = BCError.TownError.invalidDistance(42, 100);
        assertEquals("INVALID_DISTANCE", e.getCode());
        assertEquals("Town too close: distance 42, minimum required 100", e.getMessage());
    }

    @Test
    void town_nullId_embedsNullInMessage() {
        BCError.TownError e = BCError.TownError.notFound(null);
        assertEquals("Town not found: null", e.getMessage());
    }

    // --- UIError ---

    @Test
    void ui_modalCreationFailed_formatsTypeAndReason() {
        // "MODAL_CREATION_FAILED: Failed to create PaymentBoard modal: missing player"
        BCError.UIError e = BCError.UIError.modalCreationFailed("PaymentBoard", "missing player");
        assertEquals("MODAL_CREATION_FAILED", e.getCode());
        assertEquals("Failed to create PaymentBoard modal: missing player", e.getMessage());
    }

    @Test
    void ui_screenInitializationFailed_formatsLiteralMessage() {
        BCError.UIError e = BCError.UIError.screenInitializationFailed("TownInterface");
        assertEquals("SCREEN_INIT_FAILED", e.getCode());
        assertEquals("Failed to initialize TownInterface screen", e.getMessage());
    }

    @Test
    void ui_invalidScreenState_formatsExpectedAndActual() {
        // "INVALID_SCREEN_STATE: Invalid screen state: expected OPEN, got CLOSED"
        BCError.UIError e = BCError.UIError.invalidScreenState("OPEN", "CLOSED");
        assertEquals("INVALID_SCREEN_STATE", e.getCode());
        assertEquals("Invalid screen state: expected OPEN, got CLOSED", e.getMessage());
    }

    // --- NetworkError ---

    @Test
    void network_packetSerializationFailed_formatsPacketType() {
        BCError.NetworkError e = BCError.NetworkError.packetSerializationFailed("SetPlatformPathPacket");
        assertEquals("PACKET_SERIALIZATION_FAILED", e.getCode());
        assertEquals("Failed to serialize packet: SetPlatformPathPacket", e.getMessage());
    }

    @Test
    void network_invalidPacketData_formatsTypeAndReason() {
        BCError.NetworkError e = BCError.NetworkError.invalidPacketData("BidContractPacket", "amount out of range");
        assertEquals("INVALID_PACKET_DATA", e.getCode());
        assertEquals("Invalid BidContractPacket packet data: amount out of range", e.getMessage());
    }

    // --- DataError ---

    @Test
    void data_saveFailure_formatsTypeAndReason() {
        BCError.DataError e = BCError.DataError.saveFailure("VisitHistory", "disk full");
        assertEquals("SAVE_FAILURE", e.getCode());
        assertEquals("Failed to save VisitHistory: disk full", e.getMessage());
    }

    @Test
    void data_loadFailure_formatsTypeAndReason() {
        BCError.DataError e = BCError.DataError.loadFailure("TownData", "corrupt tag");
        assertEquals("LOAD_FAILURE", e.getCode());
        assertEquals("Failed to load TownData: corrupt tag", e.getMessage());
    }

    @Test
    void data_corruptedData_formatsTypeOnly() {
        BCError.DataError e = BCError.DataError.corruptedData("MarketPrices");
        assertEquals("CORRUPTED_DATA", e.getCode());
        assertEquals("Data corruption detected in MarketPrices", e.getMessage());
    }

    // --- ConfigError ---

    @Test
    void config_fileNotFound_formatsPath() {
        BCError.ConfigError e = BCError.ConfigError.fileNotFound("config/businesscraft/businesscraft.toml");
        assertEquals("CONFIG_FILE_NOT_FOUND", e.getCode());
        assertEquals("Configuration file not found: config/businesscraft/businesscraft.toml", e.getMessage());
    }

    @Test
    void config_invalidFormat_formatsPathAndReason() {
        BCError.ConfigError e = BCError.ConfigError.invalidFormat("businesscraft.toml", "bad number");
        assertEquals("INVALID_CONFIG_FORMAT", e.getCode());
        assertEquals("Invalid configuration format in businesscraft.toml: bad number", e.getMessage());
    }

    @Test
    void config_reloadFailed_formatsNameAndReason() {
        BCError.ConfigError e = BCError.ConfigError.reloadFailed("milestones", "parse error at line 3");
        assertEquals("CONFIG_RELOAD_FAILED", e.getCode());
        assertEquals("Failed to reload configuration milestones: parse error at line 3", e.getMessage());
    }

    // --- UnexpectedError ---

    @Test
    void unexpected_basicMessage() {
        BCError.UnexpectedError e = new BCError.UnexpectedError("something blew up");
        assertEquals("UNEXPECTED_ERROR", e.getCode());
        assertEquals("something blew up", e.getMessage());
    }

    @Test
    void unexpected_fromException_usesExceptionMessage() {
        Exception ex = new IllegalStateException("bad state during tick");
        BCError.UnexpectedError e = BCError.UnexpectedError.fromException(ex);
        assertEquals("UNEXPECTED_ERROR", e.getCode());
        assertEquals("Unexpected error: bad state during tick", e.getMessage());
    }

    @Test
    void unexpected_fromException_nullMessage_yieldsNullLiteral() {
        // pinning current behavior: e.getMessage() == null produces "Unexpected error: null"
        Exception ex = new RuntimeException((String) null);
        BCError.UnexpectedError e = BCError.UnexpectedError.fromException(ex);
        assertEquals("Unexpected error: null", e.getMessage());
    }

    @Test
    void unexpected_toString_includesCodePrefix() {
        BCError.UnexpectedError e = new BCError.UnexpectedError("boom");
        assertEquals("UNEXPECTED_ERROR: boom", e.toString());
    }
}
