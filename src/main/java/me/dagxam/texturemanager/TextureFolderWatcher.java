package me.dagxam.texturemanager;

import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Лёгкое асинхронное отслеживание изменений в папке текстур. */
public final class TextureFolderWatcher {
    private final TextureManagerPlugin plugin;
    private final Path folder;
    private final AtomicLong fingerprint = new AtomicLong();
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private BukkitTask task;
    private volatile long changedAt = -1L;

    public TextureFolderWatcher(TextureManagerPlugin plugin, Path folder) {
        this.plugin = plugin;
        this.folder = folder;
    }

    public void start() {
        fingerprint.set(calculateFingerprint());
        long interval = Math.max(1, plugin.getConfig().getLong("горячая-перезагрузка.интервал-секунд", 3));
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::checkAsync, interval * 20L, interval * 20L);
        plugin.getLogger().info("Горячее отслеживание папки текстур включено в асинхронном режиме.");
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void checkAsync() {
        if (!checking.compareAndSet(false, true)) return;
        try {
            long current = calculateFingerprint();
            if (current != fingerprint.getAndSet(current)) {
                changedAt = System.currentTimeMillis();
                return;
            }
            if (changedAt < 0L) return;
            long delay = Math.max(1, plugin.getConfig().getLong("горячая-перезагрузка.задержка-секунд", 2)) * 1000L;
            if (System.currentTimeMillis() - changedAt >= delay) {
                changedAt = -1L;
                plugin.requestAsyncBuild(true);
            }
        } finally {
            checking.set(false);
        }
    }

    private long calculateFingerprint() {
        if (!Files.exists(folder)) return 0L;
        try (var stream = Files.walk(folder)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(this::fileFingerprint)
                    .reduce(17L, (a, b) -> 31L * a + b);
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось проверить изменения текстур: " + exception.getMessage());
            return fingerprint.get();
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
