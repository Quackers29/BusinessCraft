package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

/**
 * Fabric block registration using direct Fabric API calls.
 */
public class FabricModBlocks {
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

    public static void register() {
        System.out.println("DEBUG: FabricModBlocks.register() called");
        if (registrationAttempted && registrationSuccessful) {
            System.out.println("DEBUG: Blocks already registered successfully, skipping");
            return; // Already registered successfully
        }

        registrationAttempted = true;
        System.out.println("DEBUG: FabricModBlocks.register() - Attempting block registration");

        // In Fabric, register blocks directly during mod initialization
        // Don't check availability - just register and handle any exceptions
        try {
            registerBlocks();
            registrationSuccessful = true;
            System.out.println("DEBUG: Block registration completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Block registration failed: " + e.getMessage());
            e.printStackTrace();
            // Try one more time after a short delay (Fabric might still be initializing)
            try {
                Thread.sleep(1000);
                registerBlocks();
                registrationSuccessful = true;
                System.out.println("DEBUG: Block registration succeeded on second attempt!");
            } catch (Exception e2) {
                System.err.println("ERROR: Block registration failed on second attempt: " + e2.getMessage());
                e2.printStackTrace();
                if (e2 instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Actual block registration logic - block with block entity
     */
    private static void registerBlocks() {
        try {
            // Create a block that implements EntityBlock to indicate it has a block entity
            class TownInterfaceBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
                public TownInterfaceBlock(Properties settings) {
                    super(settings);
                }

                @Override
                public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
                        net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
                    System.out.println("DEBUG: newBlockEntity called for block at " + pos);
                    if (FabricModBlockEntities.TOWN_INTERFACE_ENTITY_TYPE != null) {
                        return new TownInterfaceEntity(pos, state);
                    } else {
                        System.err.println("ERROR: Block entity type is null when creating block entity");
                        return null;
                    }
                }

                @Override
                public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state,
                        net.minecraft.world.level.Level world,
                        net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
                        net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {

                    if (world.isClientSide) {
                        // On client side, just return success to indicate interaction was handled
                        return net.minecraft.world.InteractionResult.SUCCESS;
                    }

                    // On server side, try to open the menu
                    try {
                        // Get the block entity
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
                        if (blockEntity != null) {
                            System.out.println(
                                    "DEBUG: Found block entity at " + pos + ": " + blockEntity.getClass().getName());

                            // Open the menu directly using PlatformAccess.openScreen() instead of using
                            // packets
                            try {
                                Class<?> platformAccessClass = Class
                                        .forName("com.quackers29.businesscraft.api.PlatformAccess");
                                Object network = platformAccessClass.getMethod("getNetwork").invoke(null);

                                // Create an ExtendedScreenHandlerFactory (Fabric) since we registered with
                                // registerExtended()
                                Class<?> extendedScreenHandlerFactoryClass = Class.forName(
                                        "net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory");
                                Object menuProvider = java.lang.reflect.Proxy.newProxyInstance(
                                        extendedScreenHandlerFactoryClass.getClassLoader(),
                                        new Class<?>[] { extendedScreenHandlerFactoryClass },
                                        new java.lang.reflect.InvocationHandler() {
                                            @Override
                                            public Object invoke(Object proxy, java.lang.reflect.Method method,
                                                    Object[] args) throws Throwable {
                                                String methodName = method.getName();
                                                if ("getDisplayName".equals(methodName)) {
                                                    // Return translated text (Fabric uses Text, but mapped to Component
                                                    // in Mojang mappings)
                                                    try {
                                                        Class<?> componentClass = Class
                                                                .forName("net.minecraft.network.chat.Component");
                                                        java.lang.reflect.Method translatableMethod = componentClass
                                                                .getMethod("translatable", String.class);
                                                        return translatableMethod.invoke(null,
                                                                "block.businesscraft.town_interface");
                                                    } catch (Exception e) {
                                                        throw new RuntimeException("Component class not found", e);
                                                    }
                                                } else if ("createMenu".equals(methodName)) {
                                                    // Create TownInterfaceMenu
                                                    try {
                                                        int windowId = (Integer) args[0];
                                                        Object inventory = args[1];

                                                        // Get the registered ScreenHandlerType
                                                        Class<?> menuTypeHelperClass = Class.forName(
                                                                "com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper");
                                                        java.lang.reflect.Method getMenuTypeMethod = menuTypeHelperClass
                                                                .getMethod("getTownInterfaceMenuTypeStatic");
                                                        Object menuType = getMenuTypeMethod.invoke(null);

                                                        if (menuType == null) {
                                                            throw new RuntimeException(
                                                                    "TownInterfaceMenuType not registered yet");
                                                        }

                                                        // Use the constructor that takes ScreenHandlerType
                                                        Class<?> menuClass = Class.forName(
                                                                "com.quackers29.businesscraft.menu.TownInterfaceMenu");
                                                        Class<?> inventoryClass = Class
                                                                .forName("net.minecraft.world.entity.player.Inventory");
                                                        Class<?> blockPosClass = Class
                                                                .forName("net.minecraft.core.BlockPos");

                                                        java.lang.reflect.Constructor<?> constructor = menuClass
                                                                .getConstructor(
                                                                        int.class,
                                                                        inventoryClass,
                                                                        blockPosClass);
                                                        return constructor.newInstance(windowId, inventory, pos);
                                                    } catch (Exception e) {
                                                        System.err.println("ERROR: Cannot create TownInterfaceMenu: "
                                                                + e.getMessage());
                                                        e.printStackTrace();
                                                        throw new RuntimeException("Failed to create TownInterfaceMenu",
                                                                e);
                                                    }
                                                } else if ("writeScreenOpeningData".equals(methodName)) {
                                                    // ExtendedScreenHandlerFactory requires this method to write
                                                    // BlockPos to PacketByteBuf
                                                    try {
                                                        Object player = args[0]; // ServerPlayerEntity
                                                        Object buf = args[1]; // FriendlyByteBuf
                                                        Class<?> blockPosClass = Class
                                                                .forName("net.minecraft.core.BlockPos");

                                                        // Find writeBlockPos method on the actual buffer object's class
                                                        java.lang.reflect.Method writeBlockPosMethod = null;
                                                        Class<?> bufClass = buf.getClass();

                                                        // Try to find writeBlockPos method
                                                        for (Class<?> c = bufClass; c != null
                                                                && writeBlockPosMethod == null; c = c.getSuperclass()) {
                                                            try {
                                                                writeBlockPosMethod = c.getMethod("writeBlockPos",
                                                                        blockPosClass);
                                                            } catch (NoSuchMethodException e) {
                                                                // Try interfaces
                                                                for (Class<?> iface : c.getInterfaces()) {
                                                                    try {
                                                                        writeBlockPosMethod = iface.getMethod(
                                                                                "writeBlockPos", blockPosClass);
                                                                        break;
                                                                    } catch (NoSuchMethodException e2) {
                                                                        continue;
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (writeBlockPosMethod == null) {
                                                            throw new RuntimeException(
                                                                    "Could not find writeBlockPos method on buffer class: "
                                                                            + bufClass.getName());
                                                        }

                                                        writeBlockPosMethod.invoke(buf, pos);
                                                        return null; // void method
                                                    } catch (Exception e) {
                                                        System.err.println(
                                                                "ERROR: Cannot write BlockPos to FriendlyByteBuf: "
                                                                        + e.getMessage());
                                                        e.printStackTrace();
                                                        throw new RuntimeException(
                                                                "Failed to write BlockPos to FriendlyByteBuf", e);
                                                    }
                                                }
                                                return null;
                                            }
                                        });

                                // Call PlatformAccess.getNetwork().openScreen(player, menuProvider, blockPos)
                                java.lang.reflect.Method openScreenMethod = network.getClass().getMethod("openScreen",
                                        Object.class, Object.class, Object.class);
                                openScreenMethod.invoke(network, player, menuProvider, pos);

                                System.out.println("DEBUG: Opened town interface menu directly via PlatformAccess");
                            } catch (Exception e) {
                                System.err.println("ERROR: Could not open menu via PlatformAccess: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("DEBUG: No block entity found at " + pos);
                        }

                        return net.minecraft.world.InteractionResult.SUCCESS;
                    } catch (Exception e) {
                        System.err.println("Error in block onUse: " + e.getMessage());
                        e.printStackTrace();
                        return net.minecraft.world.InteractionResult.FAIL;
                    }
                }
            }

            // Instantiate the block
            Block townInterfaceBlock = new TownInterfaceBlock(FabricBlockSettings.create()
                    .strength(3.0f, 3.0f)
                    .requiresTool());

            // Register the block using proper Fabric Registry.register method
            Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("businesscraft", "town_interface"),
                    townInterfaceBlock);

            // Create and register block item
            BlockItem townInterfaceBlockItem = new BlockItem(townInterfaceBlock, new FabricItemSettings());
            Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("businesscraft", "town_interface"),
                    townInterfaceBlockItem);

            System.out.println("DEBUG: Town Interface Block with BlockEntity registration completed successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to register block with block entity: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
