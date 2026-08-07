package com.rvmplestiltskin.darkone;

import com.rvmplestiltskin.darkone.command.DarkOneCommands;
import com.rvmplestiltskin.darkone.item.ModItems;
import com.rvmplestiltskin.darkone.network.ModNetworking;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
        ModNetworking.register();

        // Apply passive powers every tick to the current Dark One
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DarkOneState state = DarkOneState.get(server);
            UUID darkOneId = state.getDarkOneUuid();
            if (darkOneId == null) return;

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(darkOneId);
            if (player == null || !player.isAlive()) return;

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, true, false, false));
        });

        // Death transfer logic
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity deadPlayer)) return;

            MinecraftServer server = deadPlayer.getServer();
            if (server == null) return;

            DarkOneState state = DarkOneState.get(server);
            if (!state.isDarkOne(deadPlayer.getUuid())) return;

            if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
                ItemStack main = killer.getMainHandStack();
                ItemStack off = killer.getOffHandStack();

                boolean holdingDagger = main.isOf(ModItems.DARK_ONES_DAGGER) || off.isOf(ModItems.DARK_ONES_DAGGER);

                if (holdingDagger) {
                    state.setDarkOne(killer.getUuid());
                    transferDaggerTo(server, killer);

                    server.getPlayerManager().broadcast(
                            Text.translatable("message.the-dark-one.transferred", killer.getName()),
                            false
                    );
                    LOGGER.info("{} has taken the power of the Dark One from {}",
                            killer.getName().getString(), deadPlayer.getName().getString());
                } else {
                    state.clearDarkOne();
                    server.getPlayerManager().broadcast(
                            Text.translatable("message.the-dark-one.cleared"),
                            false
                    );
                }
            } else {
                state.clearDarkOne();
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            DarkOneState state = DarkOneState.get(server);
            if (state.isDarkOne(player.getUuid())) {
                // Only transfer existing dagger, never create a new one on join
                transferDaggerTo(server, player);
            }
        });
    }

    /**
     * Ensures there is at most one dagger in the entire world.
     * Moves the existing dagger to the target player if it exists anywhere.
     * Creates the dagger only if zero exist in the world.
     */
    public static void transferDaggerTo(MinecraftServer server, ServerPlayerEntity target) {
        // 1. Remove any daggers currently in the target's inventory (we will give exactly one)
        for (int i = 0; i < target.getInventory().size(); i++) {
            ItemStack stack = target.getInventory().getStack(i);
            if (stack.isOf(ModItems.DARK_ONES_DAGGER)) {
                target.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }

        // 2. Search all online players and remove extra daggers, keep one
        ItemStack found = ItemStack.EMPTY;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(ModItems.DARK_ONES_DAGGER)) {
                    if (found.isEmpty()) {
                        found = stack.copy();
                    }
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
        }

        // 3. Search item entities on the ground in all loaded worlds
        for (ServerWorld world : server.getWorlds()) {
            for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class,
                    item -> item.getStack().isOf(ModItems.DARK_ONES_DAGGER),
                    e -> true)) {
                if (found.isEmpty()) {
                    found = itemEntity.getStack().copy();
                }
                itemEntity.discard();
            }
        }

        // 4. If we found one, give it. If none existed, create the first (and only) one.
        ItemStack toGive = found.isEmpty() ? new ItemStack(ModItems.DARK_ONES_DAGGER) : found;

        if (!target.getInventory().insertStack(toGive)) {
            target.dropItem(toGive, false);
        }
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
