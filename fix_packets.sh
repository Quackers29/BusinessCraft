#!/bin/bash

# Careful script to fix network packet files
# Applies the established pattern to remaining files

echo "Applying network abstraction pattern to remaining packet files..."

# Get list of files that still need fixing
PACKET_FILES=$(find common/src/main/java -name "*.java" -exec grep -l "NetworkEvent\|NetworkHooks" {} \; | grep -v "OpenTownInterfacePacket")

echo "Found $(echo "$PACKET_FILES" | wc -l) files to update"

for file in $PACKET_FILES; do
    echo "Processing: $file"

    # Create backup
    cp "$file" "${file}.backup"

    # Add required imports if not present
    if ! grep -q "import java.util.function.Supplier;" "$file"; then
        sed -i '/^import.*$/a import java.util.function.Supplier;' "$file"
    fi
    if ! grep -q "import net.minecraftforge.network.NetworkEvent;" "$file"; then
        sed -i '/^import.*$/a import net.minecraftforge.network.NetworkEvent;' "$file"
    fi

    # Update handle method signature if needed
    sed -i 's/public boolean handle(Object context)/public boolean handle(Supplier<NetworkEvent.Context> ctx)/g' "$file"
    sed -i 's/public void handle(Object context)/public void handle(Supplier<NetworkEvent.Context> ctx)/g' "$file"

    # Update method body to use Forge pattern
    sed -i 's/handle(Object context)/handle(Supplier<NetworkEvent.Context> ctx)/g' "$file"

    # Add the context extraction and packet handling
    if grep -q "PlatformAccess\.getNetwork()" "$file"; then
        # If file uses PlatformAccess, convert to standard Forge pattern
        sed -i 's/PlatformAccess\.getNetwork()\.enqueueWork(context, /NetworkEvent.Context context = ctx.get();\n        context.enqueueWork(/g' "$file"
        sed -i 's/PlatformAccess\.getNetwork()\.getSender(context)/context.getSender()/g' "$file"
        sed -i 's/PlatformAccess\.getNetwork()\.setPacketHandled(context)/context.setPacketHandled(true)/g' "$file"
    else
        # If file doesn't use PlatformAccess, add the standard Forge pattern
        sed -i 's/public boolean handle(Supplier<NetworkEvent\.Context> ctx) {/public boolean handle(Supplier<NetworkEvent.Context> ctx) {\n        NetworkEvent.Context context = ctx.get();/' "$file"
        sed -i 's/public void handle(Supplier<NetworkEvent\.Context> ctx) {/public void handle(Supplier<NetworkEvent.Context> ctx) {\n        NetworkEvent.Context context = ctx.get();/' "$file"
    fi

    # Add context.setPacketHandled(true) if not present
    if ! grep -q "context\.setPacketHandled(true)" "$file" && grep -q "return true;" "$file"; then
        sed -i 's/return true;/context.setPacketHandled(true);\n        return true;/g' "$file"
    fi

    # Clean up any duplicate imports
    awk '!seen[$0]++' "$file" > "${file}.tmp"
    mv "${file}.tmp" "$file"

    echo "Completed: $file"
done

echo "Pattern application completed!"
