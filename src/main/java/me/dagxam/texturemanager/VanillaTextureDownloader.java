package me.dagxam.texturemanager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Загружает стандартные текстуры Minecraft из официального CDN Mojang.
 * Скачивание выполняется только по явному вызову команды /tt.
 */
public final class VanillaTextureDownloader {
    private static final String ASSET_BASE = "https://resources.download.minecraft.net/";
    private static final int TIMEOUT = 15000;

    public DownloadResult download(Path texturesFolder, String relativePath, String assetHash) throws IOException {
        if (assetHash == null || assetHash.length() < 2) {
            throw new IOException("Не удалось определить хеш стандартной текстуры.");
        }

        Path target = texturesFolder.resolve(relativePath).normalize();
        if (!target.startsWith(texturesFolder.normalize())) {
            throw new IOException("Недопустимый путь сохранения текстуры.");
        }

        Files.createDirectories(target.getParent());
        String url = ASSET_BASE + assetHash.substring(0, 2) + "/" + assetHash;
        Path temporary = target.resolveSibling(target.getFileName() + ".download");
        Files.deleteIfExists(temporary);

        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.setRequestProperty("User-Agent", "TextureManager/1.0");

        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Сервер Mojang вернул HTTP " + code + ".");
            }
            try (InputStream input = connection.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!isPng(temporary)) {
                throw new IOException("Скачанный файл не является корректной PNG-текстурой.");
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            return new DownloadResult(target, Files.size(target), false);
        } finally {
            Files.deleteIfExists(temporary);
            connection.disconnect();
        }
    }

    private boolean isPng(Path file) throws IOException {
        byte[] signature = new byte[8];
        try (InputStream input = Files.newInputStream(file)) {
            if (input.read(signature) != 8) return false;
        }
        return signature[0] == (byte) 0x89 && signature[1] == 0x50 && signature[2] == 0x4E && signature[3] == 0x47
                && signature[4] == 0x0D && signature[5] == 0x0A && signature[6] == 0x1A && signature[7] == 0x0A;
    }

    public record DownloadResult(Path file, long size, boolean alreadyExists) {}
}
