package com.koletar.jj.mineresetlite;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class Phrases {
    private static Phrases instance;
    private ResourceBundle phrases;
    private Properties overrides;

    private Phrases() {}

    public static Phrases getInstance() {
        if (instance == null) {
            instance = new Phrases();
        }
        return instance;
    }

    public void initialize(Locale l) {
        phrases = ResourceBundle.getBundle("phrases", l,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
    }

    public void overrides(Properties overrides) {
        this.overrides = overrides;
    }

    public static String phrase(String key, Object... replacements) {
        if (getInstance().phrases == null) {
            return "\u00A74Phrase Error! Did you /reload? Don't!";
        }
        if (!getInstance().phrases.containsKey(key)) {
            Logger.getLogger("Minecraft").warning("[MineResetLite] Unknown phrase key! '" + key + "'");
            return "";
        }
        String format;
        if (getInstance().overrides != null && getInstance().overrides.containsKey(key)) {
            format = getInstance().overrides.getProperty(key);
        } else {
            format = getInstance().phrases.getString(key);
        }
        for (int i = 0; i < replacements.length; i++) {
            format = format.replace("%" + i + "%", findName(replacements[i]));
        }
        format = format.replace("&", "\u00A7").replace("\u00A7\u00A7", "&");
        return format;
    }

    public static String findName(Object o) {
        if (o instanceof Boolean) {
            return phrase((Boolean) o ? "booleanTrue" : "booleanFalse");
        } else if (o instanceof Mine) {
            return ((Mine) o).getName();
        } else if (o instanceof Player) {
            return ((Player) o).getName();
        } else if (o instanceof World) {
            return ((World) o).getName();
        } else if (o instanceof SerializableBlock) {
            return o.toString();
        } else if (o instanceof ConsoleCommandSender) {
            return phrase("console");
        } else if (o instanceof BlockCommandSender) {
            return ((BlockCommandSender) o).getBlock().getType().toString();
        }
        return o.toString();
    }
}
