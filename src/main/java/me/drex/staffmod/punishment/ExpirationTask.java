package me.drex.staffmod.punishment;

import me.drex.staffmod.config.DataStore;
import me.drex.staffmod.config.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tarea asíncrona de expiración de castigos.
 * Detecta expiraciones en async; ejecuta acciones de mundo via server.execute().
 */
public class ExpirationTask implements Runnable {

    private final MinecraftServer server;

    public ExpirationTask(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        // snapshot de online players (seguro, es una copia)
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerData pd = DataStore.get(player.getUUID());
            if (pd == null) continue;

            // Mute
            if (pd.muted && pd.muteExpiry != -1 && now >= pd.muteExpiry) {
                pd.muted = false;
                DataStore.saveAsync();
                server.execute(() ->
                    player.sendSystemMessage(Component.literal(
                        "§a[sᴛᴀꜰꜰ] Tu silencio ha expirado. Ya puedes hablar de nuevo.")));
            }

            // Jail
            if (pd.jailed && pd.jailExpiry != -1 && now >= pd.jailExpiry) {
                pd.jailed = false;
                pd.jailName = "";
                DataStore.saveAsync();
                server.execute(() -> {
                    var overworld = server.overworld();
                    var spawn = overworld.getSharedSpawnPos();
                    player.teleportTo(overworld, spawn.getX(), spawn.getY(), spawn.getZ(),
                        player.getYRot(), player.getXRot());
                    player.sendSystemMessage(Component.literal(
                        "§a[sᴛᴀꜰꜰ] Has cumplido tu tiempo en prisión. Eres libre."));
                });
            }

            // Ban (si estaban online con un ban temporal que acaba de expirar)
            if (pd.banned && pd.banExpiry != -1 && now >= pd.banExpiry) {
                pd.banned = false;
                DataStore.saveAsync();
            }
        }
    }
}
