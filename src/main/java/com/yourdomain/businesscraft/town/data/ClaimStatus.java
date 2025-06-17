package com.yourdomain.businesscraft.town.data;

/**
 * Represents the claim status of a reward entry in the payment board
 */
public enum ClaimStatus {
    /**
     * The reward has not been claimed yet and is available for claiming
     */
    UNCLAIMED,
    
    /**
     * The reward has been successfully claimed by a player
     */
    CLAIMED,
    
    /**
     * The reward has expired and is no longer available for claiming
     */
    EXPIRED
}