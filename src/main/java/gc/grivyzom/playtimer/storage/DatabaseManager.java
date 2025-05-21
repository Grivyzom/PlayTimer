package gc.grivyzom.playtimer.storage;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager implements StorageManager {
    private final Connection connection;

    public DatabaseManager(String url, String user, String pass) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, pass);
    }

    @Override
    public long getPlayTime(UUID player) throws SQLException {
        String sql = "SELECT tiempo_jugado FROM playtimes WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("tiempo_jugado");
                }
            }
        }
        return 0L;
    }

    @Override
    public void savePlayTime(UUID player, long time) throws SQLException {
        String sql = "INSERT INTO playtimes (uuid, tiempo_jugado) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE tiempo_jugado = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setLong(2, time);
            ps.setLong(3, time);
            ps.executeUpdate();
        }
    }

    @Override
    public Map<UUID, Long> loadAll() throws SQLException {
        Map<UUID, Long> result = new HashMap<>();
        String sql = "SELECT uuid, tiempo_jugado FROM playtimes";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long t    = rs.getLong("tiempo_jugado");
                result.put(uuid, t);
            }
        }
        return result;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // Cargar o crear usuario
    public void ensureUserExists(UUID uuid, String nombre, String rango) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT IGNORE INTO users (uuid, nombre, rango, tiempo_jugado_hoy, fecha_ultimo_reset) VALUES (?, ?, ?, 0, CURDATE())");
        ps.setString(1, uuid.toString());
        ps.setString(2, nombre);
        ps.setString(3, rango);
        ps.executeUpdate();
        ps.close();
    }

    // Sumar tiempo jugado hoy
    public void addPlayTime(UUID uuid, long seconds) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET tiempo_jugado_hoy = tiempo_jugado_hoy + ? WHERE uuid = ?");
        ps.setLong(1, seconds);
        ps.setString(2, uuid.toString());
        ps.executeUpdate();
        ps.close();
    }

    // Obtener tiempo jugado hoy
    public long getPlayTimeToday(UUID uuid) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT tiempo_jugado_hoy FROM users WHERE uuid = ?");
        ps.setString(1, uuid.toString());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            long tiempo = rs.getLong("tiempo_jugado_hoy");
            rs.close(); ps.close();
            return tiempo;
        }
        rs.close(); ps.close();
        return 0;
    }

    // Resetear tiempo jugado hoy (por reset diario)
    public void resetPlayTime(UUID uuid) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET tiempo_jugado_hoy = 0, fecha_ultimo_reset = CURDATE() WHERE uuid = ?");
        ps.setString(1, uuid.toString());
        ps.executeUpdate();
        ps.close();
    }

    // Agregar bonificación
    public void addBonus(UUID uuid, long seconds, String tipo, boolean activa) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO bonuses (uuid, tipo, tiempo_segundos, fecha_otorgado, activo) VALUES (?, ?, ?, CURDATE(), ?)");
        ps.setString(1, uuid.toString());
        ps.setString(2, tipo);
        ps.setLong(3, seconds);
        ps.setBoolean(4, activa);
        ps.executeUpdate();
        ps.close();
    }

    // Quitar bonificación (por id)
    public void removeBonus(int bonusId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM bonuses WHERE id = ?");
        ps.setInt(1, bonusId);
        ps.executeUpdate();
        ps.close();
    }

    // Obtener bonificaciones activas (perm y diarias)
    public long getActiveBonus(UUID uuid) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT SUM(tiempo_segundos) FROM bonuses WHERE uuid = ? AND (tipo='permanente' AND activo=1 OR (tipo='diario' AND fecha_otorgado = CURDATE()))");
        ps.setString(1, uuid.toString());
        ResultSet rs = ps.executeQuery();
        long total = 0;
        if (rs.next()) total = rs.getLong(1);
        rs.close(); ps.close();
        return total;
    }

    // (Opcional) Historial de acciones (agregar, quitar bono, reset, etc.)
    public void logHistory(UUID uuid, String action) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO history (uuid, accion, fecha) VALUES (?, ?, NOW())");
        ps.setString(1, uuid.toString());
        ps.setString(2, action);
        ps.executeUpdate();
        ps.close();
    }

    // (Opcional) Cambiar rango
    public void setRango(UUID uuid, String rango) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET rango=? WHERE uuid=?");
        ps.setString(1, rango);
        ps.setString(2, uuid.toString());
        ps.executeUpdate();
        ps.close();
    }

    public long getRemainingTime(String playerName) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT (u.tiempo_base_rango + IFNULL(SUM(b.tiempo_segundos), 0) - u.tiempo_jugado_hoy " +
                        "FROM users u LEFT JOIN bonuses b ON u.uuid = b.uuid " +
                        "WHERE u.nombre = ? AND (b.activo=1 OR b.tipo='diario' AND b.fecha_otorgado = CURDATE())");
        ps.setString(1, playerName);
        ResultSet rs = ps.executeQuery();
        long remaining = 0;
        if (rs.next()) {
            remaining = rs.getLong(1);
        }
        rs.close();
        ps.close();
        return remaining / 60; // Convertir a minutos
    }

    public long getTiempoBasePorRango(UUID uuid) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT tiempo_base_rango FROM users WHERE uuid = ?");
        ps.setString(1, uuid.toString());
        ResultSet rs = ps.executeQuery();
        long tiempo = 0;
        if (rs.next()) {
            tiempo = rs.getLong("tiempo_base_rango");
        }
        rs.close();
        ps.close();
        return tiempo;
    }
}
