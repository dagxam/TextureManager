package me.dagxam.texturemanager;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Определяет объект, на который смотрит игрок, и показывает путь к его
 * стандартной текстуре Minecraft.
 */
public final class TextureTargetCommand implements CommandExecutor {

    private final TextureManagerPlugin plugin;

    public TextureTargetCommand(TextureManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.color("&cЭта команда доступна только игроку."));
            return true;
        }
        if (!player.hasPermission("texturemanager.target")) {
            sender.sendMessage(plugin.color(plugin.getMessage("доступ-запрещён", "&cУ вас нет прав для выполнения этой команды.")));
            return true;
        }

        Block target = player.getTargetBlockExact(8);
        if (target != null && !target.getType().isAir()) {
            Material material = target.getType();
            String path = "block/" + material.getKey().getKey() + ".png";
            plugin.showTargetTexture(player, material, path);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.getType().isAir()) {
            Material material = item.getType();
            String path = "item/" + material.getKey().getKey() + ".png";
            plugin.showTargetTexture(player, material, path);
            return true;
        }

        player.sendMessage(plugin.color("&eНаведитесь на блок или возьмите предмет в руку."));
        return true;
    }
}
