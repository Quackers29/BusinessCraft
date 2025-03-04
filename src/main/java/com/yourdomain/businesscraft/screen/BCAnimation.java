package com.yourdomain.businesscraft.screen;

/**
 * Animation system for BusinessCraft UI transitions
 * Supports various animation types and easing functions
 */
public class BCAnimation {
    /**
     * Types of animations supported
     */
    public enum AnimationType {
        FADE,       // Fade in/out
        SLIDE_LEFT, // Slide from/to left
        SLIDE_RIGHT, // Slide from/to right
        SLIDE_UP,   // Slide from/to top
        SLIDE_DOWN, // Slide from/to bottom
        SCALE,      // Scale up/down
        NONE        // No animation
    }
    
    /**
     * Easing functions for animations
     */
    public enum EasingFunction {
        LINEAR,     // Constant speed
        EASE_IN,    // Start slow, end fast
        EASE_OUT,   // Start fast, end slow
        EASE_IN_OUT, // Start slow, middle fast, end slow
        BOUNCE,     // Bounce effect at the end
        ELASTIC     // Elastic/spring effect
    }
    
    private final AnimationType type;
    private final EasingFunction easingFunction;
    private final long duration; // Duration in milliseconds
    
    private boolean running = false;
    private boolean completed = false;
    private long startTime = 0;
    private long currentTime = 0;
    private float progress = 0.0f; // 0.0 to 1.0
    
    /**
     * Create a new animation with specified type, easing and duration
     */
    public BCAnimation(AnimationType type, EasingFunction easingFunction, long duration) {
        this.type = type;
        this.easingFunction = easingFunction;
        this.duration = duration;
    }
    
    /**
     * Start the animation
     */
    public void start() {
        this.running = true;
        this.completed = false;
        this.startTime = System.currentTimeMillis();
        this.currentTime = startTime;
        this.progress = 0.0f;
    }
    
    /**
     * Update the animation progress based on elapsed time
     */
    public void update() {
        if (!running) return;
        
        currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        
        if (elapsedTime >= duration) {
            // Animation completed
            progress = 1.0f;
            running = false;
            completed = true;
        } else {
            // Animation in progress
            progress = (float) elapsedTime / duration;
            
            // Apply easing function
            progress = applyEasing(progress);
        }
    }
    
    /**
     * Apply the easing function to the linear progress
     */
    private float applyEasing(float linearProgress) {
        switch (easingFunction) {
            case LINEAR:
                return linearProgress;
                
            case EASE_IN:
                return linearProgress * linearProgress;
                
            case EASE_OUT:
                return 1.0f - (1.0f - linearProgress) * (1.0f - linearProgress);
                
            case EASE_IN_OUT:
                return linearProgress < 0.5f
                        ? 2.0f * linearProgress * linearProgress
                        : 1.0f - (float)Math.pow(-2.0f * linearProgress + 2.0f, 2) / 2.0f;
                
            case BOUNCE:
                if (linearProgress < 1 / 2.75f) {
                    return 7.5625f * linearProgress * linearProgress;
                } else if (linearProgress < 2 / 2.75f) {
                    linearProgress -= 1.5f / 2.75f;
                    return 7.5625f * linearProgress * linearProgress + 0.75f;
                } else if (linearProgress < 2.5 / 2.75) {
                    linearProgress -= 2.25f / 2.75f;
                    return 7.5625f * linearProgress * linearProgress + 0.9375f;
                } else {
                    linearProgress -= 2.625f / 2.75f;
                    return 7.5625f * linearProgress * linearProgress + 0.984375f;
                }
                
            case ELASTIC:
                float period = 0.3f; 
                float amplitude = 1.0f;
                if (linearProgress == 0 || linearProgress == 1) return linearProgress;
                float s = period / (2 * (float)Math.PI) * (float)Math.asin(1 / amplitude);
                linearProgress = linearProgress - 1;
                return -(amplitude * (float)Math.pow(2, 10 * linearProgress) * 
                        (float)Math.sin((linearProgress - s) * (2 * (float)Math.PI) / period));
                
            default:
                return linearProgress;
        }
    }
    
    /**
     * Check if the animation is running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Check if the animation is completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Get the current progress (0.0 to 1.0)
     */
    public float getProgress() {
        return progress;
    }
    
    /**
     * Get the alpha value for fade animations
     */
    public float getAlpha() {
        if (type == AnimationType.FADE) {
            return progress;
        }
        return 1.0f;
    }
    
    /**
     * Get the X translation for slide animations
     */
    public float getTranslationX(int containerWidth) {
        switch (type) {
            case SLIDE_LEFT:
                return (1.0f - progress) * containerWidth;
            case SLIDE_RIGHT:
                return -(1.0f - progress) * containerWidth;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Get the Y translation for slide animations
     */
    public float getTranslationY(int containerHeight) {
        switch (type) {
            case SLIDE_UP:
                return (1.0f - progress) * containerHeight;
            case SLIDE_DOWN:
                return -(1.0f - progress) * containerHeight;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Get the scale factor for scale animations
     */
    public float getScale() {
        if (type == AnimationType.SCALE) {
            return 0.5f + progress * 0.5f;
        }
        return 1.0f;
    }
} 