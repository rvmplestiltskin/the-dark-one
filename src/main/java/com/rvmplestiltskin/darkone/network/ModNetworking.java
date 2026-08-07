package com.rvmplestiltskin.darkone.network;

import com.rvmplestiltskin.darkone.TheDarkOne;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ModNetworking {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TeleportPayload.ID, TeleportPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                DarkOneState state = DarkOneState.get(context.server());
                if (!state.isDarkOne(player.getUuid())) {
                    return; // Only the Dark One can teleport this way
                }

                // Simple forward teleport (8 blocks)
                Vec3d look = player.getRotationVec(1.0f);
                Vec3d target = player.getPos().add(look.multiply(8.0));

                ServerWorld world = player.getServerWorld();

                // Particles at origin
                world.spawnParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        30, 0.5, 1.0, 0.5, 0.1);

                player.requestTeleport(target.x, target.y, target.z);
                player.fallDistance = 0;

                // Particles at destination + sound
                world.spawnParticles(ParticleTypes.PORTAL,
                        target.x, target.y + 1, target.z,
                        40, 0.5, 1.0, 0.5, 0.15);

                world.playSound(null, BlockPos.ofFloored(target),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.PLAYERS, 1.0f, 0.7f);
            });
        });
    }
}
