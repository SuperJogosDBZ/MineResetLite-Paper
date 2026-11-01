package com.koletar.jj.mineresetlite;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Config {
    private Config() {}
    private static boolean broadcastInWorldOnly = false;
    private static boolean broadcastNearbyOnly = false;
    private static String locale = "en";

    public static boolean getBroadcastInWorldOnly() {
        return broadcastInWorldOnly;
    }

    public static boolean getBroadcastNearbyOnly() {
        return broadcastNearbyOnly;
    }

    public static String getLocale() {
        return locale;
    }

    public static void initConfig(File dataFolder) throws IOException {
        java.nio.file.Files.createDirectories(dataFolder.toPath());
        File configFile = new File(dataFolder, "config.yml");
        YamlConfiguration config = new YamlConfiguration();
        if (configFile.exists()) {
            try { config.load(configFile); }
            catch (org.bukkit.configuration.InvalidConfigurationException ex) {
                throw new IOException("Invalid config.yml", ex);
            }
        }
        boolean changed = false;
        if (!config.contains("broadcast-in-world-only")) { config.set("broadcast-in-world-only", false); changed = true; }
        if (!config.contains("broadcast-nearby-only")) { config.set("broadcast-nearby-only", false); changed = true; }
        if (!config.contains("locale")) { config.set("locale", "en"); changed = true; }
        broadcastInWorldOnly = config.getBoolean("broadcast-in-world-only");
        broadcastNearbyOnly = config.getBoolean("broadcast-nearby-only");
        locale = config.getString("locale", "en");
        if (changed) config.save(configFile);
    }
}
