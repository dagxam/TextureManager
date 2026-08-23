package me.dagxam.texturemanager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TextureCommand implements CommandExecutor {
    private final TextureManagerPlugin plugin;
    public TextureCommand(TextureManagerPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "reload" -> { if (!sender.hasPermission("texturemanager.reload")) return noPermission(sender); rebuild(sender); }
            case "status" -> { if (!sender.hasPermission("texturemanager.status")) return noPermission(sender); status(sender); }
            case "list" -> { if (!sender.hasPermission("texturemanager.list")) return noPermission(sender); list(sender); }
            case "send" -> { if (!sender.hasPermission("texturemanager.send")) return noPermission(sender); send(sender, args); }
            default -> sender.sendMessage(plugin.color("&cНеизвестная подкоманда. Используйте &f/texture help&c."));
        }
        return true;
    }

    private void rebuild(CommandSender sender) {
        sender.sendMessage(plugin.color("&eНачинается пересборка ресурс-пака..."));
        boolean success = plugin.buildResourcePack(true);
        sender.sendMessage(plugin.color(success ? "&aПересборка успешно завершена." : "&cПересборка завершилась ошибкой. Смотрите консоль."));
    }

    private void send(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(plugin.color("&cИгрок не найден или не в сети.")); return; }
            plugin.sendPack(target);
            sender.sendMessage(plugin.color("&aРесурс-пак отправлен игроку &f" + target.getName() + "&a."));
            return;
        }
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) { plugin.sendPack(player); count++; }
        sender.sendMessage(plugin.color("&aРесурс-пак отправлен игрокам. Получателей: &f" + count));
    }

    private boolean noPermission(CommandSender sender) { sender.sendMessage(plugin.color("&cУ вас нет прав для выполнения этой команды.")); return true; }
    private void status(CommandSender sender) {
        ResourcePackBuilder.BuildResult build = plugin.getLastBuild();
        sender.sendMessage(plugin.color("&6==== Состояние TextureManager ===="));
        sender.sendMessage(plugin.color("&7Версия Minecraft: &f" + plugin.getServer().getMinecraftVersion()));
        sender.sendMessage(plugin.color("&7Формат ресурс-пака: &f" + PackFormatResolver.resolvePackFormat()));
        sender.sendMessage(plugin.color("&7Текстур найдено: &f" + plugin.scanTextures().size()));
        sender.sendMessage(plugin.color("&7URL: &f" + (plugin.getPackUrl().isBlank() ? "не настроен" : plugin.getPackUrl())));
        if (build == null) sender.sendMessage(plugin.color("&7Ресурс-пак: &cещё не собран"));
        else { sender.sendMessage(plugin.color("&7Ресурс-пак: &aсобран")); sender.sendMessage(plugin.color("&7Размер: &f" + build.size() + " байт")); sender.sendMessage(plugin.color("&7SHA-1: &f" + build.sha1())); }
    }
    private void list(CommandSender sender) { List<TextureInfo> textures=plugin.scanTextures(); sender.sendMessage(plugin.color("&6==== Установленные текстуры ====")); if(textures.isEmpty()){sender.sendMessage(plugin.color("&7Текстуры ещё не добавлены."));return;} for(TextureInfo t:textures)sender.sendMessage(plugin.color("&7- &f"+t.relativePath()+" &8("+t.width()+"x"+t.height()+")")); }
    private void help(CommandSender sender) { sender.sendMessage(plugin.color("&6==== TextureManager ====")); sender.sendMessage(plugin.color("&e/texture reload &7— проверить PNG и пересобрать ресурс-пак")); sender.sendMessage(plugin.color("&e/texture status &7— показать состояние и SHA-1")); sender.sendMessage(plugin.color("&e/texture list &7— показать все найденные текстуры")); sender.sendMessage(plugin.color("&e/texture send [игрок] &7— отправить ресурс-пак всем или одному игроку")); }
}
