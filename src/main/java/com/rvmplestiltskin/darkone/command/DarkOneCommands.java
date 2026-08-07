package com.rvmplestiltskin.darkone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.rvmplestiltskin.darkone.TheDarkOne;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DarkOneCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("darkone")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(literal("set")
                            .then(argument("player", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());

                                        state.setDarkOne(target.getUuid());
                                        TheDarkOne.giveDaggerTo(target);

                                        ctx.getSource().getServer().getPlayerManager().broadcast(
                                                Text.translatable("message.the-dark-one.became", target.getName()),
                                                false
                                        );
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("clear")
                            .executes(ctx -> {
                                DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
                                state.clearDarkOne();
                                ctx.getSource().sendFeedback(() -> Text.translatable("message.the-dark-one.cleared"), true);
                                return 1;
                            })
                    )
                    .then(literal("who")
                            .executes(ctx -> {
                                DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
                                UUID id = state.getDarkOneUuid();
                                if (id == null) {
                                    ctx.getSource().sendFeedback(() -> Text.translatable("message.the-dark-one.none"), false);
                                } else {
                                    ServerPlayerEntity player = ctx.getSource().getServer().getPlayerManager().getPlayer(id);
                                    String name = player != null ? player.getName().getString() : id.toString();
                                    ctx.getSource().sendFeedback(() -> Text.translatable("message.the-dark-one.current", name), false);
                                }
                                return 1;
                            })
                    )
            );
        });
    }
}
