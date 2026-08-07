package com.rvmplestiltskin.darkone.command;

import com.rvmplestiltskin.darkone.TheDarkOne;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class DarkOneCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("darkone")
                    // Console always allowed; in-game players need to be operators.
                    // 26.2 PlayerList.isOp takes NameAndId, not GameProfile.
                    .requires(source -> {
                        ServerPlayer player = source.getPlayer();
                        if (player == null) {
                            return true; // dedicated server console / command block
                        }
                        try {
                            // Prefer NameAndId API when present
                            var nameAndId = player.nameAndId();
                            return source.getServer().getPlayerList().isOp(nameAndId);
                        } catch (Throwable ignored) {
                            // Fallback: allow if singleplayer owner, otherwise require cheats context
                            try {
                                return source.getServer().isSingleplayerOwner(player.getGameProfile());
                            } catch (Throwable ignored2) {
                                return source.getServer().getPlayerList().isOp(player.getGameProfile());
                            }
                        }
                    })
                    .then(Commands.literal("set")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());

                                        state.setDarkOne(target.getUUID());
                                        TheDarkOne.transferDaggerTo(ctx.getSource().getServer(), target);

                                        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                Component.translatable("message.the-dark-one.became", target.getName()),
                                                false
                                        );
                                        return 1;
                                    })
                            )
                    )
                    .then(Commands.literal("clear")
                            .executes(ctx -> {
                                DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
                                state.clearDarkOne();
                                ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.cleared"), true);
                                return 1;
                            })
                    )
                    .then(Commands.literal("who")
                            .executes(ctx -> {
                                DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
                                UUID id = state.getDarkOneUuid();
                                if (id == null) {
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.none"), false);
                                } else {
                                    ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(id);
                                    String name = player != null ? player.getName().getString() : id.toString();
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.current", name), false);
                                }
                                return 1;
                            })
                    )
            );
        });
    }
}
