package me.drex.staffmod.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.staffmod.config.Kit;
import me.drex.staffmod.features.KitManager;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KitEditorGui extends SimpleGui {

    private final ServerPlayer staff;
    private final Kit kit;
    private final SimpleGui parent;
    private long currentCooldown;

    public KitEditorGui(ServerPlayer staff, Kit kit, SimpleGui parent) {
        super(MenuType.GENERIC_9x6, staff, false);
        this.staff = staff;
        this.kit = kit;
        this.parent = parent;
        this.currentCooldown = kit.cooldownSeconds;
        setTitle(Component.literal("§8❖ §eEditando Kit: §f" + kit.displayName));
        setLockPlayerInventory(false);
        loadExistingItems();
        build();
    }

    private void loadExistingItems() {
        if (kit.base64Inventory == null || kit.base64Inventory.isEmpty()) return;
        NonNullList<ItemStack> items = KitManager.deserializeItems(
            kit.base64Inventory, 36, staff.serverLevel().registryAccess());
        for (int i = 0; i < 36; i++) {
            if (!items.get(i).isEmpty()) {
                setSlot(i, items.get(i));
            }
        }
    }

    private void build() {
        // Fila 5: separador visual
        for (int i = 36; i < 45; i++) {
            setSlot(i, new GuiElementBuilder(Items.BLACK_STAINED_GLASS_PANE)
                .setName(Component.literal(" ")).build());
        }

        // Botón: Guardar
        setSlot(40, new GuiElementBuilder(Items.EMERALD_BLOCK)
            .setName(Component.literal("§a§lGUARDAR KIT"))
            .addLoreLine(Component.literal("§7Guarda los ítems de los 36 slots superiores."))
            .setCallback((idx, type, action, gui) -> saveKitAndClose())
            .build());

        // Cooldown -1h
        setSlot(38, new GuiElementBuilder(Items.REDSTONE)
            .setName(Component.literal("§c-1 Hora de Cooldown"))
            .setCallback((idx, type, action, gui) -> {
                currentCooldown = Math.max(0, currentCooldown - 3600);
                updateCooldownDisplay();
            }).build());

        // Cooldown +1h
        setSlot(42, new GuiElementBuilder(Items.GLOWSTONE_DUST)
            .setName(Component.literal("§a+1 Hora de Cooldown"))
            .setCallback((idx, type, action, gui) -> {
                currentCooldown += 3600;
                updateCooldownDisplay();
            }).build());

        updateCooldownDisplay();

        // Botón: Cancelar
        setSlot(36, new GuiElementBuilder(Items.BARRIER)
            .setName(Component.literal("§cCancelar y Volver"))
            .setCallback((idx, type, action, gui) -> {
                this.close();
                if (parent != null) parent.open();
            }).build());
    }

    private void updateCooldownDisplay() {
        long hours = currentCooldown / 3600;
        long mins  = (currentCooldown % 3600) / 60;
        setSlot(39, new GuiElementBuilder(Items.CLOCK)
            .setName(Component.literal("§eCooldown Actual"))
            .addLoreLine(Component.literal("§f" + hours + "h " + mins + "m §7(" + currentCooldown + "s)"))
            .build());
    }

    private void saveKitAndClose() {
        // Recoger los 36 slots superiores
        NonNullList<ItemStack> newItems = NonNullList.withSize(36, ItemStack.EMPTY);
        for (int i = 0; i < 36; i++) {
            // getSlot() puede retornar el GuiElementBuilder's item; usamos el slot directamente
            var slotStack = this.getSlot(i);
            if (slotStack != null) {
                ItemStack stack = slotStack.getItemStack();
                newItems.set(i, stack != null ? stack.copy() : ItemStack.EMPTY);
            }
        }

        kit.base64Inventory = KitManager.serializeItems(newItems, staff.serverLevel().registryAccess());
        kit.cooldownSeconds = currentCooldown;
        KitManager.createOrUpdateKit(kit);
        staff.sendSystemMessage(Component.literal(
            "§a[ᴋɪᴛs] Kit §f" + kit.displayName + " §aguardado correctamente."));
        this.close();
        if (parent != null) parent.open();
    }
}
