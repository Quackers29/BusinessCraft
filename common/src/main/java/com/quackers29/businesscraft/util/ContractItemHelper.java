package com.quackers29.businesscraft.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for creating and managing contract items with NBT data
 */
public class ContractItemHelper {

    /**
     * Creates a contract item with curse enchantments and NBT data
     * 
     * @param resourceType        The resource type (wood, iron, coal)
     * @param quantity            The quantity of items
     * @param contractId          The UUID of the contract
     * @param destinationTownId   The destination town's UUID
     * @param destinationTownName The destination town's name
     * @param sourceTownName      The source town's name
     * @return ItemStack with contract data and enchantments
     */
    public static ItemStack createContractItem(String resourceType, int quantity,
            UUID contractId, UUID destinationTownId,
            String destinationTownName, String sourceTownName) {
        Item baseItem = getBaseItemForResource(resourceType);
        ItemStack stack = new ItemStack(baseItem, quantity);

        // Add curse enchantments for visual distinction (purple glow)
        stack.enchant(Enchantments.VANISHING_CURSE, 1);

        // Store contract data in NBT
        CompoundTag contractData = new CompoundTag();
        contractData.putUUID("contractId", contractId);
        contractData.putString("resourceType", resourceType);
        contractData.putInt("quantity", quantity);
        contractData.putUUID("destinationTownId", destinationTownId);
        contractData.putString("destinationTownName", destinationTownName);
        contractData.putString("sourceTownName", sourceTownName);
        contractData.putLong("creationTime", System.currentTimeMillis());

        CompoundTag nbt = stack.getOrCreateTag();
        nbt.put("contractData", contractData);
        nbt.putBoolean("isContractItem", true);

        // Add custom lore
        addContractLore(stack, resourceType, quantity, destinationTownName, sourceTownName);

        return stack;
    }

    /**
     * Adds custom lore to a contract item showing contract details
     */
    private static void addContractLore(ItemStack stack, String resourceType, int quantity,
            String destinationTown, String sourceTown) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.literal("═══ Contract Delivery ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lore.add(Component.literal(""));
        lore.add(Component.literal("Pickup From: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(sourceTown).withStyle(ChatFormatting.WHITE)));
        lore.add(Component.literal("Deliver To: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(destinationTown).withStyle(ChatFormatting.WHITE)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("Cargo: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(quantity + "x " + capitalizeFirst(resourceType))
                        .withStyle(ChatFormatting.YELLOW)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("⚠ Special Delivery Item").withStyle(ChatFormatting.DARK_PURPLE,
                ChatFormatting.ITALIC));

        CompoundTag display = stack.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag loreTag = new net.minecraft.nbt.ListTag();

        for (Component line : lore) {
            loreTag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(line)));
        }

        display.put("Lore", loreTag);
    }

    /**
     * Gets the base item for a resource type
     */
    public static Item getBaseItemForResource(String resourceType) {
        return switch (resourceType.toLowerCase()) {
            case "wood" -> Items.OAK_LOG;
            case "iron" -> Items.IRON_INGOT;
            case "coal" -> Items.COAL;
            default -> Items.PAPER; // Fallback for unknown types
        };
    }

    /**
     * Checks if an ItemStack is a contract item
     */
    public static boolean isContractItem(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("isContractItem");
    }

    /**
     * Gets the contract data from an item
     */
    public static CompoundTag getContractData(ItemStack stack) {
        if (!isContractItem(stack))
            return null;
        return stack.getTag().getCompound("contractData");
    }

    /**
     * Gets the contract ID from an item
     */
    public static UUID getContractId(ItemStack stack) {
        CompoundTag data = getContractData(stack);
        return data != null && data.hasUUID("contractId") ? data.getUUID("contractId") : null;
    }

    /**
     * Checks if an item matches a specific contract
     */
    public static boolean matchesContract(ItemStack stack, UUID contractId) {
        UUID itemContractId = getContractId(stack);
        return itemContractId != null && itemContractId.equals(contractId);
    }

    /**
     * Capitalizes the first letter of a string
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
