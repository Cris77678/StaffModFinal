package me.drex.staffmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.drex.staffmod.config.DataStore;
import me.drex.staffmod.config.TicketEntry;
import me.drex.staffmod.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TicketCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ticket")
            .then(Commands.argument("mensaje", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    // Anti-spam: no más de 1 ticket abierto por jugador
                    boolean hasOpen = DataStore.getAllTickets().stream()
                        .anyMatch(t -> t.creatorUuid.equals(player.getUUID())
                            && "ABIERTO".equals(t.status));

                    if (hasOpen) {
                        player.sendSystemMessage(Component.literal(
                            "§c[ᴛɪᴄᴋᴇᴛs] Ya tienes un ticket abierto. Espera a que sea atendido."));
                        return 1;
                    }

                    String message = StringArgumentType.getString(ctx, "mensaje");
                    if (message.length() < 5) {
                        player.sendSystemMessage(Component.literal(
                            "§c[ᴛɪᴄᴋᴇᴛs] El mensaje es demasiado corto. Describe tu problema."));
                        return 1;
                    }

                    TicketEntry ticket = DataStore.createTicket(
                        player.getUUID(), player.getName().getString(), message);

                    player.sendSystemMessage(Component.literal(
                        "§a[ᴛɪᴄᴋᴇᴛs] Tu ticket §b(#" + ticket.id + ")§a fue enviado al staff."));

                    // Notificar staff en turno
                    for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                        if (PermissionUtil.has(p, "staffmod.use") && DataStore.isOnDuty(p.getUUID())) {
                            p.sendSystemMessage(Component.literal(
                                "§e[ᴛɪᴄᴋᴇᴛs] §fNuevo ticket §b#" + ticket.id
                                + "§f de §b" + player.getName().getString() + "§f: §7" + message));
                            p.sendSystemMessage(Component.literal("§7Usa §e/staff §7→ Tickets para atenderlo."));
                        }
                    }

                    return 1;
                }))
        );
    }
}
