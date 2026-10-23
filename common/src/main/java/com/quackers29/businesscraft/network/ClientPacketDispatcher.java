package com.quackers29.businesscraft.network;

/**
 * Forwards client-bound packet handling to a handler installed only on the
 * physical client. Dedicated servers leave this unset, so packet classes can
 * be loaded during registration without referencing client-only types.
 */
public final class ClientPacketDispatcher {
    @FunctionalInterface
    public interface Handler {
        void handle(Object packet);
    }

    private static volatile Handler handler;

    private ClientPacketDispatcher() {
    }

    public static void setHandler(Handler handler) {
        ClientPacketDispatcher.handler = handler;
    }

    public static void dispatch(Object packet) {
        Handler current = handler;
        if (current != null && packet != null) {
            current.handle(packet);
        }
    }
}
