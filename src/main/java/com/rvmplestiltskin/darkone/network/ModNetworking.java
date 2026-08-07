package com.rvmplestiltskin.darkone.network;

import com.rvmplestiltskin.darkone.TheDarkOne;
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

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TeleportPayload.TYPE, TeleportPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                DarkOneState state = DarkOneState.get(context.server());
                if (!state.isDarkOne(player.getUUID())) {
                    return;
                }

                Vec3 look = player.getLookAngle();
                Vec3 target = player.position().add(look.scale(8.0));

                ServerLevel level = player.serverLevel();

                level.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        30, 0.5, 1.0, 0.5, 0.1);

                player.teleportTo(target.x, target.y, target.z);
                player.fallDistance = 0;

                level.sendParticles(ParticleTypes.PORTAL,
                        target.x, target.y + 1, target.z,
                        40, 0.5, 1.0, 0.5, 0.15);

                level.playSound(null, BlockPos.containing(target),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 1.0f, 0.7f);
            });
        });
    }
}
