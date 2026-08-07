package com.rvmplestiltskin.darkone;

import com.rvmplestiltskin.darkone.command.DarkOneCommands;
import com.rvmplestiltskin.darkone.item.ModItems;
import com.rvmplestiltskin.darkone.network.ModNetworking;
import com.rvmplestiltskin.darkone.state.ContractEnforcement;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class TheDarkOne implements ModInitializer {

    public static final String MOD_ID = "the-dark-one";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final double DAGGER_CONTROL_RANGE = 16.0;

    @Override
    public void onInitialize() {
        LOGGER.info("The Dark One is awakening...");

        ModItems.register();
        DarkOneCommands.register();
        ModNetworking.register();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DarkOneState state = DarkOneState.get(server);
            UUID darkOneId = state.getDarkOneUuid();
            if (darkOneId == null) return;

            ServerPlayer darkOne = server.getPlayerList().getPlayer(darkOneId);
            if (darkOne == null || !darkOne.isAlive()) return;

            darkOne.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 2, true, false, false));
            darkOne.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, true, false, false));
            darkOne.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 2, true, false, false));
            darkOne.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 3, true, false, false));
            darkOne.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
            darkOne.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
            darkOne.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false, false));
            darkOne.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 1, true, false, false));

            ServerPlayer controller = findDaggerHolderNear(server, darkOne, DAGGER_CONTROL_RANGE);
            if (controller != null && !controller.getUUID().equals(darkOne.getUUID())) {
                darkOne.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, true, false, true));
                darkOne.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, true, false, true));
                if (darkOne.tickCount % 20 == 0) {
                    ServerLevel level = (ServerLevel) darkOne.level();
                    level.sendParticles(ParticleTypes.SMOKE,
                            darkOne.getX(), darkOne.getY() + 1.0, darkOne.getZ(),
                            8, 0.3, 0.5, 0.3, 0.01);
                }

                // Auto: contracted player holding the dagger near Dark One = betrayal attempt
                if (ContractEnforcement.isBoundToDarkOne(server, controller.getUUID())
                        && state.canAutoPunish(controller.getUUID(), 15_000)) {
                    ContractEnforcement.punish(server, controller, ContractEnforcement.Severity.SEVERE,
                            "intentar controlar al Oscuro con la daga estando bajo contrato");
                }
            }
        });

        // Immortality unless dagger
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer victim)) return true;

            MinecraftServer server = victim.level().getServer();
            if (server == null) return true;

            DarkOneState state = DarkOneState.get(server);
            if (!state.isDarkOne(victim.getUUID())) return true;

            if (source.getEntity() instanceof ServerPlayer attacker) {
                ItemStack main = attacker.getMainHandItem();
                ItemStack off = attacker.getOffhandItem();
                if (main.is(ModItems.DARK_ONES_DAGGER) || off.is(ModItems.DARK_ONES_DAGGER)) {
                    return true;
                }

                // Auto: contracted player attacks the Dark One (any weapon) = severe punishment
                if (ContractEnforcement.isBoundToDarkOne(server, attacker.getUUID())
                        && state.canAutoPunish(attacker.getUUID(), 8_000)) {
                    ContractEnforcement.punish(server, attacker, ContractEnforcement.Severity.SEVERE,
                            "atacar al Oscuro estando bajo contrato");
                }
            }
            return false;
        });

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
                    playTransferEffects(server, killer, deadPlayer);
                } else {
                    state.clearDarkOne();
                    server.getPlayerList().broadcastSystemMessage(
                            Component.translatable("message.the-dark-one.cleared"), false);
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

    private static ServerPlayer findDaggerHolderNear(MinecraftServer server, ServerPlayer darkOne, double range) {
        double rangeSq = range * range;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getUUID().equals(darkOne.getUUID())) continue;
            if (!player.level().dimension().equals(darkOne.level().dimension())) continue;
            if (player.distanceToSqr(darkOne) > rangeSq) continue;
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            if (main.is(ModItems.DARK_ONES_DAGGER) || off.is(ModItems.DARK_ONES_DAGGER)) {
                return player;
            }
        }
        return null;
    }

    public static void playTransferEffects(MinecraftServer server, ServerPlayer newDarkOne, ServerPlayer oldDarkOne) {
        ServerLevel level = (ServerLevel) newDarkOne.level();
        level.sendParticles(ParticleTypes.SOUL,
                oldDarkOne.getX(), oldDarkOne.getY() + 1, oldDarkOne.getZ(),
                80, 0.8, 1.2, 0.8, 0.05);
        level.sendParticles(ParticleTypes.PORTAL,
                newDarkOne.getX(), newDarkOne.getY() + 1, newDarkOne.getZ(),
                100, 0.8, 1.5, 0.8, 0.2);
        level.playSound(null, newDarkOne.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7f, 0.8f);
        level.playSound(null, newDarkOne.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5f, 1.2f);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.the-dark-one.transferred_dramatic",
                        newDarkOne.getName(), oldDarkOne.getName()),
                false
        );
    }

    public static void transferDaggerTo(MinecraftServer server, ServerPlayer target) {
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            if (target.getInventory().getItem(i).is(ModItems.DARK_ONES_DAGGER)) {
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
