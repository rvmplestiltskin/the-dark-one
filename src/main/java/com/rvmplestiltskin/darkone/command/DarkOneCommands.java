package com.rvmplestiltskin.darkone.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rvmplestiltskin.darkone.TheDarkOne;
import com.rvmplestiltskin.darkone.item.ModItems;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class DarkOneCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("darkone")
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
                    .then(Commands.literal("create")
                            .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                    .executes(ctx -> createItem(ctx, 1))
                                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                            .executes(ctx -> createItem(ctx, IntegerArgumentType.getInteger(ctx, "count")))
                                    )
                            )
                    )
            );
        });
    }

    private static int createItem(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can create items."));
            return 0;
        }

        if (!canUseCreate(player)) {
            ctx.getSource().sendFailure(Component.translatable("message.the-dark-one.create_denied"));
            return 0;
        }

        ItemInput input = ItemArgument.getItem(ctx, "item");
        ItemStack stack = input.createItemStack(count);

        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        final int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "message.the-dark-one.created", stack.getHoverName(), finalCount), false);
        return 1;
    }

    private static boolean canUseCreate(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) return false;

        DarkOneState state = DarkOneState.get(server);
        if (state.isDarkOne(player.getUUID())) return true;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ModItems.DARK_ONES_DAGGER) || off.is(ModItems.DARK_ONES_DAGGER);
    }
}
