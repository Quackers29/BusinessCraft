package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.CourierContract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.menu.ContractBoardMenu;
import com.quackers29.businesscraft.ui.screens.town.ContractBoardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet to sync active contracts from server to client.
 */
public class ContractSyncPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractSyncPacket.class);

    private final List<Contract> contracts;

    public ContractSyncPacket(List<Contract> contracts) {
        this.contracts = contracts;
    }

    public ContractSyncPacket(FriendlyByteBuf buf) {
        this.contracts = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String type = buf.readUtf();
            CompoundTag tag = buf.readNbt();
            Contract contract = null;

            if ("sell".equals(type)) {
                contract = new SellContract(tag);
            } else if ("courier".equals(type)) {
                contract = new CourierContract(tag);
            }

            if (contract != null) {
                this.contracts.add(contract);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(contracts.size());
        for (Contract c : contracts) {
            buf.writeUtf(c.getType());
            CompoundTag tag = new CompoundTag();
            c.save(tag);
            buf.writeNbt(tag);
        }
    }

    // Static methods for Forge network registration
    public static void encode(ContractSyncPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }

    public static ContractSyncPacket decode(FriendlyByteBuf buf) {
        return new ContractSyncPacket(buf);
    }

    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Client-side handling
            Player player = (Player) PlatformAccess.getClient().getPlayer();
            if (player != null && player.containerMenu instanceof ContractBoardMenu menu) {
                menu.setContracts(contracts);
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }

    // Client handler
    public void handleClient(Minecraft mc) {
        if (mc.screen instanceof ContractBoardScreen screen) {
            screen.updateContracts(contracts);
        }
    }
}
