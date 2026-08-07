package com.rvmplestiltskin.darkone;

import com.rvmplestiltskin.darkone.network.TeleportPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class TheDarkOneClient implements ClientModInitializer {

    public static KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(TheDarkOne.MOD_ID, "dark_one")
    );

    public static KeyMapping teleportKey;

    @Override
    public void onInitializeClient() {
        teleportKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.the-dark-one.teleport",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (teleportKey.consumeClick()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(new TeleportPayload());
                }
            }
        });
    }
}
