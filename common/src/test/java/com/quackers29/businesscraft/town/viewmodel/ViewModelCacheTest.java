package com.quackers29.businesscraft.town.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewModelCacheTest {
    @Test
    void syncAllDirtyClearsDirtyButKeepsModelForLaterPlayers() {
        ViewModelCache cache = new ViewModelCache();
        String vm = "production";
        cache.update(String.class, vm);

        List<Object> sent = new ArrayList<>();
        cache.syncAllDirty(sent::add);
        assertEquals(1, sent.size());
        assertFalse(cache.isDirty(String.class));

        // A second dirty sweep sends nothing: a player arriving later needs the cached model.
        cache.syncAllDirty(sent::add);
        assertEquals(1, sent.size());
        assertSame(vm, cache.get(String.class));
    }
}
