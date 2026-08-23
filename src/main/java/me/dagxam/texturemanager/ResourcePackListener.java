package me.dagxam.texturemanager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Автоматически отправляет актуальный ресурс-пак при входе игрока. */
public final class ResourcePackListener implements Listener {
    private final TextureManagerPlugin plugin;

    public ResourcePackListener(TextureManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.отправлять-при-входе", true)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.sendPack(event.getPlayer()), 20L);
    }

    public void send(Player player) {
        plugin.sendPack(player);
    }
}
