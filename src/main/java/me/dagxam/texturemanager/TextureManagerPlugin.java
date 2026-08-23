package me.dagxam.texturemanager;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Главный класс TextureManager.
 *
 * <p>Все сообщения, которые видит администратор, по умолчанию русские.
 * Пользовательские PNG берутся из папки plugins/TextureManager/textures/.</p>
 */
public final class TextureManagerPlugin extends JavaPlugin {

    private Path texturesFolder;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        texturesFolder = getDataFolder().toPath().resolve(
                getConfig().getString("папки.текстуры", "textures")
        );

        createPluginFolders();

        if (getCommand("texture") != null) {
            getCommand("texture").setExecutor(new TextureCommand(this));
        } else {
            getLogger().severe("Команда texture не найдена в plugin.yml.");
        }

        getLogger().info(color(getMessage("запуск", "Плагин запущен.")));
        getLogger().info("Папка пользовательских текстур: " + texturesFolder.toAbsolutePath());
    }

    @Override
    public void onDisable() {
        getLogger().info(color(getMessage("остановка", "Плагин остановлен.")));
    }

    private void createPluginFolders() {
        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.createDirectories(texturesFolder);
            Files.createDirectories(getDataFolder().toPath().resolve(
                    getConfig().getString("папки.ресурс-пак", "resourcepack")
            ));
            Files.createDirectories(getDataFolder().toPath().resolve(
                    getConfig().getString("папки.резервные-копии", "resourcepack/backup")
            ));
        } catch (Exception exception) {
            getLogger().severe("Не удалось создать необходимые папки TextureManager: " + exception.getMessage());
        }
    }

    public Path getTexturesFolder() {
        return texturesFolder;
    }

    public String getMessage(String key, String fallback) {
        return getConfig().getString("сообщения." + key, fallback);
    }

    public String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
