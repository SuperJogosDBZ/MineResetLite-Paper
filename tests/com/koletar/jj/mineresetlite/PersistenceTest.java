package com.koletar.jj.mineresetlite;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistenceTest {
    @TempDir Path directory;
    MineResetLite plugin;
    Mine mine;
    @BeforeEach void setup() throws Exception {
        plugin = mock(MineResetLite.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.isMineNameAvailable(anyString())).thenCallRealMethod();
        when(plugin.matchMines(anyString())).thenCallRealMethod();
        when(plugin.eraseMine(any())).thenCallRealMethod();
        doCallRealMethod().when(plugin).save();
        var files=MineResetLite.class.getDeclaredField("mineFiles");files.setAccessible(true);
        files.set(plugin,new HashMap<Mine,java.io.File>());
        var logger=MineResetLite.class.getDeclaredField("logger");logger.setAccessible(true);
        logger.set(plugin,mock(java.util.logging.Logger.class));
        World world=mock(World.class);when(world.getName()).thenReturn("world");
        mine=new Mine(0,0,0,1,1,1,"test",world);
        plugin.mines=new ArrayList<>(List.of(mine));
        Files.createDirectories(directory.resolve("mines"));
    }
    @Test void rejectsUnsafeAndCollidingNames() throws Exception {
        for(String name:List.of("../escape","a/b","a\\b","a:b","a&4b","   "))
            assertFalse(plugin.isMineNameAvailable(name),name);
        assertTrue(plugin.isMineNameAvailable("Mina Norte"));
        Files.writeString(directory.resolve("mines/MinaNorte.mine.yml"),"original");
        assertFalse(plugin.isMineNameAvailable("Mina Norte"));
        assertFalse(plugin.isMineNameAvailable("TEST"));
    }
    @Test void savesAndDeletesActualLoadedFilename() throws Exception {
        Path original=directory.resolve("mines/legacy-file.mine.yml");
        var files=MineResetLite.class.getDeclaredField("mineFiles");files.setAccessible(true);
        ((Map<Mine,java.io.File>)files.get(plugin)).put(mine,original.toFile());
        plugin.save();
        assertTrue(Files.readString(original).contains("name: test"));
        assertFalse(Files.exists(directory.resolve("mines/test.mine.yml")));
        assertTrue(plugin.eraseMine(mine));
        assertFalse(Files.exists(original));
        assertTrue(plugin.mines.isEmpty());
    }
    @Test void deletionFailureKeepsMineInMemory() throws Exception {
        Path target=directory.resolve("mines/test.mine.yml");Files.createDirectory(target);
        Files.writeString(target.resolve("keep"),"keep");
        assertFalse(plugin.eraseMine(mine));
        assertEquals(List.of(mine),plugin.mines);
    }
    @Test void configurationAddsDefaultsWithoutLosingExistingValues() throws Exception {
        Files.writeString(directory.resolve("config.yml"),"locale: pt\nbroadcast-in-world-only: true\ncustom: keep\n");
        Config.initConfig(directory.toFile());
        assertEquals("pt",Config.getLocale());assertTrue(Config.getBroadcastInWorldOnly());
        var config=YamlConfiguration.loadConfiguration(directory.resolve("config.yml").toFile());
        assertEquals("keep",config.getString("custom"));
        assertFalse(config.getBoolean("broadcast-nearby-only"));
    }
    @Test void exactNamesWinAndEmptyQueriesMatchNothing() {
        plugin.mines.add(new Mine(0,0,0,0,0,0,"test two",null));
        assertArrayEquals(new Mine[]{mine},plugin.matchMines("TEST"));
        assertEquals(2,plugin.matchMines("tes").length);
        assertEquals(0,plugin.matchMines("").length);
    }
}
