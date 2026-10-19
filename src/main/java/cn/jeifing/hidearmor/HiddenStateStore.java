package cn.jeifing.hidearmor;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class HiddenStateStore {
    private static final String LEGACY_HIDDEN_PLAYERS_KEY = "hidden-players";
    private static final String VISIBILITY_SETTINGS_KEY = "visibility-settings";

    private final HideArmorPlugin plugin;
    private final Path dataFile;
    private final ConcurrentHashMap<UUID, ArmorVisibilitySetting> settings =
            new ConcurrentHashMap<>();
    private final Object ioLock = new Object();
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final AtomicBoolean saveRunning = new AtomicBoolean();

    HiddenStateStore(HideArmorPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = plugin.getDataFolder().toPath().resolve("hidden-players.yml");
    }

    void load() {
        if (!Files.isRegularFile(dataFile)) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile.toFile());
        Map<String, String> rawSettings = new LinkedHashMap<>();
        var section = yaml.getConfigurationSection(VISIBILITY_SETTINGS_KEY);
        if (section != null) {
            for (String playerId : section.getKeys(false)) {
                rawSettings.put(playerId, section.getString(playerId));
            }
        }

        settings.clear();
        settings.putAll(VisibilityStateCodec.decode(
                yaml.getStringList(LEGACY_HIDDEN_PLAYERS_KEY),
                rawSettings,
                plugin.getLogger()::warning
        ));

        if (yaml.contains(LEGACY_HIDDEN_PLAYERS_KEY)) {
            writeSnapshot();
        }
    }

    ArmorVisibilitySetting setting(UUID playerId) {
        return settings.getOrDefault(playerId, ArmorVisibilitySetting.ALL_VISIBLE);
    }

    boolean hasHiddenSlots(UUID playerId) {
        return setting(playerId).hasHiddenSlots();
    }

    void setVisibility(UUID playerId, ArmorVisibilitySetting setting) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(setting, "setting");
        boolean changed = setting.isAllVisible()
                ? settings.remove(playerId) != null
                : !setting.equals(settings.put(playerId, setting));
        if (changed) {
            requestSave();
        }
    }

    int customizedCount() {
        return settings.size();
    }

    void flush() {
        dirty.set(false);
        writeSnapshot();
    }

    private void requestSave() {
        dirty.set(true);
        if (!saveRunning.compareAndSet(false, true)) {
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> drainSaves());
    }

    private void drainSaves() {
        try {
            while (dirty.getAndSet(false)) {
                writeSnapshot();
            }
        } finally {
            saveRunning.set(false);
            if (dirty.get()) {
                requestSave();
            }
        }
    }

    private void writeSnapshot() {
        synchronized (ioLock) {
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set(VISIBILITY_SETTINGS_KEY, VisibilityStateCodec.encode(settings));
                String serialized = yaml.saveToString();

                Files.createDirectories(dataFile.getParent());
                Path temporary = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
                Files.writeString(temporary, serialized, StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, dataFile,
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException error) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Unable to save hidden-players.yml", error);
            }
        }
    }
}
