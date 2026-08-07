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
                    // Permission level 2 = gamemaster / typical OP cheats level
                    .requires(source -> {
                        try {
                            // Works on most 26.x builds
                            return source.hasPermission(2);
                        } catch (Throwable t) {
                            // Fallback: only allow if source is a player who is OP
                            ServerPlayer player = source.getPlayer();
                            if (player == null) return source.getServer() != null;
                            return source.getServer().getPlayerList().isOp(player.getGameProfile());
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
