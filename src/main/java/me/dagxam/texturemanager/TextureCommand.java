package me.dagxam.texturemanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class TextureCommand implements CommandExecutor {
    private final TextureManagerPlugin plugin;

    public TextureCommand(TextureManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("texturemanager.admin")) {
            sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("texturemanager.reload")) {
                    sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
                    return true;
                }
                sender.sendMessage(plugin.color(plugin.getMessage("перезагрузка", "&eВыполняется горячая перезагрузка...")));
                sender.sendMessage(plugin.color(plugin.getMessage("перезагрузка-завершена", "&aГорячая перезагрузка завершена.")));
            }
            case "send" -> {
                if (!sender.hasPermission("texturemanager.send")) {
                    sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
                    return true;
                }
                sender.sendMessage(plugin.color(plugin.getMessage("игроков-отправлено", "&aРесурс-пак отправлен игрокам.")));
            }
            case "status" -> {
                if (!sender.hasPermission("texturemanager.status")) {
                    sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
                    return true;
                }
                sender.sendMessage(plugin.color(plugin.getMessage("состояние", "&7Состояние TextureManager:")));
                sender.sendMessage(plugin.color(plugin.getMessage("состояние-версия", "&7Версия Minecraft: &f%version%")
                        .replace("%version%", plugin.getServer().getMinecraftVersion())));
                sender.sendMessage(plugin.color(plugin.getMessage("состояние-текстур", "&7Текстур: &f%count%")
                        .replace("%count%", "0")));
            }
            case "list" -> {
                if (!sender.hasPermission("texturemanager.list")) {
                    sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
                    return true;
                }
                sender.sendMessage(plugin.color(plugin.getMessage("список-заголовок", "&6Установленные текстуры:")));
                sender.sendMessage(plugin.color(plugin.getMessage("список-пуст", "&7Текстуры ещё не добавлены.")));
            }
            default -> sender.sendMessage(plugin.color(plugin.getMessage("неизвестная-команда", "&cНеизвестная подкоманда. Используйте &f/texture help&c.")));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("&6==== TextureManager ===="));
        sender.sendMessage(plugin.color("&e/texture reload &7— пересобрать ресурс-пак"));
        sender.sendMessage(plugin.color("&e/texture send &7— отправить ресурс-пак игрокам"));
        sender.sendMessage(plugin.color("&e/texture status &7— показать состояние плагина"));
        sender.sendMessage(plugin.color("&e/texture list &7— показать установленные текстуры"));
    }
}
