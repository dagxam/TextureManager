package me.dagxam.texturemanager;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Получает хеши официальных ассетов Mojang для версии запущенного Minecraft. */
public final class MojangAssetResolver {
    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final int TIMEOUT = 15000;

    public String resolveTextureHash(String minecraftPath) throws IOException {
        String version = Bukkit.getMinecraftVersion();
        String manifest = readUrl(VERSION_MANIFEST);
        String versionUrl = findVersionUrl(manifest, version);
        if (versionUrl == null) throw new IOException("Официальная версия " + version + " не найдена в манифесте Mojang.");

        String versionJson = readUrl(versionUrl);
        String assetIndexUrl = findAssetIndexUrl(versionJson);
        if (assetIndexUrl == null) throw new IOException("Не найден индекс ассетов для версии " + version + ".");

        String assets = readUrl(assetIndexUrl);
        String hash = findAssetHash(assets, minecraftPath);
        if (hash == null) throw new IOException("Стандартная текстура не найдена: " + minecraftPath);
        return hash;
    }

    private String findVersionUrl(String json, String version) {
        Pattern pattern = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"" + Pattern.quote(version) + "\\\"[^}]*?\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String findAssetIndexUrl(String json) {
        Pattern pattern = Pattern.compile("\\\"assetIndex\\\"\\s*:\\s*\\{.*?\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String findAssetHash(String json, String minecraftPath) {
        String key = Pattern.quote(minecraftPath);
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\{\\s*\\\"hash\\\"\\s*:\\s*\\\"([0-9a-f]{40})\\\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1).toLowerCase() : null;
    }

    private String readUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.setRequestProperty("User-Agent", "TextureManager/1.0");
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("Mojang HTTP " + connection.getResponseCode());
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[8192]; int read;
                while ((read = reader.read(buffer)) != -1) result.append(buffer, 0, read);
            }
            return result.toString();
        } finally { connection.disconnect(); }
    }
}
