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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ModNetworking {

    public static final double TELEPORT_DISTANCE = 32.0;
    public static final double STEP = 0.25;

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

                // Raycast: walk forward until blocked, land just before the obstacle
                Vec3 safe = start;
                int steps = (int) (TELEPORT_DISTANCE / STEP);
                for (int i = 1; i <= steps; i++) {
                    Vec3 candidate = start.add(look.scale(i * STEP));
                    if (!isSafeTeleportSpot(level, candidate, player.getBbHeight())) {
                        break;
                    }
                    safe = candidate;
                }

                // Don't teleport if we barely moved
                if (safe.distanceToSqr(start) < 0.5) {
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

    /** Feet and head must not be inside solid blocks; allow air/water/passable. */
    private static boolean isSafeTeleportSpot(ServerLevel level, Vec3 pos, float height) {
        BlockPos feet = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockPos head = BlockPos.containing(pos.x, pos.y + height - 0.1, pos.z);

        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);

        // Reject solid collision at feet or head
        if (!feetState.getCollisionShape(level, feet).isEmpty()) {
            return false;
        }
        if (!headState.getCollisionShape(level, head).isEmpty()) {
            return false;
        }
        return true;
    }
}
