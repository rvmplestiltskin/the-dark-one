package com.rvmplestiltskin.darkone.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/**
 * World-saved state for who currently holds the Dark One power.
 * Uses a simple approach compatible with 26.2 SavedData API.
 */
public class DarkOneState extends SavedData {

    private static final String DATA_NAME = "the_dark_one";

    // Fallback in-memory if SavedData factory differs across builds
    private static DarkOneState INSTANCE = new DarkOneState();

    private UUID darkOneUuid = null;

    public UUID getDarkOneUuid() {
        return darkOneUuid;
    }

    public boolean isDarkOne(UUID uuid) {
        return darkOneUuid != null && darkOneUuid.equals(uuid);
    }

    public void setDarkOne(UUID uuid) {
        this.darkOneUuid = uuid;
        setDirty();
    }

    public void clearDarkOne() {
        this.darkOneUuid = null;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (darkOneUuid != null) {
            tag.putString("DarkOne", darkOneUuid.toString());
        }
        return tag;
    }

    public static DarkOneState load(CompoundTag tag, HolderLookup.Provider registries) {
        DarkOneState state = new DarkOneState();
        if (tag.contains("DarkOne")) {
            try {
                state.darkOneUuid = UUID.fromString(tag.getString("DarkOne"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return state;
    }

    public static DarkOneState get(MinecraftServer server) {
        // Prefer overworld data storage when available; fall back to singleton
        try {
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                var storage = overworld.getDataStorage();
                // Try modern factory API; if it fails at runtime we still have INSTANCE
                DarkOneState loaded = storage.computeIfAbsent(
                        new SavedData.Factory<>(DarkOneState::new, DarkOneState::load),
                        DATA_NAME
                );
                if (loaded != null) {
                    INSTANCE = loaded;
                    return loaded;
                }
            }
        } catch (Throwable t) {
            // API mismatch — use in-memory instance for this session
        }
        return INSTANCE;
    }
}
