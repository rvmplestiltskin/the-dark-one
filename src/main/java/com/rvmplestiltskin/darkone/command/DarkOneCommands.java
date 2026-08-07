package com.rvmplestiltskin.darkone.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

import java.util.List;
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
                    // Contracts
                    .then(Commands.literal("contract")
                            .then(Commands.literal("offer")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("terms", StringArgumentType.greedyString())
                                                    .executes(ctx -> offerContract(ctx))
                                            )
                                    )
                            )
                            .then(Commands.literal("accept")
                                    .executes(ctx -> acceptContract(ctx))
                            )
                            .then(Commands.literal("list")
                                    .executes(ctx -> listContracts(ctx))
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

    private static int offerContract(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer offerer = ctx.getSource().getPlayer();
        if (offerer == null) {
            ctx.getSource().sendFailure(Component.literal("Players only."));
            return 0;
        }

        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
        if (!state.isDarkOne(offerer.getUUID())) {
            ctx.getSource().sendFailure(Component.translatable("message.the-dark-one.contract_only_darkone"));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String terms = StringArgumentType.getString(ctx, "terms");

        if (target.getUUID().equals(offerer.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("Cannot contract yourself."));
            return 0;
        }

        state.addOffer(new DarkOneState.ContractOffer(offerer.getUUID(), target.getUUID(), terms));

        offerer.sendSystemMessage(Component.translatable(
                "message.the-dark-one.contract_offered", target.getName(), terms));
        target.sendSystemMessage(Component.translatable(
                "message.the-dark-one.contract_received", offerer.getName(), terms));
        target.sendSystemMessage(Component.translatable("message.the-dark-one.contract_accept_hint"));

        return 1;
    }

    private static int acceptContract(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Players only."));
            return 0;
        }

        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
        DarkOneState.ContractOffer offer = state.takeOfferFor(player.getUUID());
        if (offer == null) {
            ctx.getSource().sendFailure(Component.translatable("message.the-dark-one.contract_none"));
            return 0;
        }

        state.addContract(new DarkOneState.Contract(
                offer.offerer(), offer.target(), offer.terms(), System.currentTimeMillis()));

        ServerPlayer offerer = ctx.getSource().getServer().getPlayerList().getPlayer(offer.offerer());

        player.sendSystemMessage(Component.translatable(
                "message.the-dark-one.contract_accepted", offer.terms()));
        if (offerer != null) {
            offerer.sendSystemMessage(Component.translatable(
                    "message.the-dark-one.contract_accepted_by", player.getName(), offer.terms()));
        }

        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.the-dark-one.contract_sealed",
                        offerer != null ? offerer.getName() : Component.literal("?"),
                        player.getName()),
                false
        );

        return 1;
    }

    private static int listContracts(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());

        List<DarkOneState.Contract> list;
        if (player != null) {
            list = state.getContractsInvolving(player.getUUID());
            if (list.isEmpty() && state.isDarkOne(player.getUUID())) {
                list = state.getContracts();
            }
        } else {
            list = state.getContracts();
        }

        if (list.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.contract_list_empty"), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.contract_list_header"), false);
        var server = ctx.getSource().getServer();
        for (DarkOneState.Contract c : list) {
            String a = nameOf(server, c.partyA());
            String b = nameOf(server, c.partyB());
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "- " + a + " <-> " + b + ": " + c.terms()), false);
        }
        return 1;
    }

    private static String nameOf(net.minecraft.server.MinecraftServer server, UUID uuid) {
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        return p != null ? p.getName().getString() : uuid.toString().substring(0, 8);
    }
}
