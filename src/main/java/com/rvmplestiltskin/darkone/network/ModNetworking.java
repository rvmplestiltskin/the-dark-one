package com.rvmplestiltskin.darkone.network;

import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class ModNetworking {

    public static final double TELEPORT_DISTANCE = 32.0;

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TeleportPayload.TYPE, TeleportPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                DarkOneState state = DarkOneState.get(context.server());
                if (!state.isDarkOne(player.getUUID())) {
                    return;
                }

                Vec3 look = player.getLookAngle();
                Vec3 target = player.position().add(look.scale(TELEPORT_DISTANCE));

                ServerLevel level = (ServerLevel) player.level();

                level.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        40, 0.5, 1.0, 0.5, 0.15);

                player.teleportTo(target.x, target.y, target.z);
                player.fallDistance = 0;

                level.sendParticles(ParticleTypes.PORTAL,
                        target.x, target.y + 1, target.z,
                        50, 0.6, 1.2, 0.6, 0.2);

                level.playSound(null, BlockPos.containing(target),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 1.0f, 0.6f);
            });
        });
    }
}
