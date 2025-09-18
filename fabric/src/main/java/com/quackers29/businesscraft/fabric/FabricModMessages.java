package com.quackers29.businesscraft.fabric;

/**
 * Simple test version of FabricModMessages
 */
public class FabricModMessages {
    public static void register() {
        // Simple test implementation
        System.out.println("FabricModMessages registered!");
    }

    public static void sendToPlayer(Object message, Object player) {
        System.out.println("sendToPlayer called");
    }

    public static void sendToAllPlayers(Object message) {
        System.out.println("sendToAllPlayers called");
    }

    public static void sendToAllTrackingChunk(Object message, Object level, Object pos) {
        System.out.println("sendToAllTrackingChunk called");
    }

    public static void sendToServer(Object message) {
        System.out.println("sendToServer called");
    }
}
