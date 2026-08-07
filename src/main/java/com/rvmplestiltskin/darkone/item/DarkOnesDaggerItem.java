package com.rvmplestiltskin.darkone.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * The unique dagger that can harm and control the Dark One.
 * High base damage via postHurtEnemy bonus when hitting living entities.
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
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Extra burst of damage — the blade that can kill the Dark One is no ordinary knife
        if (target.isAlive()) {
            target.hurt(attacker.damageSources().magic(), 10.0f);
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Ensure it always registers a hit
        return true;
    }
}
