package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.debug.RequestTownDataPacket;

public class TownDebugNetwork {
    public static void requestTownData() {
        PlatformAccess.getNetworkMessages().sendToServer(new RequestTownDataPacket());
    }
}
