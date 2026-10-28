package com.quackers29.businesscraft.client.showcase.testbed;

import com.quackers29.businesscraft.client.showcase.Showcase;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristType;
import com.quackers29.businesscraft.init.CommonModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spawns a tagged tourist, gives the local player a conductor clipboard, and captures the name tag.
 */
public final class ConductorClipboardCheck implements ShowcaseTestbedCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");

    private int ticks;
    private boolean done;
    private boolean pass = true;

    @Override
    public String name() {
        return "conductor-clipboard";
    }

    @Override
    public void begin(Minecraft minecraft, ShowcaseTestbedContext context) {
        ticks = 0;
        done = false;
        pass = true;
        Showcase.setActiveTownBlockPos(context.townBPos());
    }

    @Override
    public boolean tick(Minecraft minecraft, ShowcaseTestbedContext context) {
        ticks++;
        BlockPos townPos = context.townBPos();
        if (ticks == 1 && minecraft.getSingleplayerServer() != null && minecraft.player != null) {
            minecraft.getSingleplayerServer().execute(() -> setupWorld(minecraft, townPos));
        } else if (ticks >= 50) {
            Screenshot.grab(minecraft.gameDirectory, "showcase_testbed_conductor_clipboard.png",
                    minecraft.getMainRenderTarget(), message ->
                            LOGGER.info("Testbed conductor clipboard shot: {}", message.getString()));
            done = true;
            return true;
        }
        return false;
    }

    private static void setupWorld(Minecraft minecraft, BlockPos townPos) {
        ServerPlayer player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(minecraft.player.getUUID());
        if (player == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos spawn = townPos.offset(2, 0, 0);
        TouristEntity tourist = new TouristEntity(
                com.quackers29.businesscraft.init.CommonModEntityTypes.TOURIST.get(), level);
        tourist.applyDisplaySample("Oakvale", "Hillcrest", TouristType.SIGHTSEER, 900);
        tourist.setPersonalName("Morgan");
        tourist.setCustomNameVisible(true);
        tourist.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        level.addFreshEntity(tourist);
        if (CommonModItems.CONDUCTOR_CLIPBOARD != null) {
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(CommonModItems.CONDUCTOR_CLIPBOARD.get()));
        }
        player.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 2.5, 180f, 10f);
    }

    @Override
    public boolean passed() {
        return pass;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("screenshot", "showcase_testbed_conductor_clipboard.png");
        return d;
    }
}
