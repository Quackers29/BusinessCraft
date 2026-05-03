package com.quackers29.businesscraft.api;

public interface NetworkHelper {
    <T> void registerMessage(int index, Class<T> messageType, Object encoder, Object decoder);

    void sendToPlayer(Object message, Object player);

    void sendToAllPlayers(Object message);

    void sendToServer(Object message);

    void sendToAllTrackingChunk(Object message, Object level, Object pos);

    boolean isClientSide();

    Object getCurrentContext();

    void enqueueWork(Object context, Runnable work);

    Object getSender(Object context);

    void setPacketHandled(Object context);

    void openScreen(Object player, Object menuProvider);

    void openScreen(Object player, Object menuProvider, Object blockPos);
}
