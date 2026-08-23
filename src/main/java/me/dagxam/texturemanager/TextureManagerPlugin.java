package me.dagxam.texturemanager;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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

/** Главный класс TextureManager. */
public final class TextureManagerPlugin extends JavaPlugin {
    private Path texturesFolder, resourcePackFolder, backupFolder, packFile;
    private volatile ResourcePackBuilder.BuildResult lastBuild;
    private ResourcePackHttpServer httpServer;
    private TextureFolderWatcher watcher;
    private final AtomicBoolean buildRunning = new AtomicBoolean(false);
    private final AtomicBoolean rebuildQueued = new AtomicBoolean(false);
    private final MojangAssetResolver assetResolver = new MojangAssetResolver();
    private final VanillaTextureDownloader textureDownloader = new VanillaTextureDownloader();

    @Override public void onEnable() { saveDefaultConfig(); loadPaths(); createPluginFolders(); if (getCommand("texture") != null) getCommand("texture").setExecutor(new TextureCommand(this)); if (getCommand("texturetarget") != null) getCommand("texturetarget").setExecutor(new TextureTargetCommand(this)); getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this); buildResourcePack(false); startServices(); getLogger().info("Плагин запущен. Папка текстур: " + texturesFolder.toAbsolutePath()); }
    private void startServices() { if ("built-in".equalsIgnoreCase(getConfig().getString("resource-pack.режим", "built-in"))) try { httpServer = new ResourcePackHttpServer(this, packFile, getConfig().getInt("resource-pack.встроенный-сервер.порт", 8080)); httpServer.start(); } catch (IOException e) { getLogger().severe("Не удалось запустить встроенный HTTP-сервер: " + e.getMessage()); } if (getConfig().getBoolean("горячая-перезагрузка.включена", true)) { watcher = new TextureFolderWatcher(this, texturesFolder); watcher.start(); } }
    @Override public void onDisable() { if (watcher != null) watcher.stop(); if (httpServer != null) httpServer.stop(); HandlerList.unregisterAll(this); getLogger().info("Плагин остановлен."); }

    public void requestAsyncBuild(boolean backupOld) { if (!buildRunning.compareAndSet(false, true)) { rebuildQueued.set(true); return; } getServer().getScheduler().runTaskAsynchronously(this, () -> { try { do { rebuildQueued.set(false); buildResourcePack(backupOld); } while (rebuildQueued.get()); } finally { buildRunning.set(false); if (rebuildQueued.getAndSet(false)) requestAsyncBuild(true); } }); }
    public boolean buildResourcePack(boolean backupOld) { try { List<TextureInfo> textures = new TextureScanner(this, texturesFolder).scan(); if (backupOld && Files.exists(packFile) && getConfig().getBoolean("резервные-копии.включены", true)) backupCurrentPack(); lastBuild = new ResourcePackBuilder().build(packFile, getConfig().getString("ресурс-пак.описание", "Пользовательский ресурс-пак сервера"), PackFormatResolver.resolvePackFormat(), textures); Files.writeString(resourcePackFolder.resolve("sha1.txt"), lastBuild.sha1(), StandardCharsets.UTF_8); getLogger().info("Ресурс-пак собран. Текстур: " + lastBuild.texturesCount() + ", SHA-1: " + lastBuild.sha1()); return true; } catch (Exception e) { getLogger().severe("Не удалось собрать ресурс-пак: " + e.getMessage()); return false; } }

    public void saveTargetTexture(Player player, Material material, String relativePath) {
        String assetPath = "minecraft/textures/" + relativePath;
        player.sendMessage(color("&eЗагрузка стандартной текстуры &f" + material.getKey() + "&e..."));
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Path target = texturesFolder.resolve(relativePath);
                if (Files.exists(target)) {
                    getServer().getScheduler().runTask(this, () -> player.sendMessage(color("&aТекстура уже сохранена: &ftextures/" + relativePath)));
                    return;
                }
                String hash = assetResolver.resolveTextureHash(assetPath);
                VanillaTextureDownloader.DownloadResult result = textureDownloader.download(texturesFolder, relativePath, hash);
                getServer().getScheduler().runTask(this, () -> player.sendMessage(color("&aТекстура сохранена: &ftextures/" + relativePath + " &7(" + result.size() + " байт)")));
                requestAsyncBuild(true);
            } catch (Exception e) {
                getLogger().warning("Не удалось сохранить стандартную текстуру " + relativePath + ": " + e.getMessage());
                getServer().getScheduler().runTask(this, () -> player.sendMessage(color("&cНе удалось сохранить стандартную текстуру: &f" + e.getMessage())));
            }
        });
    }

    public void sendPack(Player player) { ResourcePackBuilder.BuildResult build=lastBuild; if(build==null)return; String url=getPackUrl(); if(url.isBlank()){getLogger().warning("Ресурс-пак не отправлен игроку "+player.getName()+": не указан внешний адрес.");return;} try{player.setResourcePack(url,build.sha1().getBytes(StandardCharsets.UTF_8),Component.text(getConfig().getString("resource-pack.сообщение","Используется пользовательский ресурс-пак сервера.")),getConfig().getBoolean("resource-pack.обязательный",false));}catch(IllegalArgumentException e){getLogger().warning("Некорректный URL ресурс-пака: "+url);} }
    public String getPackUrl(){if("external".equalsIgnoreCase(getConfig().getString("resource-pack.режим","built-in")))return getConfig().getString("resource-pack.внешний-url","").trim();String address=getConfig().getString("resource-pack.встроенный-сервер.внешний-адрес","").trim();if(address.isEmpty())return "";if(!address.startsWith("http://")&&!address.startsWith("https://"))address="http://"+address;return address+":"+getConfig().getInt("resource-pack.встроенный-сервер.порт",8080)+"/TextureManager.zip";}
    public void showTargetTexture(Player player, Material material, String path){saveTargetTexture(player,material,path);} public void showBlockTexture(Player player,Block block){Material m=block.getType();saveTargetTexture(player,m,"block/"+m.getKey().getKey()+".png");} public void showEntityTexture(Player player,Entity entity){player.sendMessage(color("&6==== TextureManager ===="));player.sendMessage(color("&7Сущность: &f"+entity.getType().getKey()));player.sendMessage(color("&eАвтоматическое сохранение мобов будет добавлено отдельным точным сопоставлением текстур."));}
    private void backupCurrentPack() throws IOException{Files.createDirectories(backupFolder);String t=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));Files.copy(packFile,backupFolder.resolve("TextureManager-"+t+".zip"),StandardCopyOption.REPLACE_EXISTING);int max=getConfig().getInt("резервные-копии.максимум-файлов",10);try(var s=Files.list(backupFolder)){List<Path>b=s.filter(Files::isRegularFile).sorted(Comparator.comparingLong(this::lastModifiedSafe).reversed()).toList();for(int i=max;i<b.size();i++)Files.deleteIfExists(b.get(i));}}
    private long lastModifiedSafe(Path p){try{return Files.getLastModifiedTime(p).toMillis();}catch(IOException e){return 0L;}} private void loadPaths(){Path d=getDataFolder().toPath();texturesFolder=d.resolve(getConfig().getString("папки.текстуры","textures"));resourcePackFolder=d.resolve(getConfig().getString("папки.ресурс-пак","resourcepack"));backupFolder=d.resolve(getConfig().getString("папки.резервные-копии","resourcepack/backup"));packFile=resourcePackFolder.resolve(getConfig().getString("ресурс-пак.имя-файла","TextureManager.zip"));} private void createPluginFolders(){try{Files.createDirectories(texturesFolder);Files.createDirectories(resourcePackFolder);Files.createDirectories(backupFolder);}catch(IOException e){getLogger().severe("Не удалось создать папки: "+e.getMessage());}}
    public Path getTexturesFolder(){return texturesFolder;} public ResourcePackBuilder.BuildResult getLastBuild(){return lastBuild;} public List<TextureInfo> scanTextures(){return new TextureScanner(this,texturesFolder).scan();} public String getMessage(String key,String fallback){return getConfig().getString("сообщения."+key,fallback);} public String color(String message){return ChatColor.translateAlternateColorCodes('&',message);}
}
