package com.quackers29.businesscraft.ui.components.input;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.components.basic.BCButton;
import java.util.function.Supplier;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.components.basic.BCButton;

/**
 * Pure delegation wrapper around Minecraft's EditBox.
 * This implementation does absolutely no custom rendering or event modification.
 */
public class BCEditBoxComponent extends BCComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCEditBoxComponent.class);
    
    private final EditBox editBox;
    private final Consumer<String> onTextChanged;
    
    /**
     * Create a new edit box component
     */
    public BCEditBoxComponent(int width, int height, Supplier<String> textSupplier, Consumer<String> onTextChanged, int maxLength) {
        super(width, height);
        this.onTextChanged = onTextChanged;
        
        // Get font from ClientHelper
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            throw new IllegalStateException("ClientHelper not available");
        }
        Object fontObj = clientHelper.getFont();
        if (!(fontObj instanceof net.minecraft.client.gui.Font font)) {
            throw new IllegalStateException("Font not available");
        }
        
        // Create a standard vanilla EditBox
        this.editBox = new EditBox(
            font,
            0, 0,          
            width,         
            height,        
            Component.empty()
        );
        
        // Basic configuration
        this.editBox.setMaxLength(maxLength);
        this.editBox.setBordered(true);
        this.editBox.setTextColor(0xFFFFFF); 
        
        // Set initial value if provided
        if (textSupplier != null) {
            String initialText = textSupplier.get();
            if (initialText != null) {
                this.editBox.setValue(initialText);
            }
        }
        
        // Set responder
        this.editBox.setResponder(text -> {
            if (onTextChanged != null) {
                try {
                    onTextChanged.accept(text);
                } catch (Exception e) {
                    LOGGER.error("Error in text change handler", e);
                }
            }
        });
    }
    
    /**
     * Get the current text value
     */
    public String getText() {
        return this.editBox.getValue();
    }
    
    /**
     * Set the text value
     */
    public void setText(String text) {
        this.editBox.setValue(text != null ? text : "");
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver((int)mouseX, (int)mouseY) || !enabled || !visible) {
            return false;
        }
        
        boolean handled = this.editBox.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            this.editBox.setFocused(true);
            super.setFocused(true);
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Position the vanilla EditBox correctly
        this.editBox.setX(x);
        this.editBox.setY(y);
        
        // ONLY call vanilla rendering
        this.editBox.render(guiGraphics, mouseX, mouseY, 0);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.editBox.isFocused() && enabled && visible) {
            return this.editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.editBox.isFocused() && enabled && visible) {
            return this.editBox.charTyped(codePoint, modifiers);
        }
        return false;
    }
    
    @Override
    public void tick() {
        super.tick();
        if (visible) {
            this.editBox.tick();
        }
    }
    
    @Override
    public boolean isFocused() {
        return this.editBox.isFocused();
    }
    
    @Override
    public void setFocused(boolean focused) {
        this.editBox.setFocused(focused);
        super.setFocused(focused);
    }
    
    // Skip background color methods - don't allow any custom styling that could interfere
} 
