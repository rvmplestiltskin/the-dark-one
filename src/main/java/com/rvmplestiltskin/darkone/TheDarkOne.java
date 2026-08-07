package com.rvmplestiltskin.darkone;

import com.rvmplestiltskin.darkone.command.DarkOneCommands;
import com.rvmplestiltskin.darkone.item.ModItems;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TheDarkOne implements ModInitializer {

    public static final String MOD_ID = "the-dark-one";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("The Dark One is awakening...");

        ModItems.register();
        DarkOneCommands.register();

        // Apply passive powers every tick to the current Dark One
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DarkOneState state = DarkOneState.get(server);
            UUID darkOneId = state.getDarkOneUuid();
            if (darkOneId == null) return;

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(darkOneId);
            if (player == null || !player.isAlive()) return;

            // Permanent-ish effects (re-applied so they never expire)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, true, false, false));
        });

        // Death transfer logic: if the Dark One is killed by someone holding the dagger
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity deadPlayer)) return;

            var server = deadPlayer.getServer();
            if (server == null) return;

            DarkOneState state = DarkOneState.get(server);
            if (!state.isDarkOne(deadPlayer.getUuid())) return;

            // Check if the attacker is a player holding the dagger
            if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
                ItemStack main = killer.getMainHandStack();
                ItemStack off = killer.getOffHandStack();

                boolean holdingDagger = main.isOf(ModItems.DARK_ONES_DAGGER) || off.isOf(ModItems.DARK_ONES_DAGGER);

                if (holdingDagger) {
                    // Transfer power
                    state.setDarkOne(killer.getUuid());
                    // Give the dagger to the new Dark One (remove from old inventory if present)
                    giveDaggerTo(killer);

                    server.getPlayerManager().broadcast(
                            Text.translatable("message.the-dark-one.transferred", killer.getName()),
                            false
                    );
                    LOGGER.info("{} has taken the power of the Dark One from {}", killer.getName().getString(), deadPlayer.getName().getString());
                } else {
                    // Dark One died without the dagger transfer → power is lost until claimed again
                    state.clearDarkOne();
                    server.getPlayerManager().broadcast(
                            Text.translatable("message.the-dark-one.cleared"),
                            false
                    );
                }
            } else {
                // Died to environment / mobs → clear the power
                state.clearDarkOne();
            }
        });

        // When a player joins, if they are the Dark One make sure they have the dagger
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            DarkOneState state = DarkOneState.get(server);
            if (state.isDarkOne(player.getUuid())) {
                giveDaggerTo(player);
            }
        });
    }

    public static void giveDaggerTo(ServerPlayerEntity player) {
        // Remove any existing daggers from everyone first? (optional, keep it simple)
        ItemStack dagger = new ItemStack(ModItems.DARK_ONES_DAGGER);

        // Try to put in inventory, otherwise drop
        if (!player.getInventory().insertStack(dagger)) {
            player.dropItem(dagger, false);
        }
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
