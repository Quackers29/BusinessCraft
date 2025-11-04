package com.quackers29.businesscraft.ui.state.components;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.ui.state.TownInterfaceState;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A label component that binds to a state value and automatically
 * updates its display when the state changes.
 * 
 * @param <T> The type of state value to bind to
 */
public class StateLabelComponent<T> extends StateBindableComponent<T> {
    
    private int textColor = 0xFFFFFF;
    private boolean withShadow = true;
    private boolean centered = false;
    
    /**
     * Create a new state-bound label
     * 
     * @param width Width of the label
     * @param height Height of the label
     * @param state The state object
     * @param stateGetter Function to get value from state
     * @param formatter Function to format the value for display
     */
    public StateLabelComponent(
        int width, 
        int height, 
        TownInterfaceState state,
        Supplier<T> stateGetter,
        Function<T, String> formatter
    ) {
        super(width, height, state, stateGetter, null, formatter);
    }
    
    /**
     * Create a new state-bound label with a simple string value
     */
    public static StateLabelComponent<String> createForString(
        int width, 
        int height, 
        TownInterfaceState state,
        Supplier<String> stateGetter
    ) {
        return new StateLabelComponent<>(width, height, state, stateGetter, value -> value);
    }
    
    /**
     * Create a new state-bound label with an integer value
     */
    public static StateLabelComponent<Integer> createForInt(
        int width, 
        int height, 
        TownInterfaceState state,
        Supplier<Integer> stateGetter
    ) {
        return new StateLabelComponent<>(width, height, state, stateGetter, value -> String.valueOf(value));
    }
    
    /**
     * Create a new state-bound label with a boolean value
     */
    public static StateLabelComponent<Boolean> createForBoolean(
        int width, 
        int height, 
        TownInterfaceState state,
        Supplier<Boolean> stateGetter
    ) {
        return new StateLabelComponent<>(width, height, state, stateGetter, 
            value -> value ? "Enabled" : "Disabled");
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String text = formatValue();
        
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object fontObj = clientHelper.getFont();
            if (fontObj instanceof net.minecraft.client.gui.Font font) {
                if (centered) {
                    int textWidth = font.width(text);
                    int xPos = x + (width - textWidth) / 2;
                    guiGraphics.drawString(
                        font,
                        text,
                        xPos,
                        y + (height - 8) / 2,
                        textColor,
                        withShadow
                    );
                } else {
                    guiGraphics.drawString(
                        font,
                        text,
                        x,
                        y + (height - 8) / 2,
                        textColor,
                        withShadow
                    );
                }
            }
        }
    }
    
    /**
     * Set the text color for this label
     */
    public StateLabelComponent<T> withTextColor(int color) {
        this.textColor = color;
        return this;
    }
    
    /**
     * Set whether to render text with shadow
     */
    public StateLabelComponent<T> withShadow(boolean shadow) {
        this.withShadow = shadow;
        return this;
    }
    
    /**
     * Set whether to center the text horizontally
     */
    public StateLabelComponent<T> centered() {
        this.centered = true;
        return this;
    }
} 
