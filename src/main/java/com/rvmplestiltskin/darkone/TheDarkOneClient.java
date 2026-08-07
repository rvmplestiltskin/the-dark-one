package com.rvmplestiltskin.darkone;

import com.rvmplestiltskin.darkone.network.TeleportPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class TheDarkOneClient implements ClientModInitializer {

    public static KeyMapping teleportKey;

    @Override
    public void onInitializeClient() {
        teleportKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.the-dark-one.teleport",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.the-dark-one"
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
