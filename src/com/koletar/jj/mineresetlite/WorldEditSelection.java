package com.koletar.jj.mineresetlite;

import java.lang.reflect.InvocationTargetException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class WorldEditSelection {
    private WorldEditSelection() {}

    public static Location[] get(MineResetLite plugin, Player player) {
        if (!plugin.hasWorldEdit()) return null;
        try {
            ClassLoader loader = plugin.getWorldEdit().getClass().getClassLoader();
            Class<?> adapter = loader.loadClass("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> we = loader.loadClass("com.sk89q.worldedit.WorldEdit");
            Object actor = adapter.getMethod("adapt", Player.class).invoke(null, player);
            Object instance = we.getMethod("getInstance").invoke(null);
            Object manager = we.getMethod("getSessionManager").invoke(instance);
            Object session = manager.getClass().getMethod("get", loader.loadClass("com.sk89q.worldedit.session.SessionOwner")).invoke(manager, actor);
            Object selectionWorld = session.getClass().getMethod("getSelectionWorld").invoke(session);
            if (selectionWorld == null) return null;
            Object region = session.getClass().getMethod("getSelection", loader.loadClass("com.sk89q.worldedit.world.World")).invoke(session, selectionWorld);
            if (!loader.loadClass("com.sk89q.worldedit.regions.CuboidRegion").isInstance(region)) {
                throw new IllegalArgumentException("MineResetLite supports cuboid selections only. Use //sel cuboid.");
            }
            String worldName = (String) selectionWorld.getClass().getMethod("getName").invoke(selectionWorld);
            World world = Bukkit.getWorld(worldName);
            if (world == null) throw new IllegalArgumentException("Selection world is not loaded: " + worldName);
            Object min = region.getClass().getMethod("getMinimumPoint").invoke(region);
            Object max = region.getClass().getMethod("getMaximumPoint").invoke(region);
            return new Location[] { location(world, min), location(world, max) };
        } catch (InvocationTargetException ex) {
            if (ex.getCause().getClass().getSimpleName().equals("IncompleteRegionException")) return null;
            throw new IllegalStateException("Cannot read WorldEdit selection", ex.getCause());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unsupported WorldEdit API (WorldEdit 7 is required)", ex);
        }
    }

    private static Location location(World world, Object vector) throws ReflectiveOperationException {
        Class<?> type = vector.getClass();
        return new Location(world, ((Number) type.getMethod("x").invoke(vector)).intValue(),
                ((Number) type.getMethod("y").invoke(vector)).intValue(),
                ((Number) type.getMethod("z").invoke(vector)).intValue());
    }
}
