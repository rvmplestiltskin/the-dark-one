package com.rvmplestiltskin.darkone.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.UUID;

public class DarkOneState extends PersistentState {

    private UUID darkOneUuid = null;

    public UUID getDarkOneUuid() {
        return darkOneUuid;
    }

    public boolean isDarkOne(UUID uuid) {
        return darkOneUuid != null && darkOneUuid.equals(uuid);
    }

    public void setDarkOne(UUID uuid) {
        this.darkOneUuid = uuid;
        markDirty();
    }

    public void clearDarkOne() {
        this.darkOneUuid = null;
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        if (darkOneUuid != null) {
            nbt.putUuid("DarkOne", darkOneUuid);
        }
        return nbt;
    }

    public static DarkOneState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        DarkOneState state = new DarkOneState();
        if (nbt.containsUuid("DarkOne")) {
            state.darkOneUuid = nbt.getUuid("DarkOne");
        }
        return state;
    }

    private static final Type<DarkOneState> TYPE = new Type<>(
            DarkOneState::new,
            DarkOneState::createFromNbt,
            null
    );

    public static DarkOneState get(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return manager.getOrCreate(TYPE, "the_dark_one");
    }
}
