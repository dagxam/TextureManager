package me.dagxam.texturemanager;

import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Сканирует папку textures и проверяет PNG-файлы.
 */
public final class TextureScanner {

    private final JavaPlugin plugin;
    private final Path texturesFolder;

    public TextureScanner(JavaPlugin plugin, Path texturesFolder) {
        this.plugin = plugin;
        this.texturesFolder = texturesFolder;
    }

    public List<TextureInfo> scan() {
        List<TextureInfo> result = new ArrayList<>();

        if (!Files.exists(texturesFolder)) {
            return result;
        }

        try (Stream<Path> stream = Files.walk(texturesFolder)) {
            stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(file -> readTexture(file, result));
        } catch (IOException exception) {
            plugin.getLogger().severe("Не удалось прочитать папку текстур: " + exception.getMessage());
        }

        return result;
    }

    private void readTexture(Path file, List<TextureInfo> result) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".png")) {
            plugin.getLogger().warning("Файл пропущен, поддерживаются только PNG: " + file.getFileName());
            return;
        }

        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                plugin.getLogger().warning("Повреждённый или некорректный PNG: " + file);
                return;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) {
                plugin.getLogger().warning("Некорректный размер изображения: " + file);
                return;
            }

            if (plugin.getConfig().getBoolean("проверка-текстур.предупреждать-о-большом-размере", true)) {
                int warningSize = plugin.getConfig().getInt("проверка-текстур.предупреждение-при-размере", 1024);
                if (width >= warningSize || height >= warningSize) {
                    plugin.getLogger().warning("Большая текстура: " + file.getFileName() + " (" + width + "x" + height + ")");
                }
            }

            String relative = texturesFolder.relativize(file).toString().replace('\\', '/');
            result.add(new TextureInfo(file, relative, width, height));
        } catch (IOException exception) {
            plugin.getLogger().warning("Повреждённый или некорректный PNG: " + file + " — " + exception.getMessage());
        }
    }
}
