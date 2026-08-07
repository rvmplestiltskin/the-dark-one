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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ModNetworking {

    public static final double TELEPORT_DISTANCE = 32.0;
    public static final double STEP = 0.5;

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TeleportPayload.TYPE, TeleportPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                DarkOneState state = DarkOneState.get(context.server());
                if (!state.isDarkOne(player.getUUID())) {
                    return;
                }

                ServerLevel level = (ServerLevel) player.level();
                Vec3 start = player.position();
                Vec3 look = player.getLookAngle().normalize();
                AABB box = player.getBoundingBox();

                // Advance along look direction while the full player hitbox fits
                Vec3 safe = start;
                int steps = (int) (TELEPORT_DISTANCE / STEP);
                for (int i = 1; i <= steps; i++) {
                    Vec3 delta = look.scale(i * STEP);
                    AABB moved = box.move(delta);
                    // noCollision = space is free of solid blocks for this entity
                    if (!level.noCollision(player, moved)) {
                        break;
                    }
                    safe = start.add(delta);
                }

                // Need at least ~1 block of travel
                if (safe.distanceToSqr(start) < 1.0) {
                    return;
                }

                level.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        40, 0.5, 1.0, 0.5, 0.15);

                player.teleportTo(safe.x, safe.y, safe.z);
                player.fallDistance = 0;

                level.sendParticles(ParticleTypes.PORTAL,
                        safe.x, safe.y + 1, safe.z,
                        50, 0.6, 1.2, 0.6, 0.2);

                level.playSound(null, BlockPos.containing(safe),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 1.0f, 0.6f);
            });
        });
    }
}
