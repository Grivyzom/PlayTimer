package gc.grivyzom.playtimer;

import gc.grivyzom.playtimer.config.ConfigManager;
import gc.grivyzom.playtimer.commands.PlayTimerCommand;
import gc.grivyzom.playtimer.commands.TimeCommand;
import gc.grivyzom.playtimer.listeners.PlayTimeListener;
import gc.grivyzom.playtimer.storage.DatabaseManager;
import gc.grivyzom.playtimer.storage.JsonStorageManager;
import gc.grivyzom.playtimer.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

import java.sql.SQLException;

public class PlayTimerPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        // 1) Guardar/leer config
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // 2) Intentar MySQL, si falla caer en JSON
        var db = configManager.getDatabaseSettings();
        try {
            storageManager = new DatabaseManager(
                    db.toJdbcUrl(),
                    db.user(),
                    db.password()
            );
            getLogger().info("PlayTimer: conectado a MySQL.");
        } catch (SQLException ex) {
            getLogger().warning("No se pudo conectar a MySQL (" + ex.getMessage() +
                    "), usando JsonStorageManager.");
            storageManager = new JsonStorageManager(this);
        }

        // 3) Registrar listener con nuestro StorageManager
        getServer().getPluginManager()
                .registerEvents(new PlayTimeListener(storageManager), this);

        // 4) Registrar comandos pasándoles el StorageManager
        getCommand("playtimer").setExecutor(new PlayTimerCommand(storageManager));
        getCommand("playtime").setExecutor(new TimeCommand(storageManager));

    }

    // getters si los necesitas en otro sitio
    public StorageManager getStorageManager() {
        return storageManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
