package me.dagxam.texturemanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Создаёт ZIP ресурс-пака из простых папок пользователя.
 */
public final class ResourcePackBuilder {

    public BuildResult build(Path outputFile, String description, int packFormat, List<TextureInfo> textures) throws IOException {
        Files.createDirectories(outputFile.getParent());

        try (OutputStream output = Files.newOutputStream(outputFile);
             ZipOutputStream zip = new ZipOutputStream(output)) {

            String meta = "{\n" +
                    "  \"pack\": {\n" +
                    "    \"pack_format\": " + packFormat + ",\n" +
                    "    \"description\": \"" + escapeJson(description) + "\"\n" +
                    "  }\n" +
                    "}\n";
            writeBytes(zip, "pack.mcmeta", meta.getBytes(StandardCharsets.UTF_8));

            for (TextureInfo texture : textures) {
                String entryName = "assets/minecraft/textures/" + texture.relativePath();
                ZipEntry entry = new ZipEntry(entryName);
                zip.putNextEntry(entry);
                try (InputStream input = Files.newInputStream(texture.source())) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
            }
        }

        String sha1 = calculateSha1(outputFile);
        return new BuildResult(outputFile, sha1, Files.size(outputFile), textures.size());
    }

    private void writeBytes(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    public String calculateSha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) {
                result.append(String.format(Locale.ROOT, "%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("В Java отсутствует алгоритм SHA-1.", exception);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public record BuildResult(Path file, String sha1, long size, int texturesCount) {
    }
}
