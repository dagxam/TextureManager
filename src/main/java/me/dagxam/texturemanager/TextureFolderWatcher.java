package me.dagxam.texturemanager;

import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/** Периодически отслеживает изменения файлов в папке текстур. */
public final class TextureFolderWatcher {
    private final TextureManagerPlugin plugin;
    private final Path folder;
    private final AtomicLong fingerprint = new AtomicLong();
    private BukkitTask task;
    private long changedAt = -1L;

    public TextureFolderWatcher(TextureManagerPlugin plugin, Path folder) {
        this.plugin = plugin;
        this.folder = folder;
    }

    public void start() {
        long interval = Math.max(1, plugin.getConfig().getLong("горячая-перезагрузка.интервал-секунд", 3));
        fingerprint.set(calculateFingerprint());
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::check, interval * 20L, interval * 20L);
        plugin.getLogger().info("Горячее отслеживание папки текстур включено.");
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void check() {
        long current = calculateFingerprint();
        if (current != fingerprint.get()) {
            fingerprint.set(current);
            changedAt = System.currentTimeMillis();
            plugin.getLogger().info("Обнаружено изменение в папке текстур. Ожидается завершение копирования файлов...");
            return;
        }
        if (changedAt < 0L) return;
        long delay = Math.max(1, plugin.getConfig().getLong("горячая-перезагрузка.задержка-секунд", 2)) * 1000L;
        if (System.currentTimeMillis() - changedAt >= delay) {
            changedAt = -1L;
            plugin.getLogger().info("Файлы стабилизированы. Выполняется автоматическая пересборка.");
            plugin.buildResourcePack(true);
        }
    }

    private long calculateFingerprint() {
        if (!Files.exists(folder)) return 0L;
        try (var stream = Files.walk(folder)) {
            return stream.filter(Files::isRegularFile).mapToLong(this::fileFingerprint).reduce(17L, (a, b) -> 31L * a + b);
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось проверить изменения текстур: " + exception.getMessage());
            return System.nanoTime();
        }
    }

    private long fileFingerprint(Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return path.toString().hashCode() * 31L + attributes.size() * 17L + attributes.lastModifiedTime().toMillis();
        } catch (IOException exception) {
            return path.toString().hashCode();
        }
    }
}
