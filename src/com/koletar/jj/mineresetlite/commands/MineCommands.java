package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.Command;
import com.koletar.jj.mineresetlite.InvalidCommandArgumentsException;
import com.koletar.jj.mineresetlite.Mine;
import com.koletar.jj.mineresetlite.MineResetLite;
import com.koletar.jj.mineresetlite.SerializableBlock;
import com.koletar.jj.mineresetlite.StringTools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

public class MineCommands {
    private MineResetLite plugin;
    private Map<Player, Location> point1;
    private Map<Player, Location> point2;

    public MineCommands(MineResetLite plugin) {
        this.plugin = plugin;
        point1 = new HashMap<Player, Location>();
        point2 = new HashMap<Player, Location>();
    }

    @Command(aliases = {"list", "l"},
            description = "command.list.description",
            help = {"command.list.help.0"},
            permissions = {"mineresetlite.mine.list"},
            min = 0, max = 0, onlyPlayers = false)
    public void listMines(CommandSender sender, String[] args) {
        sender.sendMessage(phrase("mineList", StringTools.buildList(plugin.mines, "&c", "&d, ")));
    }

    @Command(aliases = {"pos1", "p1"},
            description = "command.pos1.description",
            help = {"command.pos1.help.0"},
            usage = "command.pos1.usage",
            permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
            min = 0, max = 1, onlyPlayers = true)
    public void setPoint1(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
        Player player = (Player) sender;
        if (args.length == 0) {

            org.bukkit.block.Block target = player.getTargetBlockExact(100);
            if (target == null) {
                player.sendMessage(phrase("notABlock"));
                return;
            }
            point1.put(player, target.getLocation());
            player.sendMessage(phrase("firstPointSet"));
            return;
        } else if (args[0].equalsIgnoreCase("-feet")) {

            point1.put(player, player.getLocation());
            player.sendMessage(phrase("firstPointSet"));
            return;
        }

        throw new InvalidCommandArgumentsException();
    }

    @Command(aliases = {"pos2", "p2"},
            description = "command.pos2.description",
            help = {"command.pos2.help.0"},
            usage = "command.pos2.usage",
            permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
            min = 0, max = 1, onlyPlayers = true)
    public void setPoint2(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
        Player player = (Player) sender;
        if (args.length == 0) {

            org.bukkit.block.Block target = player.getTargetBlockExact(100);
            if (target == null) {
                player.sendMessage(phrase("notABlock"));
                return;
            }
            point2.put(player, target.getLocation());
            player.sendMessage(phrase("secondPointSet"));
            return;
        } else if (args[0].equalsIgnoreCase("-feet")) {

            point2.put(player, player.getLocation());
            player.sendMessage(phrase("secondPointSet"));
            return;
        }

        throw new InvalidCommandArgumentsException();
    }

    @Command(aliases = {"create", "save"},
            description = "command.create.description",
            help = {"command.create.help.0"},
            usage = "command.create.usage",
            permissions = {"mineresetlite.mine.create"},
            min = 1, max = -1, onlyPlayers = true)
    public void createMine(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        World world = null;
        Vector p1 = null;
        Vector p2 = null;

        if (point1.containsKey(player) && point2.containsKey(player)) {
            world = point1.get(player).getWorld();
            if (!world.equals(point2.get(player).getWorld())) {
                player.sendMessage(phrase("crossWorldSelection"));
                return;
            }
            p1 = point1.get(player).toVector();
            p2 = point2.get(player).toVector();
        }
        Location[] selection;
        try {
            selection = com.koletar.jj.mineresetlite.WorldEditSelection.get(plugin, player);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            player.sendMessage(ex.getMessage());
            return;
        }
        if (selection != null) {
            world = selection[0].getWorld();
            p1 = selection[0].toVector();
            p2 = selection[1].toVector();
        }
        if (p1 == null) {
            player.sendMessage(phrase("emptySelection"));
            return;
        }

        String name = StringTools.buildSpacedArgument(args);

        Mine[] mines = plugin.matchMines(name);
        if (mines.length > 0) {
            player.sendMessage(phrase("nameInUse", name));
            return;
        }

        if (p1.getX() > p2.getX()) {

            double x = p1.getX();
            p1.setX(p2.getX());
            p2.setX(x);
        }
        if (p1.getY() > p2.getY()) {
            double y = p1.getY();
            p1.setY(p2.getY());
            p2.setY(y);
        }
        if (p1.getZ() > p2.getZ()) {
            double z = p1.getZ();
            p1.setZ(p2.getZ());
            p2.setZ(z);
        }
        if (p1.getBlockY() < world.getMinHeight() || p2.getBlockY() >= world.getMaxHeight() - 2) {
            player.sendMessage(phrase("mineHeightInvalid"));
            return;
        }

        Mine newMine = new Mine(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(), p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(), name, world);
        plugin.mines.add(newMine);
        player.sendMessage(phrase("mineCreated", newMine));
        plugin.buffSave();
    }

    @Command(aliases = {"info", "i"},
            description = "command.info.description",
            help = {"command.info.help.0"},
            usage = "command.info.usage",
            permissions = {"mineresetlite.mine.info"},
            min = 1, max = -1, onlyPlayers = false)
    public void mineInfo(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        sender.sendMessage(phrase("mineInfoName", mines[0]));
        sender.sendMessage(phrase("mineInfoWorld", mines[0].getWorld()));

        StringBuilder csb = new StringBuilder();
        for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
            csb.append(entry.getValue() * 100);
            csb.append("% ");
            csb.append(entry.getKey());
            csb.append(", ");
        }
        if (csb.length() > 2) {
            csb.delete(csb.length() - 2, csb.length() - 1);
        }
        sender.sendMessage(phrase("mineInfoComposition", csb));
        if (mines[0].getResetDelay() != 0) {
            sender.sendMessage(phrase("mineInfoResetDelay", mines[0].getResetDelay()));
            sender.sendMessage(phrase("mineInfoTimeUntilReset", mines[0].getTimeUntilReset()));
        }
        sender.sendMessage(phrase("mineInfoSilence", mines[0].isSilent()));
        if (mines[0].getResetWarnings().size() > 0) {
            sender.sendMessage(phrase("mineInfoWarningTimes", StringTools.buildList(mines[0].getResetWarnings(), "", ", ")));
        }
        if (mines[0].getSurface() != null) {
            sender.sendMessage(phrase("mineInfoSurface", mines[0].getSurface()));
        }
        if (mines[0].getFillMode()) {
            sender.sendMessage(phrase("mineInfoFillMode"));
        }
    }

    @Command(aliases = {"set", "add", "+"},
            description = "command.set.description",
            help = {"command.set.help.0"},
            usage = "command.set.usage",
            permissions = {"mineresetlite.mine.composition"},
            min = 3, max = -1, onlyPlayers = false)
    public void setComposition(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }

        SerializableBlock parsedBlock;
        try {
            parsedBlock = new SerializableBlock(args[args.length - 2]);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(phrase("unknownBlock"));
            return;
        }

        String percentageS = args[args.length - 1];
        if (!percentageS.endsWith("%")) {
            sender.sendMessage(phrase("badPercentage"));
            return;
        }
        StringBuilder psb = new StringBuilder(percentageS);
        psb.deleteCharAt(psb.length() - 1);
        double percentage = 0;
        try {
            percentage = Double.valueOf(psb.toString());
        } catch (NumberFormatException nfe) {
            sender.sendMessage(phrase("badPercentage"));
            return;
        }
        if (!Double.isFinite(percentage) || percentage > 100 || percentage <= 0) {
            sender.sendMessage(phrase("badPercentage"));
            return;
        }
        percentage = percentage / 100;
        SerializableBlock block = parsedBlock;
        Double oldPercentage = mines[0].getComposition().get(block);
        double total = 0;
        for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
            if (!entry.getKey().equals(block)) {
                total += entry.getValue();
            } else {
                block = entry.getKey();
            }
        }
        total += percentage;
        if (total > 1) {
            sender.sendMessage(phrase("insaneCompositionChange"));
            if (oldPercentage == null) {
                mines[0].getComposition().remove(block);
            } else {
                mines[0].getComposition().put(block, oldPercentage);
            }
            return;
        }
        mines[0].getComposition().put(block, percentage);
        sender.sendMessage(phrase("mineCompositionSet", mines[0], percentage * 100, block, (1 - mines[0].getCompositionTotal()) * 100));
        plugin.buffSave();
    }

    @Command(aliases = {"unset", "remove", "-"},
            description = "command.unset.description",
            help = {"command.unset.help.0"},
            usage = "command.unset.usage",
            permissions = {"mineresetlite.mine.composition"},
            min = 2, max = -1, onlyPlayers = false)
    public void unsetComposition(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 1));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }

        SerializableBlock parsedBlock;
        try {
            parsedBlock = new SerializableBlock(args[args.length - 1]);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(phrase("unknownBlock"));
            return;
        }

        SerializableBlock block = parsedBlock;
        for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
            if (entry.getKey().equals(block)) {
                mines[0].getComposition().remove(entry.getKey());
                plugin.buffSave();
                sender.sendMessage(phrase("blockRemovedFromMine", mines[0], block, (1 - mines[0].getCompositionTotal()) * 100));
                return;
            }
        }
        sender.sendMessage(phrase("blockNotInMine", mines[0], block));
        plugin.buffSave();
    }

    @Command(aliases = {"reset", "r"},
            description = "command.reset.description",
            help = {"command.reset.help.0"},
            usage = "command.reset.usage",
            permissions = {"mineresetlite.mine.reset"},
            min = 1, max = -1, onlyPlayers = false)
    public void resetMine(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args).replace(" -s", ""));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        if (args[args.length - 1].equalsIgnoreCase("-s")) {

            mines[0].reset();
        } else {
            MineResetLite.broadcast(phrase("mineResetBroadcast", mines[0], sender), mines[0]);
            mines[0].reset();
        }
    }

    @Command(aliases = {"flag", "f"},
            description = "command.flag.description",
            help = {"command.flag.help.0", "command.flag.help.1", "command.flag.help.2", "command.flag.help.3", "command.flag.help.4", "command.flag.help.5"},
            usage = "command.flag.usage",
            permissions = {"mineresetlite.mine.flag"},
            min = 3, max = -1, onlyPlayers = false)
    public void flag(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        String setting = args[args.length - 2];
        String value = args[args.length - 1];
        if (setting.equalsIgnoreCase("resetEvery") || setting.equalsIgnoreCase("resetDelay")) {
            int delay;
            try {
                delay = Integer.valueOf(value);
            } catch (NumberFormatException nfe) {
                sender.sendMessage(phrase("badResetDelay"));
                return;
            }
            if (delay < 0) {
                sender.sendMessage(phrase("badResetDelay"));
                return;
            }
            mines[0].setResetDelay(delay);
            if (delay == 0) {
                sender.sendMessage(phrase("resetDelayCleared", mines[0]));
            } else {
                sender.sendMessage(phrase("resetDelaySet", mines[0], delay));
            }
            plugin.buffSave();
            return;
        } else if (setting.equalsIgnoreCase("resetWarnings") || setting.equalsIgnoreCase("resetWarning")) {
            String[] bits = value.split(",");
            List<Integer> warnings = mines[0].getResetWarnings();
            List<Integer> oldList = new LinkedList<Integer>(warnings);
            warnings.clear();
            for (String bit : bits) {
                try {
                    warnings.add(Integer.valueOf(bit));
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(phrase("badWarningList"));
                    return;
                }
            }

            for (Integer warning : warnings) {
                if (warning >= mines[0].getResetDelay()) {
                    sender.sendMessage(phrase("badWarningList"));
                    mines[0].setResetWarnings(oldList);
                    return;
                }
            }
            if (warnings.contains(0) && warnings.size() == 1) {
                warnings.clear();
                sender.sendMessage(phrase("warningListCleared", mines[0]));
                return;
            } else if (warnings.contains(0)) {
                sender.sendMessage(phrase("badWarningList"));
                mines[0].setResetWarnings(oldList);
                return;
            }
            sender.sendMessage(phrase("warningListSet", mines[0]));
            plugin.buffSave();
            return;
        } else if (setting.equalsIgnoreCase("surface")) {

            SerializableBlock parsedBlock;
            try {
                parsedBlock = new SerializableBlock(value);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(phrase("unknownBlock"));
                return;
            }
            Material m = parsedBlock.getMaterial();
            if (m.equals(Material.AIR)) {
                mines[0].setSurface(null);
                sender.sendMessage(phrase("surfaceBlockCleared", mines[0]));
                plugin.buffSave();
                return;
            }
            SerializableBlock block = parsedBlock;
            mines[0].setSurface(block);
            sender.sendMessage(phrase("surfaceBlockSet", mines[0]));
            plugin.buffSave();
            return;
        } else if (setting.equalsIgnoreCase("fill") || setting.equalsIgnoreCase("fillMode")) {

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
                mines[0].setFillMode(true);
                sender.sendMessage(phrase("fillModeEnabled"));
                plugin.buffSave();
                return;
            } else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
                mines[0].setFillMode(false);
                sender.sendMessage(phrase("fillModeDisabled"));
                plugin.buffSave();
                return;
            }
            sender.sendMessage(phrase("invalidFillMode"));
        } else if (setting.equalsIgnoreCase("isSilent") || setting.equalsIgnoreCase("silent") || setting.equalsIgnoreCase("silence")) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
                mines[0].setSilence(true);
                sender.sendMessage(phrase("mineIsNowSilent", mines[0]));
                plugin.buffSave();
                return;
            } else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
                mines[0].setSilence(false);
                sender.sendMessage(phrase("mineIsNoLongerSilent", mines[0]));
                plugin.buffSave();
                return;
            }
            sender.sendMessage(phrase("badBoolean"));
        }
        sender.sendMessage(phrase("unknownFlag"));
    }
    @Command(aliases = {"erase"},
            description = "command.erase.description",
            help = {"command.erase.help.0"},
            usage = "command.erase.usage",
            permissions = {"mineresetlite.mine.erase"},
            min = 1, max = -1, onlyPlayers = false)
    public void erase(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        plugin.eraseMine(mines[0]);
        sender.sendMessage(phrase("mineErased", mines[0]));
    }
    @Command(aliases = {"stagger"},
            description = "command.stagger.description",
            help = {"command.stagger.help.0"},
            usage = "command.stagger.usage",
            permissions = {"mineresetlite.mine.flag"},
            min = 2, max = 2, onlyPlayers = false)
    public void stagger(CommandSender sender, String[] args) {
        int spacing;
        int interval;
        try {
            spacing = Integer.parseInt(args[0]);
            interval = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(phrase("badStagger"));
            return;
        }
        long duration = interval * 60L;
        if (spacing <= 0 || interval <= 0 || spacing > duration) {
            sender.sendMessage(phrase("badStagger"));
            return;
        }
        long slots = duration / spacing;
        int count = 0;
        for (Mine mine : plugin.mines) {
            if (mine.getResetDelay() == interval) {
                mine.setTimeUntilResetSeconds((count % slots + 1) * spacing);
                count++;
            }
        }
        if (count == 0) {
            sender.sendMessage(phrase("noStaggerMines", interval));
            return;
        }
        plugin.buffSave();
        sender.sendMessage(phrase("minesStaggered", count, interval, spacing));
    }

    @Command(aliases = {"reschedule"},
            description = "command.reschedule.description",
            help = {"command.reschedule.help.0"},
            usage = "",
            permissions = {"mineresetlite.mine.flag"},
            min = 0, max = 0, onlyPlayers = false)
    public void reschedule(CommandSender sender, String[] args) {
        for (Mine mine : plugin.mines) {
            mine.setResetDelay(mine.getResetDelay());
        }
        plugin.buffSave();
        sender.sendMessage(phrase("rescheduled"));
    }
}
