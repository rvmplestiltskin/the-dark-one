package com.rvmplestiltskin.darkone.item;

import com.rvmplestiltskin.darkone.TheDarkOne;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ModItems {

    public static final ResourceKey<Item> DARK_ONES_DAGGER_KEY = ResourceKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TheDarkOne.MOD_ID, "dark_ones_dagger")
    );

    public static final Item DARK_ONES_DAGGER = register(
            DARK_ONES_DAGGER_KEY,
            new DarkOnesDaggerItem(new Item.Properties()
                    .setId(DARK_ONES_DAGGER_KEY)
                    .stacksTo(1)
                    .fireResistant()
            )
    );

    private static Item register(ResourceKey<Item> key, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {
        TheDarkOne.LOGGER.info("Registering items for {}", TheDarkOne.MOD_ID);
    }
}
