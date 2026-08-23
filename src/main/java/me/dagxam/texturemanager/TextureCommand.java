package me.dagxam.texturemanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TextureCommand implements CommandExecutor {
    private final TextureManagerPlugin plugin;

    public TextureCommand(TextureManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("texturemanager.admin")) {
            sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("texturemanager.reload")) return noPermission(sender);
                sender.sendMessage(plugin.color("&eНачинается пересборка ресурс-пака..."));
                boolean success = plugin.buildResourcePack(true);
                sender.sendMessage(plugin.color(success ? "&aПересборка успешно завершена." : "&cПересборка завершилась ошибкой. Смотрите консоль."));
            }
            case "status" -> status(sender);
            case "list" -> list(sender);
            case "send" -> sender.sendMessage(plugin.color("&eОтправка игрокам будет подключена следующим этапом после настройки URL ресурс-пака."));
            default -> sender.sendMessage(plugin.color(plugin.getMessage("неизвестная-команда", "&cНеизвестная подкоманда.")));
        }
        return true;
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
        return true;
    }

    private void status(CommandSender sender) {
        ResourcePackBuilder.BuildResult build = plugin.getLastBuild();
        sender.sendMessage(plugin.color("&6==== Состояние TextureManager ===="));
        sender.sendMessage(plugin.color("&7Версия Minecraft: &f" + plugin.getServer().getMinecraftVersion()));
        sender.sendMessage(plugin.color("&7Формат ресурс-пака: &f" + PackFormatResolver.resolvePackFormat()));
        sender.sendMessage(plugin.color("&7Текстур найдено: &f" + plugin.scanTextures().size()));
        if (build == null) {
            sender.sendMessage(plugin.color("&7Ресурс-пак: &cещё не собран"));
        } else {
            sender.sendMessage(plugin.color("&7Ресурс-пак: &aсобран"));
            sender.sendMessage(plugin.color("&7Размер: &f" + build.size() + " байт"));
            sender.sendMessage(plugin.color("&7SHA-1: &f" + build.sha1()));
        }
    }

    private void list(CommandSender sender) {
        List<TextureInfo> textures = plugin.scanTextures();
        sender.sendMessage(plugin.color("&6==== Установленные текстуры ===="));
        if (textures.isEmpty()) {
            sender.sendMessage(plugin.color("&7Текстуры ещё не добавлены."));
            return;
        }
        for (TextureInfo texture : textures) {
            sender.sendMessage(plugin.color("&7- &f" + texture.relativePath() + " &8(" + texture.width() + "x" + texture.height() + ")"));
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(plugin.color("&6==== TextureManager ===="));
        sender.sendMessage(plugin.color("&e/texture reload &7— проверить PNG и пересобрать ресурс-пак"));
        sender.sendMessage(plugin.color("&e/texture status &7— показать состояние и SHA-1"));
        sender.sendMessage(plugin.color("&e/texture list &7— показать все найденные текстуры"));
        sender.sendMessage(plugin.color("&e/texture send &7— отправить ресурс-пак игрокам"));
    }
}
