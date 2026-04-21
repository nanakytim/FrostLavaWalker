package net.nanaky.frost_lava_walker.config;

public class ServerConfig {

    // Server-side
    public boolean lavaWalkerEnabled     = true;
    public int     baseRadius            = 1;
    public int     gildedInitialTicks    = 3;
    public int     blackstoneMainTicks   = 40;
    public int     gildedWarningTicks    = 15;
    public int     magmaShortTicks       = 10;
    public int     cooldownExtraTicks    = 5;

    // Derived — call after loading
    public int totalLifecycleTicks() {
        return gildedInitialTicks + blackstoneMainTicks + gildedWarningTicks + magmaShortTicks;
    }

    public int conversionCooldownTicks() {
        return totalLifecycleTicks() + cooldownExtraTicks;
    }
}