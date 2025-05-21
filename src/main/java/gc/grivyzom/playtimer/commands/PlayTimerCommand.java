package gc.grivyzom.playtimer.commands;

import gc.grivyzom.playtimer.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class PlayTimerCommand implements CommandExecutor {

    private final StorageManager storage;

    // Antes recibías PlayTimerPlugin; cámbialo a StorageManager
    public PlayTimerCommand(StorageManager storage) {
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando.");
            return true;
        }

        Player p = (Player) sender;
        UUID id = p.getUniqueId();
        try {
            long tiempo = storage.getPlayTime(id);
            p.sendMessage("§aTu tiempo de juego acumulado es: §e" + tiempo + " segundos");
        } catch (SQLException e) {
            p.sendMessage("§cError al obtener tu tiempo de juego. Intenta más tarde.");
            e.printStackTrace();
        }
        return true;
    }
}
