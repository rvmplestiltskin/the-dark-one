package com.rvmplestiltskin.darkone;

import com.rvmplestiltskin.darkone.network.TeleportPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TheDarkOneClient implements ClientModInitializer {

    public static KeyBinding teleportKey;

    @Override
    public void onInitializeClient() {
        teleportKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.the-dark-one.teleport",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.the-dark-one"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (teleportKey.wasPressed()) {
                if (client.player != null) {
                    // Send request to server
                    ClientPlayNetworking.send(new TeleportPayload());
                }
            }
        });
    }
}
