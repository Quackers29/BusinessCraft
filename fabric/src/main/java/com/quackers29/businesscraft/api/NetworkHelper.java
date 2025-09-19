package com.quackers29.businesscraft.api;

/**
 * Interface for network operations
 */
public interface NetworkHelper {
    <T> void registerMessage(int index, Class<T> messageType, Object encoder, Object decoder);
    void sendToPlayer(Object message, Object player);
    void sendToAllPlayers(Object message);
    void sendToAllTrackingChunk(Object message, Object level, Object pos);
    void sendToServer(Object message);
    boolean isClientSide();
    Object getCurrentContext();
    void enqueueWork(Object context, Runnable work);
    Object getSender(Object context);
    void setPacketHandled(Object context);
    void openScreen(Object player, Object menuProvider);
}
