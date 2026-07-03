package org.echo.worldborder.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.echo.worldborder.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportListener implements Listener {

    private final Main plugin;

    // Cooldown чтобы не спамить сообщением когда игрок упирается в границу
    private final Map<UUID, Long> messageCooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 1000;

    public TeleportListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (plugin.getMyConfig().isDisableOutOfBorderTeleport()) {
            if (plugin.getManager().isInBorder(event.getTo())) {
                event.setCancelled(true);
                sendMessage(event.getPlayer(), plugin.getMessages().getOutOfBorderTeleport(event.getTo().getWorld().getName()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Только игроки в режиме наблюдателя (GM3)
        if (player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Пропускаем если игрок только повернул голову — блок не изменился
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (plugin.getManager().isInBorder(to)) {
            // Возвращаем на прежнюю позицию, сохраняя направление взгляда
            Location safe = from.clone();
            safe.setYaw(to.getYaw());
            safe.setPitch(to.getPitch());
            event.setTo(safe);

            // Отправляем сообщение с cooldown — не чаще раза в секунду
            UUID uid = player.getUniqueId();
            long now = System.currentTimeMillis();
            if (now - messageCooldown.getOrDefault(uid, 0L) >= COOLDOWN_MS) {
                messageCooldown.put(uid, now);
                sendMessage(player, plugin.getMessages().getSpectatorOutOfBorder(to.getWorld().getName()));
            }
        }
    }

    /**
     * Отправляет сообщение игроку только если оно непустое.
     */
    private void sendMessage(Player player, String message) {
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }
}
