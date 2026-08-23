package me.dagxam.texturemanager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Главный класс TextureManager. */
public final class TextureManagerPlugin extends JavaPlugin {
    private Path texturesFolder, resourcePackFolder, backupFolder, packFile;
    private ResourcePackBuilder.BuildResult lastBuild;
    private ResourcePackHttpServer httpServer;
    private TextureFolderWatcher watcher;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPaths(); createPluginFolders();
        if (getCommand("texture") != null) getCommand("texture").setExecutor(new TextureCommand(this));
        if (getCommand("texturetarget") != null) getCommand("texturetarget").setExecutor(new TextureTargetCommand(this));
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);
        buildResourcePack(false);
        startServices();
        getLogger().info("Плагин запущен. Папка текстур: " + texturesFolder.toAbsolutePath());
    }

    private void startServices() {
        String mode = getConfig().getString("resource-pack.режим", "built-in");
        if ("built-in".equalsIgnoreCase(mode)) {
            try {
                int port = getConfig().getInt("resource-pack.встроенный-сервер.порт", 8080);
                httpServer = new ResourcePackHttpServer(this, packFile, port);
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

    public synchronized boolean buildResourcePack(boolean backupOld) {
        try {
            List<TextureInfo> textures = new TextureScanner(this, texturesFolder).scan();
            if (backupOld && Files.exists(packFile) && getConfig().getBoolean("резервные-копии.включены", true)) backupCurrentPack();
            lastBuild = new ResourcePackBuilder().build(packFile,
                    getConfig().getString("ресурс-пак.описание", "Пользовательский ресурс-пак сервера"),
                    PackFormatResolver.resolvePackFormat(), textures);
            Files.writeString(resourcePackFolder.resolve("sha1.txt"), lastBuild.sha1());
            getLogger().info("Ресурс-пак собран. Текстур: " + lastBuild.texturesCount() + ", SHA-1: " + lastBuild.sha1());
            return true;
        } catch (Exception exception) {
            getLogger().severe("Не удалось собрать ресурс-пак: " + exception.getMessage());
            return false;
        }
    }

    public void sendPack(Player player) {
        if (lastBuild == null) { player.sendMessage(color("&cРесурс-пак ещё не собран.")); return; }
        String url = getPackUrl();
        if (url.isBlank()) { player.sendMessage(color("&cНе указан внешний адрес ресурс-пака в config.yml.")); return; }
        try {
            boolean required = getConfig().getBoolean("resource-pack.обязательный", false);
            String prompt = getConfig().getString("resource-pack.сообщение", "Используется пользовательский ресурс-пак сервера.");
            player.setResourcePack(new URI(url), lastBuild.sha1().getBytes(), prompt, required);
        } catch (URISyntaxException exception) {
            getLogger().warning("Некорректный URL ресурс-пака: " + url);
        }
    }

    public String getPackUrl() {
        String mode = getConfig().getString("resource-pack.режим", "built-in");
        if ("external".equalsIgnoreCase(mode)) return getConfig().getString("resource-pack.внешний-url", "");
        String address = getConfig().getString("resource-pack.встроенный-сервер.внешний-адрес", "").trim();
        if (address.isEmpty()) return "";
        if (!address.startsWith("http://") && !address.startsWith("https://")) address = "http://" + address;
        return address + ":" + getConfig().getInt("resource-pack.встроенный-сервер.порt", 8080) + "/TextureManager.zip";
    }

    public void showTargetTexture(Player player, Material material, String path) {
        player.sendMessage(color("&6==== TextureManager ===="));
        player.sendMessage(color("&7Объект: &f" + material.getKey()));
        player.sendMessage(color("&7Стандартная текстура: &f" + path));
        player.sendMessage(color("&7Положите свою PNG сюда: &ftextures/" + path));
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
    private long lastModifiedSafe(Path path) { try { return Files.getLastModifiedTime(path).toMillis(); } catch (IOException e) { return 0L; } }
    private void loadPaths() { Path data=getDataFolder().toPath(); texturesFolder=data.resolve(getConfig().getString("папки.текстуры","textures")); resourcePackFolder=data.resolve(getConfig().getString("папки.ресурс-пак","resourcepack")); backupFolder=data.resolve(getConfig().getString("папки.резервные-копии","resourcepack/backup")); packFile=resourcePackFolder.resolve(getConfig().getString("ресурс-пак.имя-файла","TextureManager.zip")); }
    private void createPluginFolders() { try { Files.createDirectories(texturesFolder); Files.createDirectories(resourcePackFolder); Files.createDirectories(backupFolder); } catch(IOException e){ getLogger().severe("Не удалось создать папки: "+e.getMessage()); } }
    public Path getTexturesFolder(){return texturesFolder;} public ResourcePackBuilder.BuildResult getLastBuild(){return lastBuild;} public List<TextureInfo> scanTextures(){return new TextureScanner(this,texturesFolder).scan();} public String getMessage(String key,String fallback){return getConfig().getString("сообщения."+key,fallback);} public String color(String message){return ChatColor.translateAlternateColorCodes('&',message);}
}
