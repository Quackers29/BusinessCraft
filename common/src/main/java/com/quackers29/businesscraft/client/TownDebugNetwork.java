package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.debug.RequestTownDataPacket;

/**
 * Handles network communication for the town debug overlay
 * Now uses PlatformAccess instead of a separate network channel
 */
public class TownDebugNetwork {
    /**
     * Client-side method to request town data from the server
     */
    public static void requestTownData() {
        RequestTownDataPacket packet = new RequestTownDataPacket();
        PlatformAccess.getNetwork().sendToServer(packet);
    }
} 
