package com.rvmplestiltskin.darkone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Unique, indestructible dagger.
 * Instakills on hit. Right-click purges hostiles in a huge radius.
 */
public class DarkOnesDaggerItem extends Item {

    /** 16 chunks = 256 blocks */
    public static final double PURGE_RANGE = 16 * 16;

    /** Cooldown in ticks (10 seconds) */
    public static final int PURGE_COOLDOWN = 20 * 10;

    private static final Identifier COOLDOWN_ID =
            Identifier.fromNamespaceAndPath("the-dark-one", "dagger_purge");

    public DarkOnesDaggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.the-dark-one.dark_ones_dagger.tooltip")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.the-dark-one.dark_ones_dagger.tooltip2")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.the-dark-one.dark_ones_dagger.tooltip3")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.accept(Component.translatable("item.the-dark-one.dark_ones_dagger.tooltip4")
                .withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.literal("Click derecho: elimina hostiles en 16 chunks")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 26.2: cooldown keyed by Identifier or ItemStack
        if (player.getCooldowns().isOnCooldown(COOLDOWN_ID) || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        AABB box = player.getBoundingBox().inflate(PURGE_RANGE);

        List<LivingEntity> hostiles = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive()
                        && !(e instanceof Player)
                        && (e instanceof Monster || e instanceof Enemy)
        );

        int killed = 0;
        for (LivingEntity mob : hostiles) {
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    mob.getX(), mob.getY() + 0.5, mob.getZ(),
                    8, 0.3, 0.5, 0.3, 0.02);
            mob.discard();
            killed++;
        }

        player.getCooldowns().addCooldown(COOLDOWN_ID, PURGE_COOLDOWN);

        serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.6f, 1.5f);
        serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.4f, 0.8f);

        serverLevel.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 1.0, 1.0, 1.0, 0.05);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "La daga reclama " + killed + " hostiles en " + (int) PURGE_RANGE + " bloques.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide()) {
            target.hurt(attacker.damageSources().magic(), Float.MAX_VALUE);
            target.setHealth(0.0f);
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide() && target.isAlive()) {
            target.hurt(attacker.damageSources().magic(), Float.MAX_VALUE);
            target.setHealth(0.0f);
        }
    }
}
