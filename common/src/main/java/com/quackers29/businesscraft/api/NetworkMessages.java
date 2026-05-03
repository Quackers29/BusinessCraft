package com.quackers29.businesscraft.api;

public interface NetworkMessages {
    void sendToPlayer(Object message, Object player);

    void sendToAllPlayers(Object message);

    void sendToAllTrackingChunk(Object message, Object level, Object pos);

    void sendToServer(Object message);

}
