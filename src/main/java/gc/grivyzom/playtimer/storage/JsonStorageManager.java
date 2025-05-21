package gc.grivyzom.playtimer.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JsonStorageManager implements StorageManager {
    private final File file;
    private final Gson gson = new Gson();
    private Map<UUID, Long> data;

    public JsonStorageManager(Plugin plugin) {
        file = new File(plugin.getDataFolder(), "playtimes.json");
        plugin.getDataFolder().mkdirs();
        load();
    }

    private void load() {
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<UUID, Long>>(){}.getType();
            data = gson.fromJson(reader, type);
            if (data == null) data = new HashMap<>();
        } catch (Exception e) {
            data = new HashMap<>();
        }
    }

    private void save() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (Exception ignored) {}
    }

    @Override
    public long getPlayTime(UUID player) {
        return data.getOrDefault(player, 0L);
    }

    @Override
    public void savePlayTime(UUID player, long time) {
        data.put(player, time);
        save();
    }

    @Override
    public Map<UUID, Long> loadAll() {
        return new HashMap<>(data);
    }

    @Override
    public void close() {
        save();
    }
}
