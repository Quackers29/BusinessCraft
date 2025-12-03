package com.quackers29.businesscraft.town.data;

/**
 * Represents the source of a reward entry in the payment board
 */
public enum RewardSource {
    /**
     * Reward from reaching distance milestones
     */
    MILESTONE("🏆", "Milestone Reward"),

    /**
     * Payment from tourist transportation
     */
    TOURIST_PAYMENT("🚂", "Tourist Payment"),

    /**
     * Bundled tourist arrival (fare + milestone rewards combined)
     */
    TOURIST_ARRIVAL("🚂🏆", "Tourist Arrival"),

    /**
     * Reward from trading activities
     */
    TRADE("💰", "Trade Profit"),

    /**
     * Reward from completed jobs
     */
    JOB_COMPLETION("⚒️", "Job Completed"),

    /**
     * Manual reward added by administrators
     */
    ADMIN_REWARD("👑", "Admin Reward"),

    /**
     * Other miscellaneous rewards
     */
    OTHER("📦", "Other"),

    /**
     * Items for courier pickup
     */
    COURIER_PICKUP("📦", "Courier Pickup");

    private final String icon;
    private final String displayName;

    RewardSource(String icon, String displayName) {
        this.icon = icon;
        this.displayName = displayName;
    }

    /**
     * Get the icon representation of this reward source
     * 
     * @return The icon string
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get the display name of this reward source
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
