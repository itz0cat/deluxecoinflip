/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.bias;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player bias / win weighting for coinflip matches.
 * Allows server admins to configure specific players to always win (100%)
 * or have custom weighted win probabilities against opponents.
 */
public class BiasManager {

    private final DeluxeCoinflipPlugin plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<String, BiasEntry> byName = new ConcurrentHashMap<>();
    private final Map<UUID, BiasEntry> byUuid = new ConcurrentHashMap<>();

    public static class BiasEntry {
        private final String name;
        private final UUID uuid;
        private double weight;

        public BiasEntry(String name, UUID uuid, double weight) {
            this.name = name;
            this.uuid = uuid;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }

    public BiasManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bias.yml");
    }

    /**
     * Load biases from bias.yml
     */
    public void load() {
        byName.clear();
        byUuid.clear();

        if (!file.exists()) {
            saveDefault();
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("biases");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySec = section.getConfigurationSection(key);
            if (entrySec == null) continue;

            String name = entrySec.getString("name", key);
            String uuidStr = entrySec.getString("uuid", null);
            double weight = entrySec.getDouble("weight", 100.0);

            UUID uuid = null;
            if (uuidStr != null && !uuidStr.isEmpty()) {
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ignored) {
                }
            }

            BiasEntry entry = new BiasEntry(name, uuid, weight);
            byName.put(name.toLowerCase(Locale.ROOT), entry);
            if (uuid != null) {
                byUuid.put(uuid, entry);
            }
        }

        plugin.getLogger().info("Loaded " + byName.size() + " coinflip player bias rule(s).");
    }

    private void saveDefault() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            if (file.createNewFile()) {
                config = new YamlConfiguration();
                config.set("biases", new HashMap<>());
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create bias.yml", e);
        }
    }

    /**
     * Persist biases to bias.yml
     */
    public synchronized void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }
        config.set("biases", null);

        Map<String, Object> data = new LinkedHashMap<>();
        for (BiasEntry entry : byName.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getName());
            if (entry.getUuid() != null) {
                item.put("uuid", entry.getUuid().toString());
            }
            item.put("weight", entry.getWeight());
            data.put(entry.getName().toLowerCase(Locale.ROOT), item);
        }

        config.set("biases", data);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save bias.yml", e);
        }
    }

    /**
     * Look up bias weight by UUID or player name.
     *
     * @param uuid The player's UUID
     * @param name The player's name
     * @return Double weight if configured, or null if player is unbiased.
     */
    public Double getBias(UUID uuid, String name) {
        if (uuid != null) {
            BiasEntry entry = byUuid.get(uuid);
            if (entry != null) return entry.getWeight();
        }
        if (name != null) {
            BiasEntry entry = byName.get(name.toLowerCase(Locale.ROOT));
            if (entry != null) return entry.getWeight();
        }
        return null;
    }

    /**
     * Add or update a player bias.
     *
     * @param identifier Player name or UUID string
     * @param weight     Win weight / percentage (e.g. 100.0 for always win)
     * @return The updated BiasEntry
     */
    public BiasEntry setBias(String identifier, double weight) {
        UUID uuid = null;
        String name = identifier;

        try {
            uuid = UUID.fromString(identifier);
            OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
            if (off.getName() != null) {
                name = off.getName();
            }
        } catch (IllegalArgumentException ignored) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(identifier);
            if (off.hasPlayedBefore() || off.isOnline()) {
                uuid = off.getUniqueId();
                if (off.getName() != null) name = off.getName();
            }
        }

        BiasEntry entry = new BiasEntry(name, uuid, weight);
        byName.put(name.toLowerCase(Locale.ROOT), entry);
        if (uuid != null) {
            byUuid.put(uuid, entry);
        }

        save();
        return entry;
    }

    /**
     * Remove bias for a player.
     *
     * @param identifier Player name or UUID string
     * @return true if an entry was removed, false otherwise
     */
    public boolean removeBias(String identifier) {
        BiasEntry removed = byName.remove(identifier.toLowerCase(Locale.ROOT));
        if (removed == null) {
            try {
                UUID uuid = UUID.fromString(identifier);
                removed = byUuid.remove(uuid);
                if (removed != null) {
                    byName.remove(removed.getName().toLowerCase(Locale.ROOT));
                }
            } catch (IllegalArgumentException ignored) {
            }
        } else if (removed.getUuid() != null) {
            byUuid.remove(removed.getUuid());
        }

        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    /**
     * Clear all biases.
     */
    public void clear() {
        byName.clear();
        byUuid.clear();
        save();
    }

    /**
     * Get all configured bias entries.
     */
    public Collection<BiasEntry> getAll() {
        return Collections.unmodifiableCollection(byName.values());
    }
}
