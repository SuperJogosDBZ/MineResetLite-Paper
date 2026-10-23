package com.koletar.jj.mineresetlite;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public final class SerializableBlock {
    private static final java.util.regex.Pattern LEGACY_MATERIAL =
            java.util.regex.Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*):(-?\\d+)$");
    private final BlockData data;
    private final String state;
    public SerializableBlock(String specification) {
        if (specification.matches("\\d+(:\\d+)?")) {
            throw new IllegalArgumentException("Migrate legacy numeric block IDs to material names: " + specification);
        }
        java.util.regex.Matcher legacy = LEGACY_MATERIAL.matcher(specification);
        if (legacy.matches()) {
            if (!legacy.group(2).equals("0")) {
                throw new IllegalArgumentException("Unsupported legacy block data; convert to a modern material name: " + specification);
            }
            specification = legacy.group(1);
        }
        data = Bukkit.createBlockData(specification.toLowerCase(java.util.Locale.ROOT));
        state = data.getAsString();
    }
    public SerializableBlock(Material material) { this(material.getKey().toString()); }
    public Material getMaterial() { return data.getMaterial(); }
    public void apply(Block block) { block.setBlockData(data, false); }
    @Override public String toString() { return state; }
    @Override public boolean equals(Object other) {
        return other instanceof SerializableBlock && state.equals(((SerializableBlock) other).state);
    }
    @Override public int hashCode() { return state.hashCode(); }
}
