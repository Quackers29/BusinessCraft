# AI CONTEXT
ROOT: \\wsl.localhost\Ubuntu\home\az\project\BusinessCraft
WSL_PREFIX: true

## COMMANDS
BUILD_COMMON: wsl ./gradlew common:build
BUILD_ALL:    wsl ./gradlew build

## CRITICAL PATHS
PACKET_REGISTRY: common/src/main/java/com/quackers29/businesscraft/network/PacketRegistry.java
PACKETS_DIR:     common/src/main/java/com/quackers29/businesscraft/network/packets/
SCREENS_DIR:     common/src/main/java/com/quackers29/businesscraft/ui/screens/town/
BASE_SCREEN:     common/src/main/java/com/quackers29/businesscraft/ui/screens/BaseTownScreen.java
TOWN_ENTITY:     common/src/main/java/com/quackers29/businesscraft/block/entity/TownInterfaceEntity.java
TOWN_DATA:       common/src/main/java/com/quackers29/businesscraft/town/Town.java
CONFIG:          common/src/main/java/com/quackers29/businesscraft/config/ConfigLoader.java

## COMMAND REGISTRATION
FORGE:  forge/src/main/java/com/quackers29/businesscraft/forge/BusinessCraftForge.java -> onRegisterCommands
FABRIC: fabric/src/main/java/com/quackers29/businesscraft/fabric/event/FabricEventCallbackHandler.java -> registerServerEvents

## RULES & GOTCHAS
1. COMMANDS: Must register in BOTH Forge (Event) and Fabric (Callback).
2. FABRIC_UI: BaseTownScreen needs public getMinecraft() helper for inner classes.
3. PACKETS: Register in PacketRegistry.java.
4. WSL: Always use 'wsl' prefix for gradle commands.
