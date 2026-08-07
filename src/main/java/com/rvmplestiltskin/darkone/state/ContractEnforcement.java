package com.rvmplestiltskin.darkone.state;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.UUID;

/**
 * Magical punishments for contract violations — manual or automatic.
 */
public final class ContractEnforcement {

    private ContractEnforcement() {}

    public enum Severity {
        WARNING,
        STANDARD,
        SEVERE
    }

    public static boolean punish(MinecraftServer server, ServerPlayer target, Severity severity, String reason) {
        DarkOneState state = DarkOneState.get(server);
        if (state.getDarkOneUuid() == null) return false;

        boolean bound = isBoundToDarkOne(server, target.getUUID());
        if (!bound) return false;

        ServerLevel level = (ServerLevel) target.level();

        switch (severity) {
            case WARNING -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 30, 0));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 15, 0));
                level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 0.6f);
            }
            case STANDARD -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 60 * 3, 1));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 60 * 2, 1));
                target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20 * 60 * 5, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 20, 0));
                target.hurt(target.damageSources().magic(), 4.0f);
                level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.7f, 0.5f);
            }
            case SEVERE -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 60 * 10, 2));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 60 * 5, 2));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 30, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 15, 1));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 60, 0));
                target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20 * 60 * 10, 1));
                target.hurt(target.damageSources().magic(), 12.0f);
                level.playSound(null, target.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.4f);
                level.playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0f, 0.7f);
            }
        }

        level.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + 1, target.getZ(),
                40, 0.5, 1.0, 0.5, 0.05);
        level.sendParticles(ParticleTypes.SMOKE,
                target.getX(), target.getY() + 1, target.getZ(),
                20, 0.4, 0.8, 0.4, 0.02);

        target.sendSystemMessage(Component.translatable("message.the-dark-one.punish_self", reason));

        ServerPlayer darkOne = server.getPlayerList().getPlayer(state.getDarkOneUuid());
        if (darkOne != null) {
            darkOne.sendSystemMessage(Component.translatable(
                    "message.the-dark-one.punish_notify", target.getName(), reason));
        }

        state.recordViolation(target.getUUID());
        return true;
    }

    public static boolean isBoundToDarkOne(MinecraftServer server, UUID playerId) {
        DarkOneState state = DarkOneState.get(server);
        UUID dark = state.getDarkOneUuid();
        if (dark == null || dark.equals(playerId)) return false;
        for (DarkOneState.Contract c : state.getContractsInvolving(playerId)) {
            if (c.partyA().equals(dark) || c.partyB().equals(dark)) {
                return true;
            }
        }
        return false;
    }
}
