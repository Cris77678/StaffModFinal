package me.drex.staffmod.features;

import me.drex.staffmod.logging.AuditLogManager;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BuilderManager {

    private static final Map<UUID, ListTag> savedInventories = new ConcurrentHashMap<>();

    public static boolean isBuilderMode(UUID uuid) {
        return savedInventories.containsKey(uuid);
    }

    public static void toggleBuilderMode(ServerPlayer player) {
        UUID uuid = player.getUUID();

        if (isBuilderMode(uuid)) {
            // SALIR: restaurar inventario de supervivencia
            player.getInventory().clearContent();
            ListTag saved = savedInventories.remove(uuid);
            player.getInventory().load(saved);
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.sendSystemMessage(Component.literal("§c[ʙᴜɪʟᴅᴇʀ] Modo constructor desactivado. Inventario restaurado."));
            AuditLogManager.log(player.getName().getString(), "BUILDER_OFF", player.getName().getString(), "Salió de Builder Mode");
        } else {
            // ENTRAR: guardar inventario y cambiar a creativo
            ListTag currentInv = new ListTag();
            player.getInventory().save(currentInv);
            savedInventories.put(uuid, currentInv);
            player.getInventory().clearContent();
            player.setGameMode(GameType.CREATIVE);
            player.sendSystemMessage(Component.literal("§a[ʙᴜɪʟᴅᴇʀ] Modo constructor activado."));
            player.sendSystemMessage(Component.literal("§eAtención: §fÍtems peligrosos bloqueados (Bedrock, Command Blocks...)."));
            AuditLogManager.log(player.getName().getString(), "BUILDER_ON", player.getName().getString(), "Entró en Builder Mode");
        }
    }
}
