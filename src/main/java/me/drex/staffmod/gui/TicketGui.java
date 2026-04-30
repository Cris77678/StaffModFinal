package me.drex.staffmod.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.staffmod.config.DataStore;
import me.drex.staffmod.config.TicketEntry;
import me.drex.staffmod.logging.AuditLogManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TicketGui extends SimpleGui {

    private final ServerPlayer staff;
    private final SimpleGui parent;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 44;

    public TicketGui(ServerPlayer staff, SimpleGui parent) {
        super(MenuType.GENERIC_9x6, staff, false);
        this.staff = staff;
        this.parent = parent;
        setTitle(Component.literal("§8❖ §eGestión de ᴛɪᴄᴋᴇᴛs §8❖"));
        build();
    }

    private void openReplyAnvil(TicketEntry ticket) {
        eu.pb4.sgui.api.gui.AnvilInputGui anvil = new eu.pb4.sgui.api.gui.AnvilInputGui(staff, false);
        anvil.setTitle(Component.literal("Respuesta al Ticket #" + ticket.id));
        anvil.setDefaultText("Escribe tu respuesta...");
        
        anvil.setConfirmCallback((reply) -> {
            if (reply.length() < 3 || reply.equals("Escribe tu respuesta...")) return;
            
            String format = "§c[sᴛᴀꜰꜰ] §f" + staff.getName().getString() + "§7: §e" + reply;
            ticket.replies.add(format);
            ticket.hasUnreadReply = true;
            ticket.status = "TOMADO";
            ticket.handledBy = staff.getName().getString();
            
            DataStore.updateTicket(ticket);
            
            ServerPlayer target = staff.getServer().getPlayerList().getPlayer(ticket.creatorUuid);
            if (target != null) {
                target.sendSystemMessage(Component.literal(" "));
                target.sendSystemMessage(Component.literal("§a§l¡El staff ha respondido a tu ticket #" + ticket.id + "!"));
                target.sendSystemMessage(Component.literal(format));
                target.sendSystemMessage(Component.literal("§7Usa §e/ticket reply " + ticket.id + " <mensaje> §7para contestar."));
                target.sendSystemMessage(Component.literal(" "));
                ticket.hasUnreadReply = false;
                DataStore.updateTicket(ticket);
            }
            
            staff.sendSystemMessage(Component.literal("§a[ᴛɪᴄᴋᴇᴛs] Respuesta enviada con éxito al ticket #" + ticket.id));
            
            // Volver a abrir el TicketGui
            net.minecraft.server.MinecraftServer server = staff.getServer();
            if(server != null) {
               server.execute(this::open);
            }
        });
        
        anvil.open();
    }

    private void build() {
        for (int i = 0; i < getSize(); i++) clearSlot(i);

        for (int i = 45; i < 54; i++) {
            setSlot(i, new GuiElementBuilder(Items.BLACK_STAINED_GLASS_PANE)
                .setName(Component.literal(" ")).build());
        }

        List<TicketEntry> sorted = DataStore.getAllTickets().stream()
            .sorted(Comparator.<TicketEntry, Integer>comparing(t -> "ABIERTO".equals(t.status) ? 0 : 1)
                .thenComparingInt(t -> -t.id))
            .collect(Collectors.toList());

        int maxPages = Math.max(1, (int) Math.ceil((double) sorted.size() / PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, maxPages - 1));
        int start = currentPage * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, sorted.size());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");

        for (int i = start; i < end; i++) {
            TicketEntry t = sorted.get(i);
            int slot = i - start;

            var item = "ABIERTO".equals(t.status) ? Items.WRITABLE_BOOK
                : "TOMADO".equals(t.status) ? Items.WRITTEN_BOOK
                : Items.BOOK;

            String statusColor = "ABIERTO".equals(t.status) ? "§a" : "TOMADO".equals(t.status) ? "§e" : "§7";
            String dateStr = t.createdAt > 0 ? sdf.format(new Date(t.createdAt)) : "?";

            GuiElementBuilder btn = new GuiElementBuilder(item)
                .setName(Component.literal("§f§lTicket §b#" + t.id + " §8— §f" + t.creatorName))
                .addLoreLine(Component.literal("§7Estado: " + statusColor + t.status))
                .addLoreLine(Component.literal("§7Fecha: §f" + dateStr))
                .addLoreLine(Component.literal("§7Mensaje original: §e" + t.message));

            if (t.replies != null && !t.replies.isEmpty()) {
                btn.addLoreLine(Component.literal(" "));
                btn.addLoreLine(Component.literal("§aÚltimos mensajes:"));
                int startReply = Math.max(0, t.replies.size() - 5);
                for (int j = startReply; j < t.replies.size(); j++) {
                    btn.addLoreLine(Component.literal(t.replies.get(j)));
                }
            }

            btn.addLoreLine(Component.literal(" "));
            btn.addLoreLine(Component.literal("§8(O responde en chat: §e/ticket reply " + t.id + " <msg>§8)"));
            btn.addLoreLine(Component.literal(" "));

            if ("ABIERTO".equals(t.status)) {
                btn.addLoreLine(Component.literal("§aClick izq: §fTomar ticket"));
                btn.addLoreLine(Component.literal("§cClick der: §fCerrar ticket"));
            } else if ("TOMADO".equals(t.status)) {
                btn.addLoreLine(Component.literal("§7Atendido por: §f" + (t.handledBy == null || t.handledBy.isEmpty() ? "?" : t.handledBy)));
                btn.addLoreLine(Component.literal("§bClick izq: §fEscribir respuesta"));
                btn.addLoreLine(Component.literal("§cClick der: §fCerrar ticket"));
            }

            final TicketEntry ticket = t;
            btn.setCallback((idx, type, action, gui) -> {
                if (action == net.minecraft.world.inventory.ClickType.PICKUP
                    || action == net.minecraft.world.inventory.ClickType.QUICK_MOVE) {
                    if ("ABIERTO".equals(ticket.status)) {
                        ticket.status = "TOMADO";
                        ticket.handledBy = staff.getName().getString();
                        DataStore.updateTicket(ticket);
                        AuditLogManager.log(staff.getName().getString(), "TICKET_TAKE",
                            ticket.creatorName, "Ticket #" + ticket.id);
                        staff.sendSystemMessage(Component.literal(
                            "§a[ᴛɪᴄᴋᴇᴛs] Tomaste el ticket §b#" + ticket.id + " §ade §f" + ticket.creatorName));
                        build();
                    } else if ("TOMADO".equals(ticket.status)) {
                        openReplyAnvil(ticket);
                    }
                } else {
                    ticket.status = "CERRADO";
                    DataStore.updateTicket(ticket);
                    AuditLogManager.log(staff.getName().getString(), "TICKET_CLOSE",
                        ticket.creatorName, "Ticket #" + ticket.id);
                    staff.sendSystemMessage(Component.literal(
                        "§7[ᴛɪᴄᴋᴇᴛs] Ticket §b#" + ticket.id + " §7cerrado."));
                    build();
                }
            });

            setSlot(slot, btn.build());
        }

        if (currentPage > 0) {
            setSlot(45, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("§e◄ Anterior"))
                .setCallback((i, t, a, g) -> { currentPage--; build(); }).build());
        }
        setSlot(49, new GuiElementBuilder(Items.PAPER)
            .setName(Component.literal("§aPágina §f" + (currentPage + 1) + " §ade §f" + maxPages))
            .addLoreLine(Component.literal("§7Total tickets: §f" + sorted.size()))
            .build());
        if (currentPage < maxPages - 1) {
            setSlot(53, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("§eSiguiente ►"))
                .setCallback((i, t, a, g) -> { currentPage++; build(); }).build());
        }
        setSlot(47, new GuiElementBuilder(Items.DARK_OAK_DOOR)
            .setName(Component.literal("§c◄ Volver"))
            .setCallback((i, t, a, g) -> parent.open()).build());
    }
}
