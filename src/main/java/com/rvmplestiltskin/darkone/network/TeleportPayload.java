package com.rvmplestiltskin.darkone.network;

import com.rvmplestiltskin.darkone.TheDarkOne;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TeleportPayload() implements CustomPacketPayload {

    public static final Type<TeleportPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TheDarkOne.MOD_ID, "teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPayload> STREAM_CODEC =
            StreamCodec.unit(new TeleportPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
