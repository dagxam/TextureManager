package me.dagxam.texturemanager;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Определяет объект, на который смотрит игрок. */
public final class TextureTargetCommand implements CommandExecutor {
    private static final int DISTANCE = 8;
    private final TextureManagerPlugin plugin;

    public TextureTargetCommand(TextureManagerPlugin plugin) { this.plugin = plugin; }

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

        Entity entity = player.getTargetEntity(DISTANCE, false);
        if (entity != null) {
            plugin.showEntityTexture(player, entity);
            return true;
        }

        Block block = player.getTargetBlockExact(DISTANCE);
        if (block != null && !block.getType().isAir()) {
            plugin.showBlockTexture(player, block);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.getType().isAir()) {
            Material material = item.getType();
            plugin.showTargetTexture(player, material, "item/" + material.getKey().getKey() + ".png");
            return true;
        }

        player.sendMessage(plugin.color("&eНаведитесь на моба или блок. Если цели нет — возьмите предмет в руку."));
        return true;
    }
}
