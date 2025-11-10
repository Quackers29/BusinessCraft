package com.quackers29.businesscraft.fabric.client;

import com.quackers29.businesscraft.fabric.menu.FabricTownInterfaceMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric-specific screen for the Town Interface.
 * This is a minimal implementation that extends HandledScreen (Fabric's equivalent of AbstractContainerScreen).
 * 
 * TODO: Port full UI functionality from TownInterfaceScreen/BaseTownScreen
 */
@Environment(EnvType.CLIENT)
public class FabricTownInterfaceScreen extends HandledScreen<FabricTownInterfaceMenu> {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/FabricTownInterfaceScreen");
    
    public FabricTownInterfaceScreen(FabricTownInterfaceMenu handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 256;
        this.backgroundHeight = 204;
        this.playerInventoryTitleY = 300; // Hide player inventory label
    }
    
    @Override
    protected void init() {
        super.init();
        LOGGER.info("FabricTownInterfaceScreen initialized");
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        
        // Draw basic town information
        // Cast handler to FabricTownInterfaceMenu (it's guaranteed to be this type due to generic parameter)
        FabricTownInterfaceMenu menu = (FabricTownInterfaceMenu) this.handler;
        if (menu != null) {
            String townName = menu.getTownName();
            int population = menu.getTownPopulation();
            
            // Draw town name
            if (townName != null && !townName.isEmpty()) {
                context.drawText(this.textRenderer, "Town: " + townName, this.x + 10, this.y + 10, 0xFFFFFF, false);
            }
            
            // Draw population
            context.drawText(this.textRenderer, "Population: " + population, this.x + 10, this.y + 25, 0xFFFFFF, false);
            
            // Draw search radius
            int searchRadius = menu.getSearchRadius();
            context.drawText(this.textRenderer, "Search Radius: " + searchRadius, this.x + 10, this.y + 40, 0xFFFFFF, false);
        }
    }
    
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Draw a simple dark background
        context.fill(this.x, this.y, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0xC0101010);
        
        // Draw border
        context.fill(this.x, this.y, this.x + this.backgroundWidth, this.y + 1, 0xFF404040);
        context.fill(this.x, this.y + this.backgroundHeight - 1, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0xFF404040);
        context.fill(this.x, this.y, this.x + 1, this.y + this.backgroundHeight, 0xFF404040);
        context.fill(this.x + this.backgroundWidth - 1, this.y, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0xFF404040);
    }
}

