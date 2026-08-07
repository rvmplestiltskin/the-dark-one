package com.rvmplestiltskin.darkone.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.UUID;

public class DarkOneState extends SavedData {

    private static final String DATA_NAME = "the_dark_one";

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

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (darkOneUuid != null) {
            tag.putUUID("DarkOne", darkOneUuid);
        }
        return tag;
    }

    public static DarkOneState load(CompoundTag tag, HolderLookup.Provider registries) {
        DarkOneState state = new DarkOneState();
        if (tag.hasUUID("DarkOne")) {
            state.darkOneUuid = tag.getUUID("DarkOne");
        }
        return state;
    }

    private static final SavedData.Factory<DarkOneState> FACTORY = new SavedData.Factory<>(
            DarkOneState::new,
            DarkOneState::load,
            null
    );

    public static DarkOneState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not loaded");
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_NAME);
    }
}
