package com.koletar.jj.mineresetlite;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public class Mine implements ConfigurationSerializable {
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private World world;
    private Map<SerializableBlock, Double> composition;
    private int resetDelay;
    private List<Integer> resetWarnings;
    private String name;
    private SerializableBlock surface;
    private boolean fillMode;
    private long resetClockSeconds;
    private boolean isSilent;

    public Mine(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String name, World world) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.name = name;
        this.world = world;
        composition = new HashMap<SerializableBlock, Double>();
        resetWarnings = new LinkedList<Integer>();
    }

    public Mine(Map<String, Object> me) {
        try {
            minX = (Integer) me.get("minX");
            minY = (Integer) me.get("minY");
            minZ = (Integer) me.get("minZ");
            maxX = (Integer) me.get("maxX");
            maxY = (Integer) me.get("maxY");
            maxZ = (Integer) me.get("maxZ");
        } catch (RuntimeException t) {
            throw new IllegalArgumentException("Error deserializing coordinate pairs");
        }
        try {
            world = Bukkit.getServer().getWorld((String) me.get("world"));
        } catch (RuntimeException t) {
            throw new IllegalArgumentException("Error finding world");
        }
        if (world == null) {
            Logger l = Bukkit.getLogger();
            l.severe("[MineResetLite] Unable to find a world! Please include these logger lines along with the stack trace when reporting this bug!");
            l.severe("[MineResetLite] Attempted to load world named: " + me.get("world"));
            l.severe("[MineResetLite] Worlds listed: " + StringTools.buildList(Bukkit.getWorlds(), "", ", "));
            throw new IllegalArgumentException("World was null!");
        }
        try {
            Map<String, Number> sComposition = (Map<String, Number>) me.get("composition");
            composition = new HashMap<SerializableBlock, Double>();
            for (Map.Entry<String, Number> entry : sComposition.entrySet()) {
                double chance = entry.getValue().doubleValue();
                if (!Double.isFinite(chance) || chance <= 0 || chance > 1) {
                    throw new IllegalArgumentException("Invalid block percentage");
                }
                composition.merge(new SerializableBlock(entry.getKey()), chance, Double::sum);
            }
        } catch (RuntimeException t) {
            throw new IllegalArgumentException("Error deserializing composition; migrate legacy IDs to modern material names", t);
        }
        if (getCompositionTotal() > 1 + 1e-12) throw new IllegalArgumentException("Composition exceeds 100%");
        name = (String) me.get("name");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing mine name");
        if (minX > maxX || minY > maxY || minZ > maxZ
                || minY < world.getMinHeight() || maxY >= world.getMaxHeight() - 2) {
            throw new IllegalArgumentException("Invalid mine bounds");
        }
        resetDelay = (Integer) me.get("resetDelay");
        if (resetDelay < 0) throw new IllegalArgumentException("Negative reset interval");
        List<?> warnings = (List<?>) me.get("resetWarnings");
        resetWarnings = new LinkedList<Integer>();
        for (Object warning : warnings) {
            try {
                int minutes = Integer.parseInt(warning.toString());
                if (minutes > 0 && minutes < resetDelay && !resetWarnings.contains(minutes)) {
                    resetWarnings.add(minutes);
                }
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Non-numeric reset warnings supplied");
            }
        }
        if (me.containsKey("surface")) {
            if (!me.get("surface").equals("")) {
                surface = new SerializableBlock((String) me.get("surface"));
            }
        }
        if (me.containsKey("fillMode")) {
            fillMode = (Boolean) me.get("fillMode");
        }
        if (me.containsKey("resetClockSeconds")) {
            resetClockSeconds = ((Number) me.get("resetClockSeconds")).longValue();
        } else if (me.containsKey("resetClock")) {
            resetClockSeconds = ((Number) me.get("resetClock")).longValue() * 60L;
        }

        if (resetDelay > 0 && (resetClockSeconds <= 0 || resetClockSeconds > resetDelay * 60L)) {
            resetClockSeconds = resetDelay * 60L;
        }
        if (me.containsKey("isSilent")) {
            isSilent = (Boolean) me.get("isSilent");
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> me = new HashMap<String, Object>();
        me.put("minX", minX);
        me.put("minY", minY);
        me.put("minZ", minZ);
        me.put("maxX", maxX);
        me.put("maxY", maxY);
        me.put("maxZ", maxZ);
        me.put("world", world.getName());

        Map<String, Double> sComposition = new HashMap<String, Double>();
        for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
            sComposition.put(entry.getKey().toString(), entry.getValue());
        }
        me.put("composition", sComposition);
        me.put("name", name);
        me.put("resetDelay", resetDelay);
        List<String> warnings = new LinkedList<String>();
        for (Integer warning : resetWarnings) {
            warnings.add(warning.toString());
        }
        me.put("resetWarnings", warnings);
        if (surface != null) {
            me.put("surface", surface.toString());
        } else {
            me.put("surface", "");
        }
        me.put("fillMode", fillMode);
        me.put("resetClock", getTimeUntilReset());
        me.put("resetClockSeconds", resetClockSeconds);
        me.put("isSilent", isSilent);
        return me;
    }

    public boolean getFillMode() {
        return fillMode;
    }

    public void setFillMode(boolean fillMode) {
        this.fillMode = fillMode;
    }

    public void setResetDelay(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Negative reset interval");
        resetDelay = minutes;
        resetWarnings.removeIf(warning -> warning >= minutes);
        resetClockSeconds = minutes * 60L;
    }

    public void setResetWarnings(List<Integer> warnings) {
        resetWarnings = new ArrayList<Integer>(warnings);
    }

    public List<Integer> getResetWarnings() {
        return resetWarnings;
    }

    public int getResetDelay() {
        return resetDelay;
    }

    public int getTimeUntilReset() {
        return (int) ((resetClockSeconds + 59) / 60);
    }

    public void setTimeUntilResetSeconds(long seconds) {
        if (seconds <= 0 || seconds > resetDelay * 60L) {
            throw new IllegalArgumentException("Next reset must be within the mine's reset interval");
        }
        resetClockSeconds = seconds;
    }

    public SerializableBlock getSurface() {
        return surface;
    }

    public void setSurface(SerializableBlock surface) {
        this.surface = surface;
    }

    public World getWorld() {
        return world;
    }

    public String getName() {
        return name;
    }

    public Map<SerializableBlock, Double> getComposition() {
        return composition;
    }

    public boolean isSilent() {
        return isSilent;
    }

    public void setSilence(boolean isSilent) {
        this.isSilent = isSilent;
    }

    public double getCompositionTotal() {
        double total = 0;
        for (Double d : composition.values()) {
            total += d;
        }
        return total;
    }

    public boolean isInside(Player p) {
        return isInside(p.getLocation());
    }

    public boolean isInside(Location l) {
        return isInside(l, 0);
    }

    public boolean isInside(Location l, int blocksAbove) {
        return l.getWorld().equals(world)
            && (l.getBlockX() >= minX && l.getBlockX() <= maxX)
            && (l.getBlockY() >= minY && l.getBlockY() <= maxY + blocksAbove)
            && (l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ);
    }

    public void reset() {

        SerializableBlock singleBlock = null;
        if (composition.isEmpty()) {
            singleBlock = new SerializableBlock(org.bukkit.Material.AIR);
        } else if (composition.size() == 1) {
            Map.Entry<SerializableBlock, Double> entry = composition.entrySet().iterator().next();
            if (entry.getValue() == 1.0) {
                singleBlock = entry.getKey();
            }
        }
        List<CompositionEntry> probabilityMap = singleBlock == null ? mapComposition(composition) : null;

        for (Player p : world.getPlayers()) {
            Location l = p.getLocation();
            if (isInside(l)) {
                l.setY(maxY + 1D);
                if (!p.teleport(l)) {
                    throw new IllegalStateException("Teleport cancelled for player: " + p.getName());
                }
            }
        }

        Random rand = singleBlock == null ? java.util.concurrent.ThreadLocalRandom.current() : null;
        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int y = minY; y <= maxY; ++y) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    if (!fillMode || block.getType().isAir()) {
                        if (y == maxY && surface != null) {
                            surface.apply(block);
                            continue;
                        }
                        if (singleBlock != null) {
                            singleBlock.apply(block);
                            continue;
                        }
                        double r = rand.nextDouble();
                        for (CompositionEntry ce : probabilityMap) {
                            if (r < ce.getChance()) {
                                ce.getBlock().apply(block);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void cron() {
        if (resetDelay == 0) {
            return;
        }
        if (resetClockSeconds > 0) {
            resetClockSeconds--;
        }
        if (resetClockSeconds == 0) {
            resetClockSeconds = resetDelay * 60L;
            reset();
            if (!isSilent) {
                MineResetLite.broadcast(Phrases.phrase("mineAutoResetBroadcast", this), this);
            }
            return;
        }
        for (Integer warning : resetWarnings) {
            if (warning * 60L == resetClockSeconds) {
                MineResetLite.broadcast(Phrases.phrase("mineWarningBroadcast", this, warning), this);
            }
        }
    }

    public static class CompositionEntry {
        private SerializableBlock block;
        private double chance;

        public CompositionEntry(SerializableBlock block, double chance) {
            this.block = block;
            this.chance = chance;
        }

        public SerializableBlock getBlock() {
            return block;
        }

        public double getChance() {
            return chance;
        }
    }

    public static ArrayList<CompositionEntry> mapComposition(Map<SerializableBlock, Double> compositionIn) {
        ArrayList<CompositionEntry> probabilityMap = new ArrayList<CompositionEntry>();
        Map<SerializableBlock, Double> composition = new HashMap<SerializableBlock, Double>(compositionIn);
        double max = 0;
        for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
            max += entry.getValue();
        }

        if (max < 1) {
            composition.merge(new SerializableBlock(org.bukkit.Material.AIR), 1 - max, Double::sum);
            max = 1;
        }
        double i = 0;
        for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet())  {
            double v = entry.getValue() / max;
            i += v;
            probabilityMap.add(new CompositionEntry(entry.getKey(), i));
        }
        if (!probabilityMap.isEmpty()) {
            CompositionEntry last = probabilityMap.get(probabilityMap.size() - 1);
            probabilityMap.set(probabilityMap.size() - 1, new CompositionEntry(last.getBlock(), 1.0));
        }
        return probabilityMap;
    }
}
