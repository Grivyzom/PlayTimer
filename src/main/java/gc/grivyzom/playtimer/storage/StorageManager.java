// src/main/java/gc/grivyzom/playtimer/storage/StorageManager.java
package gc.grivyzom.playtimer.storage;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public interface StorageManager {
    long getPlayTime(UUID player) throws SQLException;
    void savePlayTime(UUID player, long time) throws SQLException;
    Map<UUID, Long> loadAll() throws SQLException;
    void close() throws SQLException;
}
