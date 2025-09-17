#!/bin/bash

# Clean up unnecessary Forge imports from packet files
# Only remove imports that are no longer needed after proper abstraction

echo "Cleaning up unnecessary Forge imports from packet files..."

# Files that have NetworkEvent imports but correct handle methods
PACKET_FILES=(
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/OpenPaymentBoardPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/RequestTownMapDataPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/RefreshDestinationsPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/OpenTownInterfacePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/BufferStoragePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/PersonalStorageRequestPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/misc/PaymentResultPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/DeletePlatformPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/TownMapDataResponsePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/misc/BaseBlockEntityPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/SetPlatformPathCreationModePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/SetPathCreationModePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/CommunalStoragePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/BoundarySyncRequestPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/town/SetTownNamePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/town/ToggleTouristSpawningPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/ResetPlatformPathPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/SetPlatformDestinationPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/BufferSlotStorageResponsePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/SetPlatformPathPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/AddPlatformPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/SetSearchRadiusPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/BoundarySyncResponsePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/PaymentBoardClaimPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/PaymentBoardRequestPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/RefreshPlatformsPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/PersonalStorageResponsePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/RequestTownPlatformDataPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/CommunalStorageResponsePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/PlayerExitUIPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/platform/SetPlatformEnabledPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/BufferStorageResponsePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/PersonalStoragePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/PlatformVisualizationPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/TownPlatformDataResponsePacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/OpenDestinationsUIPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/storage/TradeResourcePacket.java"
)

for file in "${PACKET_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "Processing: $file"

        # Create backup
        cp "$file" "${file}.backup"

        # Remove NetworkEvent import (but keep it if file still uses NetworkEvent.Context in handle method)
        if grep -q "Supplier<NetworkEvent.Context>" "$file"; then
            echo "  Keeping NetworkEvent import (still used in handle method)"
        else
            sed -i '/^import net\.minecraftforge\.network\.NetworkEvent;$/d' "$file"
            echo "  Removed NetworkEvent import"
        fi

        # Clean up duplicate blank lines
        sed -i '/^$/N;/^\n$/d' "$file"
    fi
done

# Special handling for files with NetworkHooks
echo "Processing NetworkHooks files..."
HOOKS_FILES=(
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/OpenPaymentBoardPacket.java"
    "common/src/main/java/com/quackers29/businesscraft/network/packets/ui/OpenTownInterfacePacket.java"
)

for file in "${HOOKS_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "Processing hooks file: $file"
        # Keep NetworkHooks import as it's still used
        echo "  Keeping NetworkHooks import (still used)"
    fi
done

echo "Import cleanup completed!"
