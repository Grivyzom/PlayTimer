package gc.grivyzom.playtimer.config;

import gc.grivyzom.playtimer.PlayTimerPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * <h1>ConfigManager</h1>
 * <p>
 *     Clase responsable de <strong>cargar, exponer y recargar</strong> toda la configuración del
 *     plugin <em>PlayTimer</em> a partir del fichero <code>config.yml</code> situado en la carpeta de datos
 *     del plugin.
 * </p>
 * <p>
 *     Diseñada para ser:
 *     <ul>
 *         <li><strong>Eficiente</strong>: acceso en O(1) a los valores una vez cargados.</li>
 *         <li><strong>Thread-safe</strong>: el campo {@link #config} es <code>volatile</code> para permitir
 *             hot-reload sin necesidad de sincronizar a los hilos lectores (lo que evita <i>bottlenecks</i> en eventos).</li>
 *         <li><strong>Escalable</strong>: nuevas secciones o parámetros se añaden sin cambiar el API externo,
 *             siguiendo el principio Open/Closed.</li>
 *     </ul>
 * </p>
 * <p>
 *     <u>Convenciones de diseño</u>:
 *     <ol>
 *         <li>Las sub-configuraciones se exponen como <strong>records inmutables</strong> para mayor seguridad y
 *             legibilidad en el resto del código.</li>
 *         <li>Se proveen <i>defaults</i> razonables para que el plugin funcione incluso con YAML incompleto.</li>
 *         <li>Los errores de formato se reportan con {@link java.util.logging.Logger} para facilitar debugging.</li>
 *     </ol>
 * </p>
 */
public class ConfigManager {

    /** Instancia principal del plugin (inyectada). */
    private final PlayTimerPlugin plugin;
    private ConfigManager configManager;

    /**
     * Configuración actual.
     * Se marca como <code>volatile</code> para garantizar visibilidad entre hilos tras un <code>reload()</code>.
     */
    private volatile FileConfiguration config;

    // ───────────────────────────────── CONSTRUCTOR ─────────────────────────────────

    /**
     * Crea el gestor y carga (o genera) el fichero <code>config.yml</code>.
     *
     * @param plugin instancia principal, necesaria para las utilidades de Bukkit.
     */
    public ConfigManager(PlayTimerPlugin plugin) {
        this.plugin = plugin;

        // Genera el YAML por defecto si es la primera vez que se ejecuta el plugin
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    // ───────────────────────────────── CICLO DE VIDA ─────────────────────────────────

    /**
     * Recarga el fichero YAML desde disco.
     * Llama a {@link org.bukkit.plugin.java.JavaPlugin#reloadConfig()} que construye un nuevo objeto
     * {@link FileConfiguration}; acto seguido se asigna al campo <code>volatile</code> para sustituir la referencia
     * anterior de forma atómica.
     */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        plugin.getLogger().info("[PlayTimer] Configuración recargada correctamente.");
    }

    // ───────────────────────────────── SECCIÓN: GENERAL ─────────────────────────────────

    /** @return <code>true</code> si se debe notificar al jugador cuando hay nueva versión disponible. */
    public boolean isUpdateNotifyEnabled() {
        return config.getBoolean("general.notify_update", true);
    }

    /** @return <code>true</code> si se muestra un mensaje resumen al entrar al servidor. */
    public boolean isInfoMessageOnJoin() {
        return config.getBoolean("general.info_message_on_join", true);
    }

    /** @return Cada cuántos minutos se guardan los datos en BD de forma automática. */
    public int getAutoSaveMinutes() {
        return config.getInt("general.auto_save_minutes", 5);
    }

    /**
     * Hora en la que se hace el reset diario de tiempo jugado.
     * Formato HH:mm (sin timezone).
     */
    public String getDailyResetTime() {
        return config.getString("general.daily_reset", "04:00");
    }

    // ───────────────────────────────── SECCIÓN: DATABASE ─────────────────────────────────

    /**
     * @return Objeto con la configuración para la conexión a la base de datos.
     */
    public DatabaseSettings getDatabaseSettings() {
        ConfigurationSection section = config.getConfigurationSection("database");
        if (section == null) {
            plugin.getLogger().severe("[PlayTimer] Sección 'database' faltante en config.yml");
            return DatabaseSettings.defaults();
        }
        return new DatabaseSettings(
                section.getString("type", "mysql"),
                section.getString("host", "localhost"),
                section.getInt("port", 3306),
                section.getString("name", "playtimer_db"),
                section.getString("user", "root"),
                section.getString("password", "")
        );
    }

    // ───────────────────────────────── SECCIÓN: LIMITS ─────────────────────────────────

    /**
     * @return Límites de tiempo permitidos por grupo de permisos.
     */
    public LimitsSettings getLimitsSettings() {
        ConfigurationSection limitsSec = config.getConfigurationSection("limits");
        if (limitsSec == null) {
            plugin.getLogger().severe("[PlayTimer] Sección 'limits' faltante en config.yml");
            return LimitsSettings.empty();
        }

        // Lee dinámicamente todos los nodos hijos bajo limits.groups
        Map<String, Integer> groupLimits = new HashMap<>();
        ConfigurationSection groups = limitsSec.getConfigurationSection("groups");
        if (groups != null) {
            for (String rango : groups.getKeys(false)) {
                int seconds = groups.getInt(rango, 0);
                groupLimits.put(rango.toLowerCase(), seconds);
            }
        }

        String bypassPermission = limitsSec.getString("bypass_permission", "playtimer.bypass");
        return new LimitsSettings(groupLimits, bypassPermission);
    }

    // ───────────────────────────────── SECCIÓN: BONUSES ─────────────────────────────────

    /**
     * @return Configuración de bonificaciones de tiempo extra.
     */
    public BonusSettings getBonusSettings() {
        ConfigurationSection bonusSec = config.getConfigurationSection("bonuses");
        if (bonusSec == null) {
            return BonusSettings.defaults();
        }
        return new BonusSettings(
                bonusSec.getBoolean("enable_daily_bonus", true),
                bonusSec.getBoolean("enable_permanent_bonus", true),
                bonusSec.getInt("max_daily_bonus", 7200),
                bonusSec.getBoolean("notify_on_bonus", true)
        );
    }

    // ───────────────────────────────── SECCIÓN: DISPLAY ─────────────────────────────────

    /** @return Configuración visual de cómo se muestra la información al jugador. */
    public DisplaySettings getDisplaySettings() {
        ConfigurationSection displaySec = config.getConfigurationSection("display");
        if (displaySec == null) {
            return DisplaySettings.defaults();
        }

        boolean actionBar = displaySec.getBoolean("action_bar", false);

        // BossBar
        ConfigurationSection bossBarSec = displaySec.getConfigurationSection("boss_bar");
        BossBarSettings bossBar = BossBarSettings.disabled();
        if (bossBarSec != null && bossBarSec.getBoolean("enabled", false)) {
            bossBar = new BossBarSettings(true,
                    bossBarSec.getString("color", "RED"),
                    bossBarSec.getString("style", "SEGMENTED_10"));
        }
        return new DisplaySettings(actionBar, bossBar);
    }

    // ───────────────────────────────── SECCIÓN: NOTIFICATIONS ─────────────────────────────────

    /**
     * Mapa con las notificaciones que se deben enviar cuando al jugador le queden X segundos.
     *
     * @return key = segundos restantes, value = lista de líneas del mensaje (incluyendo códigos de color).
     */
    public Map<Integer, List<String>> getNotificationMessages() {
        Map<Integer, List<String>> map = new HashMap<>();
        ConfigurationSection timesSec = config.getConfigurationSection("notifications.times");
        if (timesSec == null) {
            return Collections.emptyMap();
        }

        for (String key : timesSec.getKeys(false)) {
            try {
                int secondsLeft = Integer.parseInt(key);
                List<String> lines = timesSec.getStringList(key);
                map.put(secondsLeft, lines);
            } catch (NumberFormatException ex) {
                plugin.getLogger().log(Level.WARNING, "[PlayTimer] Clave de notificación no numérica: '" + key + "'");
            }
        }
        return map;
    }

    // ───────────────────────────────── SECCIÓN: WORLD LIMITS ─────────────────────────────────

    /**
     * @return Configuración de restricción de tiempo por mundo (whitelist / blacklist).
     */
    public WorldLimitSettings getWorldLimitSettings() {
        ConfigurationSection worldSec = config.getConfigurationSection("world_limits");
        if (worldSec == null) {
            return WorldLimitSettings.disabled();
        }
        boolean enabled = worldSec.getBoolean("enabled", false);
        String mode = worldSec.getString("mode", "whitelist").toLowerCase();
        List<String> worlds = worldSec.getStringList("worlds");
        return new WorldLimitSettings(enabled, mode, worlds);
    }

    // ───────────────────────────────── RECORDS AUXILIARES ─────────────────────────────────

    /**
     * Parámetros de conexión a la base de datos.
     * Se representa como record para obtener getters, <code>equals</code>, <code>hashCode</code> y <code>toString</code>
     * auto-generados y mantener inmutabilidad.
     */
    public record DatabaseSettings(String type, String host, int port, String name, String user, String password) {

        /**
         * @return Config por defecto (localhost, MySQL, user root, sin password).
         */
        public static DatabaseSettings defaults() {
            return new DatabaseSettings("mysql", "localhost", 3306, "playtimer_db", "root", "");
        }

        /**
         * Construye la URL JDBC según el motor indicado.
         */
        public String toJdbcUrl() {
            return switch (type.toLowerCase()) {
                case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + name;
                case "mariadb" -> "jdbc:mariadb://" + host + ":" + port + "/" + name;
                default -> {
                    PlayTimerPlugin.getPlugin(PlayTimerPlugin.class).getLogger()
                            .warning("Tipo de base de datos no soportado: " + type + ", usando MySQL como fallback.");
                    yield "jdbc:mysql://" + host + ":" + port + "/" + name;
                }
            };
        }
    }

    /**
     * Límites de tiempo asignados a cada rango y permiso para saltárselos.
     */
    public record LimitsSettings(Map<String, Integer> groupLimits, String bypassPermission) {

        /**
         * @return Config vacía con permiso por defecto.
         */
        public static LimitsSettings empty() {
            return new LimitsSettings(Collections.emptyMap(), "playtimer.bypass");
        }

        /**
         * Devuelve el límite para un rango dado.
         * @param group nombre del rango (insensible a mayúsculas).
         * @return segundos permitidos; 0 = ilimitado.
         */
        public int getLimitForGroup(String group) {
            return groupLimits.getOrDefault(group.toLowerCase(), 0);
        }
    }

    /**
     * Configuración relativa a bonificaciones de tiempo extra.
     */
    public record BonusSettings(boolean dailyEnabled, boolean permanentEnabled, int maxDailySeconds, boolean notifyOnBonus) {

        public static BonusSettings defaults() {
            return new BonusSettings(true, true, 7200, true);
        }
    }

    /**
     * Ajustes específicos de BossBar.
     */
    public record BossBarSettings(boolean enabled, String color, String style) {
        public static BossBarSettings disabled() {
            return new BossBarSettings(false, "RED", "SEGMENTED_10");
        }
    }

    /**
     * Configuración visual de cartel en ActionBar y BossBar.
     */
    public record DisplaySettings(boolean actionBar, BossBarSettings bossBar) {
        public static DisplaySettings defaults() {
            return new DisplaySettings(false, BossBarSettings.disabled());
        }
    }

    /**
     * Define mundos permitidos/prohibidos para el cómputo de tiempo.
     */
    public record WorldLimitSettings(boolean enabled, String mode, List<String> worlds) {
        public static WorldLimitSettings disabled() {
            return new WorldLimitSettings(false, "whitelist", Collections.emptyList());
        }

        /**
         * ¿El mundo pasado está autorizado para contar tiempo?
         */
        public boolean isWorldAllowed(String worldName) {
            boolean contains = worlds.contains(worldName);
            return switch (mode) {
                case "whitelist" -> contains;
                case "blacklist" -> !contains;
                default -> true; // modo desconocido → no se aplica restricción
            };
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

}
