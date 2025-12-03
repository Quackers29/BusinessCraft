# Contract Courier Item Spawning Plan

## Overview
Currently, contract deliveries transfer resources directly between town inventories. To make courier contracts more engaging and immersive, we need to spawn physical items that couriers must transport. These items should contain contract information in NBT data and be visually distinct (enchanted) to prevent confusion with normal items.

## Requirements
- **Non-standard items**: Items with special properties/visuals
- **NBT contract data**: Store contract ID, resource info, destination, etc.
- **Visual distinction**: Should look different from regular items (curse enchantments)
- **Contract binding**: Items should be tied to specific contracts

## Implementation: Enchanted Standard Items

### Core Helper Class
Create a `ContractItemHelper` class to generate contract items:

```java
public class ContractItemHelper {
    public static ItemStack createContractItem(String resourceType, int quantity,
                                             UUID contractId, UUID destinationTownId,
                                             String destinationTownName) {
        Item baseItem = getBaseItemForResource(resourceType);
        ItemStack stack = new ItemStack(baseItem, quantity);

        // Add special enchantments for visual effect (purple glow)
        stack.enchant(Enchantments.BINDING_CURSE, 1); // Visual effect, can't remove
        stack.enchant(Enchantments.VANISHING_CURSE, 1); // Visual effect

        // Store contract data in NBT
        CompoundTag contractData = new CompoundTag();
        contractData.putUUID("contractId", contractId);
        contractData.putString("resourceType", resourceType);
        contractData.putInt("quantity", quantity);
        contractData.putUUID("destinationTownId", destinationTownId);
        contractData.putString("destinationTownName", destinationTownName);
        contractData.putLong("creationTime", System.currentTimeMillis());

        CompoundTag nbt = stack.getOrCreateTag();
        nbt.put("contractData", contractData);
        nbt.putBoolean("isContractItem", true);

        return stack;
    }

    private static Item getBaseItemForResource(String resourceType) {
        return switch (resourceType) {
            case "wood" -> Items.OAK_LOG;
            case "iron" -> Items.IRON_INGOT;
            case "coal" -> Items.COAL;
            default -> Items.STONE; // fallback
        };
    }
}
```

### Item Spawning Integration
Modify the contract acceptance logic to spawn items when a courier accepts a contract:

```java
// In AcceptContractPacket.handle() or similar
if (courierContract.getCourierId() == null) {
    courierContract.setCourierId(player.getUUID());

    // Spawn contract items at courier's location
    ItemStack contractItem = ContractItemHelper.createContractItem(
        courierContract.getResourceId(),
        courierContract.getQuantity(),
        courierContract.getId(),
        courierContract.getDestinationTownId(),
        // Need to get destination town name
        getTownName(courierContract.getDestinationTownId())
    );

    // Drop item at player's feet
    ItemEntity itemEntity = new ItemEntity(
        level,
        player.getX(),
        player.getY(),
        player.getZ(),
        contractItem
    );
    level.addFreshEntity(itemEntity);

    LOGGER.info("Spawned contract item for player {}: {} x{}",
        player.getName().getString(),
        courierContract.getResourceId(),
        courierContract.getQuantity());
}
```

### Visual Distinction
- **Curse Enchantments**: Binding Curse + Vanishing Curse provide purple glow effect
- **Unique Appearance**: Curse enchantments make items visually distinct from normal items
- **Tooltip Enhancement**: Could add custom hover text (future enhancement)

### Contract Item Validation
Create helper methods to validate contract items:

```java
public class ContractItemHelper {
    public static boolean isContractItem(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("isContractItem");
    }

    public static CompoundTag getContractData(ItemStack stack) {
        if (!isContractItem(stack)) return null;
        return stack.getTag().getCompound("contractData");
    }

    public static UUID getContractId(ItemStack stack) {
        CompoundTag data = getContractData(stack);
        return data != null ? data.getUUID("contractId") : null;
    }

    public static boolean matchesContract(ItemStack stack, UUID contractId) {
        UUID itemContractId = getContractId(stack);
        return itemContractId != null && itemContractId.equals(contractId);
    }
}
```

### Delivery Completion
When courier delivers to destination town, validate the contract item:

```java
public void completeContractDelivery(ServerPlayer courier, ItemStack contractItem, Town destinationTown) {
    CompoundTag contractData = ContractItemHelper.getContractData(contractItem);
    if (contractData == null) {
        LOGGER.warn("Invalid contract item delivered by {}", courier.getName().getString());
        return;
    }

    UUID contractId = contractData.getUUID("contractId");
    UUID expectedDestination = contractData.getUUID("destinationTownId");

    if (!destinationTown.getId().equals(expectedDestination)) {
        LOGGER.warn("Contract item delivered to wrong town by {}", courier.getName().getString());
        return;
    }

    // Find and complete the contract
    ContractBoard board = ContractBoard.get(courier.level());
    Contract contract = board.getContract(contractId);

    if (contract instanceof CourierContract courierContract) {
        // Transfer resources to destination town
        String resourceType = contractData.getString("resourceType");
        int quantity = contractData.getInt("quantity");
        Item resourceItem = ContractItemHelper.getBaseItemForResource(resourceType);

        destinationTown.addResource(resourceItem, quantity);
        courierContract.markCompleted();

        // Pay courier reward
        // ... reward logic ...

        // Remove the contract item
        contractItem.shrink(quantity);

        LOGGER.info("Contract {} completed by courier {}", contractId, courier.getName().getString());
    }
}
```

## Technical Considerations

### NBT Data Structure
```java
{
  "contractData": {
    "contractId": "uuid",
    "resourceType": "string",
    "quantity": int,
    "destinationTownId": "uuid",
    "destinationTownName": "string",
    "creationTime": long
  },
  "isContractItem": true,
  "Enchantments": [
    { "id": "minecraft:binding_curse", "lvl": 1 },
    { "id": "minecraft:vanishing_curse", "lvl": 1 }
  ]
}
```

### Delivery Validation
- Check item NBT matches contract requirements
- Validate courier has accepted contract
- Verify delivery location (town boundaries)
- Ensure item quantity matches contract

### Anti-Abuse Measures
- Items despawn if contract expires (future enhancement)
- Contract items cannot be duplicated legitimately
- Only the assigned courier can complete delivery

### Configuration Options
```toml
[contract_items]
enabled = true
use_curse_visuals = true
item_despawn_minutes = 1440  # 24 hours
allow_drop_on_death = false
```

## Future Enhancements
- Crafting prevention (separate implementation)
- Custom tooltips showing contract info
- Item despawning on contract expiration
- Custom model data for unique textures
- Sound/visual effects on delivery

This plan provides multiple approaches with increasing complexity and visual distinction, allowing implementation to start simple and enhance over time.
