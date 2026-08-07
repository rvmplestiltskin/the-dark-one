package com.rvmplestiltskin.darkone.network;

import com.rvmplestiltskin.darkone.TheDarkOne;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TeleportPayload() implements CustomPayload {

    public static final Id<TeleportPayload> ID = new Id<>(Identifier.of(TheDarkOne.MOD_ID, "teleport"));

    public static final PacketCodec<RegistryByteBuf, TeleportPayload> CODEC = PacketCodec.unit(new TeleportPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
