package com.quackers29.businesscraft.platform;

/**
 * The ticket machine's sign lamp. It shines only while the interface is linked and that platform
 * is enabled. A disabled platform, or an interface whose Town Centre is gone, stays dark.
 */
public final class PlatformLamp {
    public static final int LIT_LEVEL = 10;

    private PlatformLamp() {
    }

    public static boolean isLit(boolean interfaceActive, boolean platformEnabled) {
        return interfaceActive && platformEnabled;
    }

    public static int emission(boolean lit) {
        return lit ? LIT_LEVEL : 0;
    }
}
