package com.rvmplestiltskin.darkone.item;

import com.rvmplestiltskin.darkone.TheDarkOne;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item DARK_ONES_DAGGER = register("dark_ones_dagger",
            new DarkOnesDaggerItem(ToolMaterials.NETHERITE, new Item.Settings()
                    .maxCount(1)
                    .fireproof()
            )
    );

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(TheDarkOne.MOD_ID, name), item);
    }

    public static void register() {
        TheDarkOne.LOGGER.info("Registering items for " + TheDarkOne.MOD_ID);
    }
}
