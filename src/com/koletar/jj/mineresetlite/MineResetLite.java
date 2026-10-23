package com.koletar.jj.mineresetlite;

import com.koletar.jj.mineresetlite.commands.MineCommands;
import com.koletar.jj.mineresetlite.commands.PluginCommands;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

public class MineResetLite extends JavaPlugin {
    public List<Mine> mines;
    private Logger logger;
    private CommandManager commandManager;
    private Plugin worldEdit = null;
    private int saveTaskId = -1;
    private int resetTaskId = -1;

    static {
        ConfigurationSerialization.registerClass(Mine.class);
    }

    private static class IsMineFile implements FilenameFilter {
        public boolean accept(File file, String s) {
            return s.contains(".mine.yml");
        }
    }

    public void onEnable() {
        mines = new ArrayList<Mine>();
        logger = getLogger();
        if (!setupConfig()) {
            logger.severe("Since I couldn't setup config files properly, I guess this is goodbye. ");
            logger.severe("Plugin Loading Aborted!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        commandManager = new CommandManager();
        commandManager.register(MineCommands.class, new MineCommands(this));
        commandManager.register(CommandManager.class, commandManager);
        commandManager.register(PluginCommands.class, new PluginCommands(this));
        Locale locale = new Locale(Config.getLocale());
        Phrases.getInstance().initialize(locale);
        File overrides = new File(getDataFolder(), "phrases.properties");
        if (overrides.exists()) {
            Properties overridesProps = new Properties();
            try {
                try (FileInputStream stream = new FileInputStream(overrides)) {
                    overridesProps.load(stream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Phrases.getInstance().overrides(overridesProps);
        }

        if (getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
        }
        if (worldEdit == null && getServer().getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            worldEdit = getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
        }

        File[] mineFiles = new File(getDataFolder(), "mines").listFiles(new IsMineFile());
        if (mineFiles == null) throw new IllegalStateException("Cannot read mines directory");
        for (File file : mineFiles) {
            logger.info("Loading mine from file '" + file.getName() + "'...");
            FileConfiguration fileConf = YamlConfiguration.loadConfiguration(file);
            try {
                Object o = fileConf.get("mine");
                if (!(o instanceof Mine)) {
                    logger.severe("Mine wasn't a mine object! Something is off with serialization!");
                    continue;
                }
                Mine mine = (Mine) o;
                mines.add(mine);
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.SEVERE, "Unable to load mine: " + file.getName(), t);
            }
        }
        resetTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for (Mine mine : mines) {
                    mine.cron();
                }
            }
        }, 60 * 20L, 60 * 20L);
        logger.info("MineResetLite version " + getDescription().getVersion() + " enabled!");
    }

    public void onDisable() {
        getServer().getScheduler().cancelTask(resetTaskId);
        getServer().getScheduler().cancelTask(saveTaskId);
        HandlerList.unregisterAll(this);
        save();
        logger.info("MineResetLite disabled");
    }

    public Mine[] matchMines(String in) {
        List<Mine> matches = new LinkedList<Mine>();
        for (Mine mine : mines) {
            if (mine.getName().toLowerCase().contains(in.toLowerCase())) {
                matches.add(mine);
            }
        }
        return matches.toArray(new Mine[matches.size()]);
    }

    public void buffSave() {
        BukkitScheduler scheduler = getServer().getScheduler();
        if (saveTaskId != -1) {

            scheduler.cancelTask(saveTaskId);
        }

        final MineResetLite plugin = this;
        saveTaskId = scheduler.scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                saveTaskId = -1;
                plugin.save();
            }
        }, 60 * 20L);
    }

    public void save() {
        for (Mine mine : mines) {
            File mineFile = getMineFile(mine);
            FileConfiguration mineConf = YamlConfiguration.loadConfiguration(mineFile);
            mineConf.set("mine", mine);
            try {
                mineConf.save(mineFile);
            } catch (IOException e) {
                logger.severe("Unable to serialize mine!");
                e.printStackTrace();
            }
        }
    }

    private File getMineFile(Mine mine) {
        return new File(new File(getDataFolder(), "mines"), mine.getName().replace(" ", "") + ".mine.yml");
    }

    public void eraseMine(Mine mine) {
        mines.remove(mine);
        getMineFile(mine).delete();
    }

    public boolean hasWorldEdit() {
        return worldEdit != null;
    }

    public Plugin getWorldEdit() {
        return worldEdit;
    }

    private boolean setupConfig() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) {
            logger.severe("Could not make plugin folder! This won't end well...");
            return false;
        }
        File mineFolder = new File(getDataFolder(), "mines");
        if (!mineFolder.exists() && !mineFolder.mkdir()) {
            logger.severe("Could not make mine folder! Abort! Abort!");
            return false;
        }
        try {
            Config.initConfig(getDataFolder());
        } catch (IOException e) {
            logger.severe("Could not make config file!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mineresetlite")) {
            if (args.length == 0) {
                String[] helpArgs = new String[0];
                commandManager.callCommand("help", sender, helpArgs);
                return true;
            }

            String[] spoofedArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                spoofedArgs[i - 1] = args[i];
            }
            commandManager.callCommand(args[0], sender, spoofedArgs);
            return true;
        }
        return false;
    }

    public static void broadcast(String message, Mine mine) {
        if (Config.getBroadcastNearbyOnly()) {
            for (Player p : mine.getWorld().getPlayers()) {
                if (mine.isInside(p)) {
                    p.sendMessage(message);
                }
            }
            Bukkit.getLogger().info(message);
        } else if (Config.getBroadcastInWorldOnly()) {
            for (Player p : mine.getWorld().getPlayers()) {
                p.sendMessage(message);
            }
            Bukkit.getLogger().info(message);
        } else {
            Bukkit.getServer().broadcastMessage(message);
        }
    }
}
