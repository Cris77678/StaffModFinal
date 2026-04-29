package me.drex.staffmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.drex.staffmod.config.Kit;
import me.drex.staffmod.features.KitManager;
import me.drex.staffmod.gui.KitEditorGui;
import me.drex.staffmod.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KitCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("staffkit")
            .requires(src -> {
                try { return PermissionUtil.has(src.getPlayerOrException(), "staffmod.admin"); }
                catch (Exception e) { return false; }
            })
            .then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("nombre", StringArgumentType.string())
                .then(Commands.argument("permiso", StringArgumentType.word())
                .executes(KitCommand::createKit)))))
            .then(Commands.literal("edit")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        KitManager.getAllKits().forEach(k -> builder.suggest(k.id));
                        return builder.buildFuture();
                    })
                    .executes(KitCommand::editKit)))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        KitManager.getAllKits().forEach(k -> builder.suggest(k.id));
                        return builder.buildFuture();
                    })
                    .executes(KitCommand::deleteKit)))
        );
    }

    private static int createKit(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id     = StringArgumentType.getString(ctx, "id").toLowerCase();
        String name   = StringArgumentType.getString(ctx, "nombre");
        String perm   = StringArgumentType.getString(ctx, "permiso");

        if (KitManager.getKit(id) != null) {
            player.sendSystemMessage(Component.literal("§cEl kit '" + id + "' ya existe. Usa /staffkit edit " + id));
            return 0;
        }
        Kit newKit = new Kit(id, name, perm, 86400L, "minecraft:chest", "");
        KitManager.createOrUpdateKit(newKit);
        new KitEditorGui(player, newKit, null).open();
        return 1;
    }

    private static int editKit(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id").toLowerCase();
        Kit kit = KitManager.getKit(id);
        if (kit == null) {
            player.sendSystemMessage(Component.literal("§cNo existe ningún kit con esa ID."));
            return 0;
        }
        new KitEditorGui(player, kit, null).open();
        return 1;
    }

    private static int deleteKit(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id").toLowerCase();
        if (KitManager.getKit(id) == null) {
            player.sendSystemMessage(Component.literal("§cNo existe ningún kit con esa ID."));
            return 0;
        }
        KitManager.deleteKit(id);
        player.sendSystemMessage(Component.literal("§a[ᴋɪᴛs] Kit '" + id + "' eliminado."));
        return 1;
    }
}
