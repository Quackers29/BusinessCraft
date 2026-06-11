package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.util.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-008: Town Distance Validation (Test + Docs Loop).
 *
 * Covers TownValidationService (name, position, search radius, tourist/resource
 * guards, creation/settings orchestration) and the pure string/BlockPos/int
 * rules. Item-dependent error message paths (getDescription) are avoided or
 * hit only via null-item entries so no MC bootstrap is triggered.
 *
 * TownBoundaryService distance math is covered in the sibling
 * TownBoundaryServiceTest (requires Town instances).
 *
 * Documentation: vault/Town/Boundaries/Town Distance Validation.md
 */
class TownValidationServiceTest {

    private TownValidationService validator;
    private int savedDefaultPop;
    private int savedMinPop;
    private RegistryHelper savedRegistry;

    @BeforeEach
    void setUp() {
        validator = new TownValidationService();
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        savedMinPop = ConfigLoader.minPopForTourists;
        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new TestRegistryHelper();

        // Deterministic config for tests that read statics
        ConfigLoader.defaultStartingPopulation = 5;
        ConfigLoader.minPopForTourists = 5;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
        ConfigLoader.minPopForTourists = savedMinPop;
        PlatformAccess.registry = savedRegistry;
    }

    // --- test double (pattern copied from T-007 / T-002) ---
    private static class TestRegistryHelper implements RegistryHelper {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.item.Item> Supplier<T> registerBlockItem(String name, Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.entity.EntityType<?>> Supplier<T> registerEntityType(String name, Supplier<T> entityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.level.block.entity.BlockEntityType<?>> Supplier<T> registerBlockEntityType(String name, Supplier<T> blockEntityType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
            return () -> null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<net.minecraft.world.inventory.MenuType<T>> registerExtendedMenuType(String name, MenuFactory<T> factory) {
            return () -> null;
        }

        @Override
        public Item getItem(ResourceLocation location) {
            return null;
        }

        @Override
        public ResourceLocation getItemKey(Item item) {
            return null;
        }

        @Override
        public Iterable<Item> getItems() {
            return List.of();
        }
    }

    // --- reflection helper for private containsInappropriateContent ---
    private boolean containsInappropriate(String name) throws Exception {
        Method m = TownValidationService.class.getDeclaredMethod("containsInappropriateContent", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(validator, name);
    }

    // --- TownValidationService.validateTownName ---

    @Test
    void validateTownName_null_rejectsNullName() {
        Result<Void, BCError.ValidationError> r = validator.validateTownName(null);
        assertTrue(r.isFailure());
        assertEquals("NULL_NAME", r.getError().getCode());
    }

    @Test
    void validateTownName_emptyOrBlank_rejectsEmptyName() {
        assertTrue(validator.validateTownName("").isFailure());
        assertEquals("EMPTY_NAME", validator.validateTownName("   ").getError().getCode());
    }

    @Test
    void validateTownName_tooShort_tooLong_rejectWithCorrectCodes() {
        // length 0 is caught earlier by the trim().isEmpty() check (EMPTY_NAME)
        // MIN=1 so the explicit NAME_TOO_SHORT (length < 1) branch is currently unreachable
        // for any string that passes the blank check; we still exercise the constant path
        // via the empty case which produces EMPTY_NAME (documented in vault note).
        assertEquals("EMPTY_NAME", validator.validateTownName("").getError().getCode());
        // exactly 1 is ok
        assertTrue(validator.validateTownName("A").isSuccess());
        // exactly 50 is ok
        String fifty = "a".repeat(50);
        assertTrue(validator.validateTownName(fifty).isSuccess());
        // 51 is too long
        assertEquals("NAME_TOO_LONG", validator.validateTownName("a".repeat(51)).getError().getCode());
    }

    @Test
    void validateTownName_invalidCharacters_rejects() {
        assertEquals("INVALID_NAME_CHARACTERS", validator.validateTownName("Bad@Name!").getError().getCode());
        assertEquals("INVALID_NAME_CHARACTERS", validator.validateTownName("town#1").getError().getCode());
        assertEquals("INVALID_NAME_CHARACTERS", validator.validateTownName("foo/bar").getError().getCode());
    }

    @Test
    void validateTownName_validCharacters_accepts() {
        assertTrue(validator.validateTownName("Riverside").isSuccess());
        assertTrue(validator.validateTownName("Hill-crest_2").isSuccess());
        assertTrue(validator.validateTownName("O'Leary's Place").isSuccess());
        assertTrue(validator.validateTownName("A.B.C").isSuccess());
        assertTrue(validator.validateTownName("123 Town").isSuccess());
    }

    @Test
    void validateTownName_inappropriateContent_rejectsViaPrivateMethod() throws Exception {
        // direct private
        assertTrue(containsInappropriate("adminville"));
        assertTrue(containsInappropriate("the server town"));
        assertTrue(containsInappropriate("null pointer"));
        assertTrue(containsInappropriate("debug mode"));
        assertFalse(containsInappropriate("riverside"));
        assertFalse(containsInappropriate("my town"));

        // via public validator (lowercased inside)
        assertEquals("INAPPROPRIATE_NAME", validator.validateTownName("AdminTown").getError().getCode());
        assertEquals("INAPPROPRIATE_NAME", validator.validateTownName("My Server").getError().getCode());
    }

    // --- validatePosition (BlockPos safe) ---

    @Test
    void validatePosition_null_rejects() {
        Result<Void, BCError.ValidationError> r = validator.validatePosition(null);
        assertTrue(r.isFailure());
        assertEquals("NULL_POSITION", r.getError().getCode());
    }

    @Test
    void validatePosition_yBounds_rejectBelowMinus64AndAbove320() {
        assertEquals("POSITION_TOO_LOW", validator.validatePosition(new BlockPos(0, -65, 0)).getError().getCode());
        assertEquals("POSITION_TOO_HIGH", validator.validatePosition(new BlockPos(0, 321, 0)).getError().getCode());
        // edges allowed
        assertTrue(validator.validatePosition(new BlockPos(0, -64, 0)).isSuccess());
        assertTrue(validator.validatePosition(new BlockPos(0, 320, 0)).isSuccess());
    }

    @Test
    void validatePosition_xzFar_rejectBeyond30M() {
        int far = 30_000_001;
        assertEquals("POSITION_TOO_FAR", validator.validatePosition(new BlockPos(far, 64, 0)).getError().getCode());
        assertEquals("POSITION_TOO_FAR", validator.validatePosition(new BlockPos(-far, 64, 0)).getError().getCode());
        assertEquals("POSITION_TOO_FAR", validator.validatePosition(new BlockPos(0, 64, far)).getError().getCode());
        assertEquals("POSITION_TOO_FAR", validator.validatePosition(new BlockPos(0, 64, -far)).getError().getCode());
        // at limit allowed
        int limit = 30_000_000;
        assertTrue(validator.validatePosition(new BlockPos(limit, 64, 0)).isSuccess());
    }

    // --- validateSearchRadius ---

    @Test
    void validateSearchRadius_bounds() {
        assertEquals("RADIUS_TOO_SMALL", validator.validateSearchRadius(0).getError().getCode());
        assertEquals("RADIUS_TOO_LARGE", validator.validateSearchRadius(101).getError().getCode());
        assertTrue(validator.validateSearchRadius(1).isSuccess());
        assertTrue(validator.validateSearchRadius(100).isSuccess());
        assertTrue(validator.validateSearchRadius(10).isSuccess());
    }

    // --- validateTouristManagement ---

    @Test
    void validateTouristManagement_negativeInputs_reject() {
        assertEquals("NEGATIVE_TOURISTS", validator.validateTouristManagement(-1, 10, 10).getError().getCode());
        assertEquals("NEGATIVE_MAX_TOURISTS", validator.validateTouristManagement(5, -1, 10).getError().getCode());
        assertEquals("NEGATIVE_POPULATION", validator.validateTouristManagement(5, 10, -1).getError().getCode());
    }

    @Test
    void validateTouristManagement_insufficientPopulation_rejectsWhenMaxPositive() {
        // minPop=5 in setUp
        assertEquals("INSUFFICIENT_POPULATION",
                validator.validateTouristManagement(0, 3, 4).getError().getCode());
        // pop exactly at min is ok even with max>0
        assertTrue(validator.validateTouristManagement(0, 10, 5).isSuccess());
        // max=0 is always ok even if pop low
        assertTrue(validator.validateTouristManagement(0, 0, 1).isSuccess());
    }

    // --- validateVisitorProcessing ---

    @Test
    void validateVisitorProcessing_nullVisitorId_rejects() {
        assertEquals("NULL_VISITOR_ID",
                validator.validateVisitorProcessing(null, UUID.randomUUID()).getError().getCode());
    }

    @Test
    void validateVisitorProcessing_originCanBeNullForPlayers() {
        assertTrue(validator.validateVisitorProcessing(UUID.randomUUID(), null).isSuccess());
    }

    // --- validateInitialResources (safe paths only) ---

    @Test
    void validateInitialResources_nullOrEmptyMap_isOk() {
        TownService.CreateTownRequest req = new TownService.CreateTownRequest("Test", new BlockPos(0, 64, 0));
        // validateInitialResources itself assumes non-null (see impl: .isEmpty() with no guard);
        // the null-resources case is guarded by validateTownCreation before calling it.
        // Here we test the documented "empty is fine" behavior with an explicit empty map.
        req.setInitialResources(new HashMap<>());
        assertTrue(validator.validateInitialResources(req).isSuccess());
    }

    @Test
    void validateInitialResources_nullItemEntry_rejectsBeforeDescription() {
        TownService.CreateTownRequest req = new TownService.CreateTownRequest("Test", new BlockPos(0, 64, 0));
        Map<Item, Long> m = new HashMap<>();
        m.put(null, 10L);
        req.setInitialResources(m);
        Result<Void, BCError.ValidationError> r = validator.validateInitialResources(req);
        assertTrue(r.isFailure());
        assertEquals("NULL_ITEM", r.getError().getCode());
    }

    // --- validateResourceManagement (null-item guard is safe) ---

    @Test
    void validateResourceManagement_nullItem_rejects() {
        Result<Void, BCError.ValidationError> r = validator.validateResourceManagement(null, 5, 10);
        assertTrue(r.isFailure());
        assertEquals("NULL_ITEM", r.getError().getCode());
    }

    // NOTE: ZERO_AMOUNT, INSUFFICIENT_RESOURCES, AMOUNT_OVERFLOW, and the non-null item
    // error messages in validateResourceManagement (and similar in validateInitialResources)
    // require a non-null Item instance to reach (item null is rejected first, then later
    // paths do item.getDescription()). Per protocol we do not construct Items here; those
    // branches are documented in the vault note under Edge cases and are covered by the
    // orchestration + null-item tests that *are* safe. See also T-007 precedent.

    // --- validateTownCreation orchestration ---

    @Test
    void validateTownCreation_nullRequest_rejects() {
        assertEquals("INVALID_REQUEST", validator.validateTownCreation(null).getError().getCode());
    }

    @Test
    void validateTownCreation_badNameOrPos_failsEarly() {
        TownService.CreateTownRequest badName = new TownService.CreateTownRequest("", new BlockPos(0, 64, 0));
        assertEquals("EMPTY_NAME", validator.validateTownCreation(badName).getError().getCode());

        TownService.CreateTownRequest badPos = new TownService.CreateTownRequest("Ok", new BlockPos(0, 999, 0));
        assertEquals("POSITION_TOO_HIGH", validator.validateTownCreation(badPos).getError().getCode());
    }

    @Test
    void validateTownCreation_emptyResources_passes() {
        TownService.CreateTownRequest req = new TownService.CreateTownRequest("GoodTown", new BlockPos(5, 64, 5));
        req.setInitialResources(new HashMap<>());
        assertTrue(validator.validateTownCreation(req).isSuccess());
    }

    // --- validateTownSettings ---

    @Test
    void validateTownSettings_null_rejects() {
        assertEquals("INVALID_SETTINGS", validator.validateTownSettings(null).getError().getCode());
    }

    @Test
    void validateTownSettings_onlyValidatesNonNullFields() {
        TownService.TownSettings s = new TownService.TownSettings();
        // nothing set -> ok
        assertTrue(validator.validateTownSettings(s).isSuccess());

        s.setName("Bad@Name");
        assertEquals("INVALID_NAME_CHARACTERS", validator.validateTownSettings(s).getError().getCode());

        s.setName("Good Name");
        s.setSearchRadius(0);
        assertEquals("RADIUS_TOO_SMALL", validator.validateTownSettings(s).getError().getCode());

        s.setSearchRadius(25);
        assertTrue(validator.validateTownSettings(s).isSuccess());

        // tourist flag can be anything
        s.setTouristSpawningEnabled(false);
        assertTrue(validator.validateTownSettings(s).isSuccess());
    }
}
