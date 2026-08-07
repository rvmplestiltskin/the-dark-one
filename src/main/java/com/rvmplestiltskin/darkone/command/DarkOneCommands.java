package com.rvmplestiltskin.darkone.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rvmplestiltskin.darkone.TheDarkOne;
import com.rvmplestiltskin.darkone.item.ModItems;
import com.rvmplestiltskin.darkone.state.ContractEnforcement;
import com.rvmplestiltskin.darkone.state.ContractTemplates;
import com.rvmplestiltskin.darkone.state.DarkOneState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
                    .then(Commands.literal("punish")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> punish(ctx, ContractEnforcement.Severity.STANDARD))
                                    .then(Commands.literal("warning")
                                            .executes(ctx -> punish(ctx, ContractEnforcement.Severity.WARNING))
                                    )
                                    .then(Commands.literal("severe")
                                            .executes(ctx -> punish(ctx, ContractEnforcement.Severity.SEVERE))
                                    )
                            )
                    )
                    .then(Commands.literal("contract")
                            .then(Commands.literal("templates")
                                    .executes(DarkOneCommands::listTemplates)
                            )
                            .then(Commands.literal("offer")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.literal("template")
                                                    .then(Commands.argument("id", StringArgumentType.word())
                                                            .executes(DarkOneCommands::offerTemplate)
                                                    )
                                            )
                                            .then(Commands.argument("terms", StringArgumentType.greedyString())
                                                    .executes(DarkOneCommands::offerCustom)
                                            )
                                    )
                            )
                            .then(Commands.literal("accept")
                                    .executes(DarkOneCommands::acceptContract)
                            )
                            .then(Commands.literal("decline")
                                    .executes(DarkOneCommands::declineContract)
                            )
                            .then(Commands.literal("list")
                                    .executes(DarkOneCommands::listContracts)
                            )
                            .then(Commands.literal("read")
                                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                            .executes(DarkOneCommands::readContract)
                                    )
                            )
                    )
            );
        });
    }

    private static int punish(CommandContext<CommandSourceStack> ctx, ContractEnforcement.Severity severity) throws CommandSyntaxException {
        ServerPlayer dark = ctx.getSource().getPlayer();
        if (dark == null) {
            ctx.getSource().sendFailure(Component.literal("Players only."));
            return 0;
        }
        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
        if (!state.isDarkOne(dark.getUUID())) {
            ctx.getSource().sendFailure(Component.translatable("message.the-dark-one.punish_only_darkone"));
            return 0;
        }
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean ok = ContractEnforcement.punish(ctx.getSource().getServer(), target, severity,
                "castigo manual del Oscuro");
        if (!ok) {
            ctx.getSource().sendFailure(Component.translatable("message.the-dark-one.punish_not_bound"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "message.the-dark-one.punish_done", target.getName()), false);
        return 1;
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

    private static int listTemplates(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.templates_header"), false);
        for (var e : ContractTemplates.TEMPLATES.entrySet()) {
            String id = e.getKey();
            String preview = e.getValue().length() > 80 ? e.getValue().substring(0, 80) + "..." : e.getValue();
            ctx.getSource().sendSuccess(() -> Component.literal("- " + id + ": " + preview), false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.templates_usage"), false);
        return 1;
    }

    private static int offerTemplate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        String terms = ContractTemplates.get(id);
        if (terms == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "message.the-dark-one.template_unknown", id, ContractTemplates.listIds()));
            return 0;
        }
        return offerTo(ctx, terms, id);
    }

    private static int offerCustom(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String terms = StringArgumentType.getString(ctx, "terms");
        return offerTo(ctx, terms, "");
    }

    private static int offerTo(CommandContext<CommandSourceStack> ctx, String terms, String templateId) throws CommandSyntaxException {
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
        if (target.getUUID().equals(offerer.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("Cannot contract yourself."));
            return 0;
        }
        state.addOffer(new DarkOneState.ContractOffer(offerer.getUUID(), target.getUUID(), terms, templateId));

        offerer.sendSystemMessage(Component.translatable(
                "message.the-dark-one.contract_offered", target.getName()));
        sendLongMessage(offerer, terms);
        target.sendSystemMessage(Component.translatable(
                "message.the-dark-one.contract_received", offerer.getName()));
        sendLongMessage(target, terms);
        target.sendSystemMessage(Component.translatable("message.the-dark-one.contract_accept_hint"));
        return 1;
    }

    private static void sendLongMessage(ServerPlayer player, String text) {
        int max = 200;
        for (int i = 0; i < text.length(); i += max) {
            int end = Math.min(i + max, text.length());
            player.sendSystemMessage(Component.literal(text.substring(i, end)));
        }
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
                offer.offerer(), offer.target(), offer.terms(), System.currentTimeMillis(),
                offer.templateId(), 0));

        ServerPlayer offerer = ctx.getSource().getServer().getPlayerList().getPlayer(offer.offerer());
        player.sendSystemMessage(Component.translatable("message.the-dark-one.contract_accepted"));
        if (offerer != null) {
            offerer.sendSystemMessage(Component.translatable(
                    "message.the-dark-one.contract_accepted_by", player.getName()));
        }
        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.the-dark-one.contract_sealed",
                        offerer != null ? offerer.getName() : Component.literal("?"),
                        player.getName()),
                false
        );
        return 1;
    }

    private static int declineContract(CommandContext<CommandSourceStack> ctx) {
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
        ServerPlayer offerer = ctx.getSource().getServer().getPlayerList().getPlayer(offer.offerer());
        player.sendSystemMessage(Component.translatable("message.the-dark-one.contract_declined"));
        if (offerer != null) {
            offerer.sendSystemMessage(Component.translatable(
                    "message.the-dark-one.contract_declined_by", player.getName()));
        }
        return 1;
    }

    private static int listContracts(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
        MinecraftServer server = ctx.getSource().getServer();

        List<DarkOneState.Contract> list;
        if (player != null && !state.isDarkOne(player.getUUID())) {
            list = state.getContractsInvolving(player.getUUID());
        } else {
            list = state.getContracts();
        }

        if (list.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.contract_list_empty"), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.contract_list_header"), false);
        int i = 1;
        for (DarkOneState.Contract c : list) {
            String a = nameOf(server, c.partyA());
            String b = nameOf(server, c.partyB());
            String preview = c.terms().length() > 50 ? c.terms().substring(0, 50) + "..." : c.terms();
            final int idx = i;
            final int viol = c.violations();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    idx + ". " + a + " <-> " + b + " [violaciones: " + viol + "]: " + preview), false);
            i++;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.contract_read_hint"), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.contract_unbreakable"), false);
        return 1;
    }

    private static int readContract(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        DarkOneState state = DarkOneState.get(ctx.getSource().getServer());
        MinecraftServer server = ctx.getSource().getServer();

        List<DarkOneState.Contract> list;
        if (player != null && !state.isDarkOne(player.getUUID())) {
            list = state.getContractsInvolving(player.getUUID());
        } else {
            list = state.getContracts();
        }

        int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
        if (index < 0 || index >= list.size()) {
            ctx.getSource().sendFailure(Component.translatable("message.the-dark-one.contract_bad_index"));
            return 0;
        }

        DarkOneState.Contract c = list.get(index);
        ctx.getSource().sendSuccess(() -> Component.literal(
                nameOf(server, c.partyA()) + " <-> " + nameOf(server, c.partyB())
                        + " | violaciones: " + c.violations()), false);
        if (player != null) {
            sendLongMessage(player, c.terms());
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(c.terms()), false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.the-dark-one.contract_unbreakable"), false);
        return 1;
    }

    private static String nameOf(MinecraftServer server, UUID uuid) {
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        return p != null ? p.getName().getString() : uuid.toString().substring(0, 8);
    }
}
