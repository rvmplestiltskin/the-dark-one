package com.rvmplestiltskin.darkone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Unique, indestructible dagger.
 * Instakills any living entity it hits.
 * Holder can create items via /darkone create.
 */
public class DarkOnesDaggerItem extends Item {

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
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** Never loses durability. */
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Instakill anything the dagger touches
        if (!target.level().isClientSide()) {
            target.hurt(attacker.damageSources().magic(), Float.MAX_VALUE);
            target.setHealth(0.0f);
            if (target instanceof ServerPlayer player) {
                player.kill(attacker.damageSources().magic());
            }
        }
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.isAlive() && !target.level().isClientSide()) {
            target.hurt(attacker.damageSources().magic(), Float.MAX_VALUE);
            target.setHealth(0.0f);
        }
        // Do not call super in a way that damages the item
    }
}
