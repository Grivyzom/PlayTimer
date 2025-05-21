package gc.grivyzom.playtimer.listeners;

import gc.grivyzom.playtimer.storage.StorageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.sql.SQLException;

public class PlayTimeListener implements Listener {

    private final StorageManager storage;
    private final Map<UUID, Long> joinTimestamps = new HashMap<>();

    public PlayTimeListener(StorageManager storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        joinTimestamps.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();
        long in = joinTimestamps.getOrDefault(id, System.currentTimeMillis());
        long diff = System.currentTimeMillis() - in;

        // Convert ms → seconds
        long played = diff / 1000;

        try {
            storage.savePlayTime(id, played);
        } catch (SQLException e) {
            // Log the error but don't crash the server
            p.getServer().getLogger().warning("Error saving playtime for " + p.getName() + ": " + e.getMessage());
        }

        joinTimestamps.remove(id);
    }
}