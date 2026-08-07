package com.rvmplestiltskin.darkone.state;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * Tracks who currently holds the Dark One power.
 * In-memory for 26.2 compatibility (persists for the lifetime of the server process).
 * Restart clears the Dark One until claimed again with /darkone set.
 */
public class DarkOneState {

    private static final DarkOneState INSTANCE = new DarkOneState();

    private UUID darkOneUuid = null;

    private DarkOneState() {}

    public UUID getDarkOneUuid() {
        return darkOneUuid;
    }

    public boolean isDarkOne(UUID uuid) {
        return darkOneUuid != null && darkOneUuid.equals(uuid);
    }

    public void setDarkOne(UUID uuid) {
        this.darkOneUuid = uuid;
    }

    public void clearDarkOne() {
        this.darkOneUuid = null;
    }

    public static DarkOneState get(MinecraftServer server) {
        return INSTANCE;
    }
}
