package me.dagxam.texturemanager;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Главный класс TextureManager. Текстуры берутся только из папки плагина. */
public final class TextureManagerPlugin extends JavaPlugin {
    private Path texturesFolder, resourcePackFolder, backupFolder, packFile;
    private volatile ResourcePackBuilder.BuildResult lastBuild;
    private ResourcePackHttpServer httpServer;
    private TextureFolderWatcher watcher;
    private final AtomicBoolean buildRunning = new AtomicBoolean(false);
    private final AtomicBoolean rebuildQueued = new AtomicBoolean(false);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPaths();
        createPluginFolders();
        if (getCommand("texture") != null) getCommand("texture").setExecutor(new TextureCommand(this));
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);
        buildResourcePack(false);
        startServices();
        getLogger().info("Плагин запущен. Для замены текстур помещайте PNG в: " + texturesFolder.toAbsolutePath());
    }

    private void startServices() {
        if ("built-in".equalsIgnoreCase(getConfig().getString("resource-pack.режим", "built-in"))) {
            try {
                httpServer = new ResourcePackHttpServer(this, packFile, getConfig().getInt("resource-pack.встроенный-сервер.порт", 8080));
                httpServer.start();
            } catch (IOException exception) {
                getLogger().severe("Не удалось запустить встроенный HTTP-сервер: " + exception.getMessage());
            }
        }
        if (getConfig().getBoolean("горячая-перезагрузка.включена", true)) {
            watcher = new TextureFolderWatcher(this, texturesFolder);
            watcher.start();
        }
    }

    @Override
    public void onDisable() {
        if (watcher != null) watcher.stop();
        if (httpServer != null) httpServer.stop();
        HandlerList.unregisterAll(this);
        getLogger().info("Плагин остановлен.");
    }

    public void requestAsyncBuild(boolean backupOld) {
        if (!buildRunning.compareAndSet(false, true)) {
            rebuildQueued.set(true);
            return;
        }
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                do {
                    rebuildQueued.set(false);
                    buildResourcePack(backupOld);
                } while (rebuildQueued.get());
            } finally {
                buildRunning.set(false);
                if (rebuildQueued.getAndSet(false)) requestAsyncBuild(true);
            }
        });
    }

    public boolean buildResourcePack(boolean backupOld) {
        try {
            List<TextureInfo> textures = new TextureScanner(this, texturesFolder).scan();
            if (backupOld && Files.exists(packFile) && getConfig().getBoolean("резервные-копии.включены", true)) backupCurrentPack();
            lastBuild = new ResourcePackBuilder().build(
                    packFile,
                    getConfig().getString("ресурс-пак.описание", "Пользовательский ресурс-пак сервера"),
                    PackFormatResolver.resolvePackFormat(),
                    textures
            );
            Files.writeString(resourcePackFolder.resolve("sha1.txt"), lastBuild.sha1(), StandardCharsets.UTF_8);
            getLogger().info("Ресурс-пак обновлён. Пользовательских текстур: " + lastBuild.texturesCount() + ", SHA-1: " + lastBuild.sha1());
            return true;
        } catch (Exception exception) {
            getLogger().severe("Не удалось собрать ресурс-пак: " + exception.getMessage());
            return false;
        }
    }

    public void sendPack(Player player) {
        ResourcePackBuilder.BuildResult build = lastBuild;
        if (build == null) return;
        String url = getPackUrl();
        if (url.isBlank()) {
            getLogger().warning("Ресурс-пак не отправлен игроку " + player.getName() + ": не указан внешний адрес.");
            return;
        }
        try {
            player.setResourcePack(
                    url,
                    build.sha1().getBytes(StandardCharsets.UTF_8),
                    Component.text(getConfig().getString("resource-pack.сообщение", "Используется пользовательский ресурс-пак сервера.")),
                    getConfig().getBoolean("resource-pack.обязательный", false)
            );
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Некорректный URL ресурс-пака: " + url);
        }
    }

    public String getPackUrl() {
        if ("external".equalsIgnoreCase(getConfig().getString("resource-pack.режим", "built-in"))) {
            return getConfig().getString("resource-pack.внешний-url", "").trim();
        }
        String address = getConfig().getString("resource-pack.встроенный-сервер.внешний-адрес", "").trim();
        if (address.isEmpty()) return "";
        if (!address.startsWith("http://") && !address.startsWith("https://")) address = "http://" + address;
        return address + ":" + getConfig().getInt("resource-pack.встроенный-сервер.порт", 8080) + "/TextureManager.zip";
    }

    private void backupCurrentPack() throws IOException {
        Files.createDirectories(backupFolder);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        Files.copy(packFile, backupFolder.resolve("TextureManager-" + timestamp + ".zip"), StandardCopyOption.REPLACE_EXISTING);
        int maxFiles = getConfig().getInt("резервные-копии.максимум-файлов", 10);
        try (var stream = Files.list(backupFolder)) {
            List<Path> backups = stream.filter(Files::isRegularFile).sorted(Comparator.comparingLong(this::lastModifiedSafe).reversed()).toList();
            for (int i = maxFiles; i < backups.size(); i++) Files.deleteIfExists(backups.get(i));
        }
    }

    private long lastModifiedSafe(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException exception) { return 0L; }
    }

    private void loadPaths() {
        Path data = getDataFolder().toPath();
        texturesFolder = data.resolve(getConfig().getString("папки.текстуры", "textures"));
        resourcePackFolder = data.resolve(getConfig().getString("папки.ресурс-пак", "resourcepack"));
        backupFolder = data.resolve(getConfig().getString("папки.резервные-копии", "resourcepack/backup"));
        packFile = resourcePackFolder.resolve(getConfig().getString("ресурс-пак.имя-файла", "TextureManager.zip"));
    }

    private void createPluginFolders() {
        try {
            Files.createDirectories(texturesFolder);
            Files.createDirectories(resourcePackFolder);
            Files.createDirectories(backupFolder);
        } catch (IOException exception) {
            getLogger().severe("Не удалось создать папки: " + exception.getMessage());
        }
    }

    public Path getTexturesFolder() { return texturesFolder; }
    public ResourcePackBuilder.BuildResult getLastBuild() { return lastBuild; }
    public List<TextureInfo> scanTextures() { return new TextureScanner(this, texturesFolder).scan(); }
    public String getMessage(String key, String fallback) { return getConfig().getString("сообщения." + key, fallback); }
    public String color(String message) { return ChatColor.translateAlternateColorCodes('&', message); }
}
