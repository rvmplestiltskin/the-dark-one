package com.rvmplestiltskin.darkone.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class DarkOnesDaggerItem extends SwordItem {

    public DarkOnesDaggerItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.the-dark-one.dark_ones_dagger.tooltip").formatted(Formatting.DARK_PURPLE));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Always enchanted look
    }
}
