package com.rvmplestiltskin.darkone;

import com.rvmplestiltskin.darkone.command.DarkOneCommands;
import com.rvmplestiltskin.darkone.item.ModItems;
import com.rvmplestiltskin.darkone.network.ModNetworking;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

        // Passiveives + slow falling while Dark One
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DarkOneState state = DarkOneState.get(server);
            UUID darkOneId = state.getDarkOneUuid();
            if (darkOneId == null) return;

            ServerPlayer player = server.getPlayerList().getPlayer(darkOneId);
            if (player == null || !player.isAlive()) return;

            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 2, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 2, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 3, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 1, true, false, false));
        });

        // Immortality: cancel ALL damage unless attacker holds the dagger
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer victim)) return true;

            MinecraftServer server = victim.level().getServer();
            if (server == null) return true;

            DarkOneState state = DarkOneState.get(server);
            if (!state.isDarkOne(victim.getUUID())) return true;

            // Only the dagger can harm the Dark One
            if (source.getEntity() instanceof ServerPlayer attacker) {
                ItemStack main = attacker.getMainHandItem();
                ItemStack off = attacker.getOffhandItem();
                boolean holdingDagger = main.is(ModItems.DARK_ONES_DAGGER) || off.is(ModItems.DARK_ONES_DAGGER);
                if (holdingDagger) {
                    return true; // allow dagger damage
                }
            }

            // Block everything else (fall, fire, mobs, void is still deadly in some cases)
            return false;
        });

        // Death transfer with dagger
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayer deadPlayer)) return;

            MinecraftServer server = deadPlayer.level().getServer();
            if (server == null) return;

            DarkOneState state = DarkOneState.get(server);
            if (!state.isDarkOne(deadPlayer.getUUID())) return;

            if (damageSource.getEntity() instanceof ServerPlayer killer) {
                ItemStack main = killer.getMainHandItem();
                ItemStack off = killer.getOffhandItem();
                boolean holdingDagger = main.is(ModItems.DARK_ONES_DAGGER) || off.is(ModItems.DARK_ONES_DAGGER);

                if (holdingDagger) {
                    state.setDarkOne(killer.getUUID());
                    transferDaggerTo(server, killer);
                    server.getPlayerList().broadcastSystemMessage(
                            Component.translatable("message.the-dark-one.transferred", killer.getName()),
                            false
                    );
                    LOGGER.info("{} took the Dark One power from {}",
                            killer.getName().getString(), deadPlayer.getName().getString());
                } else {
                    state.clearDarkOne();
                    server.getPlayerList().broadcastSystemMessage(
                            Component.translatable("message.the-dark-one.cleared"),
                            false
                    );
                }
            } else {
                state.clearDarkOne();
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            DarkOneState state = DarkOneState.get(server);
            if (state.isDarkOne(player.getUUID())) {
                transferDaggerTo(server, player);
            }
        });
    }

    public static void transferDaggerTo(MinecraftServer server, ServerPlayer target) {
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            if (stack.is(ModItems.DARK_ONES_DAGGER)) {
                target.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        ItemStack found = ItemStack.EMPTY;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(ModItems.DARK_ONES_DAGGER)) {
                    if (found.isEmpty()) found = stack.copy();
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AABB box = player.getBoundingBox().inflate(64);
            List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, box,
                    e -> e.getItem().is(ModItems.DARK_ONES_DAGGER));
            for (ItemEntity itemEntity : items) {
                if (found.isEmpty()) found = itemEntity.getItem().copy();
                itemEntity.discard();
            }
        }

        ItemStack toGive = found.isEmpty() ? new ItemStack(ModItems.DARK_ONES_DAGGER) : found;
        if (!target.getInventory().add(toGive)) {
            target.drop(toGive, false);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
