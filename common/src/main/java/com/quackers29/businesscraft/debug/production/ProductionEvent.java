package com.quackers29.businesscraft.debug.production;

/**
 * One completed change. Progress inside a cycle is not an event.
 */
public final class ProductionEvent {
    public static final String RECIPE_COMPLETED = "recipe_completed";
    public static final String RECIPE_STALLED = "recipe_stalled";
    public static final String RECIPE_RESUMED = "recipe_resumed";
    public static final String RESEARCH_STARTED = "research_started";
    public static final String RESEARCH_COMPLETED = "research_completed";
    public static final String UPGRADE_LEVEL_GAINED = "upgrade_level_gained";
    public static final String POPULATION_CHANGED = "population_changed";
    public static final String BORDER_CHANGED = "border_changed";
    public static final String WORK_UNITS_CHANGED = "work_units_changed";
    public static final String CONTRACT_LISTED = "contract_listed";
    public static final String CONTRACT_BID = "contract_bid";
    public static final String CONTRACT_WON = "contract_won";
    public static final String CONTRACT_COURIER_DISPATCHED = "contract_courier_dispatched";
    public static final String CONTRACT_DELIVERED = "contract_delivered";
    public static final String CONTRACT_EXPIRED = "contract_expired";
    public static final String CONTRACT_CANCELLED = "contract_cancelled";
    public static final String TOWN_CREATED = "town_created";
    public static final String STATE_SNAPSHOT = "state_snapshot";

    private final long gameTick;
    private final String townName;
    private final String townId;
    private final String eventType;
    private final String subjectId;
    private final Double amount;
    private final String details;
    private final String stock;

    public ProductionEvent(long gameTick, String townName, String townId, String eventType,
            String subjectId, Double amount, String details, String stock) {
        this.gameTick = gameTick;
        this.townName = townName == null ? "" : townName;
        this.townId = townId == null ? "" : townId;
        this.eventType = eventType == null ? "" : eventType;
        this.subjectId = subjectId == null ? "" : subjectId;
        this.amount = amount;
        this.details = details == null ? "" : details;
        this.stock = stock == null ? "" : stock;
    }

    public long gameTick() {
        return gameTick;
    }

    public String townName() {
        return townName;
    }

    public String townId() {
        return townId;
    }

    public String eventType() {
        return eventType;
    }

    public String subjectId() {
        return subjectId;
    }

    public Double amount() {
        return amount;
    }

    public String details() {
        return details;
    }

    public String stock() {
        return stock;
    }
}
