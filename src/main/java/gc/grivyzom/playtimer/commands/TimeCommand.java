package gc.grivyzom.playtimer.commands;

import gc.grivyzom.playtimer.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class TimeCommand implements CommandExecutor {

    private final StorageManager storage;

    // Ahora sólo recibe StorageManager
    public TimeCommand(StorageManager storage) {
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        try {
            long tiempo = storage.getPlayTime(uuid);
            player.sendMessage("§aTu tiempo de juego acumulado es: §e" + tiempo + " segundos");
        } catch (SQLException e) {
            player.sendMessage("§cError al obtener tu tiempo de juego. Intenta más tarde.");
            e.printStackTrace();
        }

        return true;
    }
}
