package me.dagxam.texturemanager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Создаёт ресурс-пак потоково и безопасно заменяет готовый ZIP только после успешной сборки. */
public final class ResourcePackBuilder {
    private static final int BUFFER_SIZE = 32 * 1024;

    public BuildResult build(Path outputFile, String description, int packFormat, List<TextureInfo> textures) throws IOException {
        Files.createDirectories(outputFile.getParent());
        Path temporaryFile = outputFile.resolveSibling(outputFile.getFileName() + ".tmp");
        Files.deleteIfExists(temporaryFile);

        try {
            try (OutputStream rawOutput = new BufferedOutputStream(Files.newOutputStream(temporaryFile), BUFFER_SIZE);
                 ZipOutputStream zip = new ZipOutputStream(rawOutput)) {
                zip.setLevel(Deflater.DEFAULT_COMPRESSION);
                String meta = "{\n  \"pack\": {\n    \"pack_format\": " + packFormat + ",\n    \"description\": \"" + escapeJson(description) + "\"\n  }\n}\n";
                writeBytes(zip, "pack.mcmeta", meta.getBytes(StandardCharsets.UTF_8));

                byte[] buffer = new byte[BUFFER_SIZE];
                for (TextureInfo texture : textures) {
                    ZipEntry entry = new ZipEntry("assets/minecraft/textures/" + texture.relativePath());
                    zip.putNextEntry(entry);
                    try (InputStream input = new BufferedInputStream(Files.newInputStream(texture.source()), BUFFER_SIZE)) {
                        int read;
                        while ((read = input.read(buffer)) != -1) zip.write(buffer, 0, read);
                    }
                    zip.closeEntry();
                }
            }
            moveAtomicallyOrReplace(temporaryFile, outputFile);
        } catch (IOException exception) {
            Files.deleteIfExists(temporaryFile);
            throw exception;
        }

        String sha1 = calculateSha1(outputFile);
        return new BuildResult(outputFile, sha1, Files.size(outputFile), textures.size());
    }

    private void moveAtomicallyOrReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void writeBytes(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    public String calculateSha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = new BufferedInputStream(Files.newInputStream(file), BUFFER_SIZE)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            StringBuilder result = new StringBuilder(40);
            for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("В Java отсутствует алгоритм SHA-1.", exception);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public record BuildResult(Path file, String sha1, long size, int texturesCount) {}
}
