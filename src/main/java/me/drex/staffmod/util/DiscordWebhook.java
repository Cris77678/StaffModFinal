package me.drex.staffmod.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.drex.staffmod.StaffMod;
import me.drex.staffmod.core.StaffModAsync;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private static String webhookUrl = "";

    public static void setUrl(String url) {
        webhookUrl = url != null ? url : "";
    }

    public static void sendEmbed(String title, String description, int colorHex) {
        if (webhookUrl.isBlank() || webhookUrl.equals("AQUI_TU_WEBHOOK_URL")) return;
        StaffModAsync.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", colorHex);

                JsonArray embeds = new JsonArray();
                embeds.add(embed);

                JsonObject json = new JsonObject();
                json.add("embeds", embeds);
                json.addProperty("username", "StaffMod | Auditoría");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    StaffMod.LOGGER.warn("[StaffMod] Discord webhook respondió con código {}", code);
                }
            } catch (Exception e) {
                StaffMod.LOGGER.error("[StaffMod] Error enviando webhook Discord:", e);
            }
        });
    }
}
