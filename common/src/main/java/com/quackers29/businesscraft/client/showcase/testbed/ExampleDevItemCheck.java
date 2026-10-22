package com.quackers29.businesscraft.client.showcase.testbed;

import com.quackers29.businesscraft.client.showcase.Showcase;
import com.quackers29.businesscraft.client.showcase.ShowcaseButtonIds;
import com.quackers29.businesscraft.client.showcase.ShowcaseNavigator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Template check for new dev features. Copy this class, register a name in
 * {@link ShowcaseTestbedCheckRegistry}, and add real UI or world assertions.
 */
public final class ExampleDevItemCheck implements ShowcaseTestbedCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");

    private int ticks;
    private boolean done;
    private boolean pass = true;

    @Override
    public String name() {
        return "example-dev-item";
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
        BlockPos active = context.townBPos();
        if (ticks == 1) {
            ShowcaseNavigator.runStep(minecraft, active, "open-town");
        } else if (ticks == 20) {
            ShowcaseNavigator.runStep(minecraft, active, "set-tab:resources");
        } else if (ticks >= 40) {
            String err = ShowcaseNavigator.runStep(minecraft, active,
                    "press-button:id:" + ShowcaseButtonIds.TRADE);
            pass = err == null;
            Screenshot.grab(minecraft.gameDirectory, "showcase_testbed_example_dev_item.png",
                    minecraft.getMainRenderTarget(), message ->
                            LOGGER.info("Testbed example shot: {}", message.getString()));
            done = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean passed() {
        return pass;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("note", "Replace with asserts for your dev item or screen");
        d.put("activeTown", Showcase.getActiveTownBlockPos().toShortString());
        return d;
    }
}
