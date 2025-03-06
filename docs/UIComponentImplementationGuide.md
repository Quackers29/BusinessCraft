# UIComponent Implementation Guide

This guide provides instructions for properly implementing the `UIComponent` interface in the BusinessCraft mod.

## Required Methods

When creating a new component that implements `UIComponent`, you must implement all of the following methods:

```java
// Render the component at the given position
void render(GuiGraphics graphics, int mouseX, int mouseY, int offsetX, int offsetY);

// Set and get the visibility state
void setVisible(boolean visible);
boolean isVisible();

// Get the component's position
int getX();
int getY();

// Get the component's dimensions
int getWidth();
int getHeight();

// Update the component on each game tick
void tick();

// Initialize the component, registering any Buttons
void init(Consumer<Button> buttonConsumer);
```

## Handling Mouse Input

The `UIComponent` interface doesn't include a `mouseClicked` method in its definition. However, most components need to handle mouse clicks. To do this:

1. Implement a non-overridden method with this signature:
```java
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // Handle mouse clicks
    return clickHandled; // Return true if the click was handled
}
```

2. The `BCScreenBuilder` system will call this method when appropriate, even though it's not part of the interface.

3. Make sure to check visibility and perform bounds checking:
```java
if (!visible) {
    return false;
}

if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
    // Handle the click inside the component's bounds
    return true;
}

return false;
```

## Common Pitfalls

1. **Render Method Signature**: The correct signature is `render(GuiGraphics, int, int, int, int)` where the last two parameters are the X and Y offsets.

2. **Text Rendering**: Use `Minecraft.getInstance().font` for text rendering, not `graphics.getFont()`.

3. **Handling Mouse Events**: The coordinates passed to `mouseClicked` are in screen space, so you need to compare them with your component's absolute position.

4. **Button Registration**: The `init` method is used to register buttons with the screen. If your component doesn't use any vanilla buttons, provide an empty implementation.

## Example Implementation

Here's a minimal example of a properly implemented UIComponent:

```java
public class MyComponent implements UIComponent {
    private int width;
    private int height;
    private int x;
    private int y;
    private boolean visible = true;
    
    public MyComponent(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, int offsetX, int offsetY) {
        if (!visible) {
            return;
        }
        
        this.x = offsetX;
        this.y = offsetY;
        
        // Draw a simple rectangle
        graphics.fill(offsetX, offsetY, offsetX + width, offsetY + height, 0xFF808080);
    }
    
    // Note: This is NOT an @Override method - UIComponent doesn't define mouseClicked
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            // Handle the click
            return true;
        }
        
        return false;
    }
    
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public int getX() {
        return x;
    }
    
    @Override
    public int getY() {
        return y;
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public void tick() {
        // No tick logic needed
    }
    
    @Override
    public void init(Consumer<Button> buttonConsumer) {
        // No buttons to register
    }
}
```

## Using Components with the Screen Builder

When adding a component to a screen using the screen builder, it will automatically handle the positioning of components based on the layout manager:

```java
BCScreenBuilder.create(menu, inventory, title, 256, 204)
    .withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10))
    .addComponent(new MyComponent(100, 30))
    .build();
```

## Debugging Component Rendering

If your component is not rendering correctly:

1. Check that you're updating the `x` and `y` fields in the `render` method with the provided offsets.
2. Ensure that your rendering code uses the offsets for all drawing operations.
3. Verify that the `isVisible()` check is properly implemented and respected.

## Testing Input Handling

To test if your component is handling input correctly:

1. Make sure the click area is correctly defined based on the component's position and dimensions.
2. Add debug output or visual feedback to confirm when a click is detected.
3. Remember that the mouse coordinates are in screen space, not relative to the component. 