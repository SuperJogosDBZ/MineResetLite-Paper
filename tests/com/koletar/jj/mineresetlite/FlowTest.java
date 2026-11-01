package com.koletar.jj.mineresetlite;

import com.koletar.jj.mineresetlite.commands.MineCommands;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowTest {
    World world;
    Mine mine;
    MineResetLite plugin;
    CommandSender sender;
    MineCommands commands;

    @BeforeEach void setup() {
        Phrases.getInstance().initialize(Locale.ENGLISH);
        Phrases.getInstance().overrides(null);
        world = mock(World.class);
        mine = new Mine(0, 0, 0, 0, 0, 0, "test", world);
        mine.setResetDelay(10);
        plugin = mock(MineResetLite.class, RETURNS_DEEP_STUBS);
        when(plugin.matchMines("test")).thenReturn(new Mine[]{mine});
        plugin.mines = new ArrayList<>(List.of(mine));
        sender = mock(CommandSender.class);
        commands = new MineCommands(plugin);
    }

    @Test void invalidWarningsLeaveOriginalUntouched() {
        mine.setResetWarnings(List.of(5, 1));
        for (String value : List.of("3,bad", "-1", "2,", "10", "0,1")) {
            commands.flag(sender, new String[]{"test", "resetWarnings", value});
            assertEquals(List.of(5, 1), mine.getResetWarnings());
        }
        verify(plugin, never()).buffSave();
    }
    @Test void clearingWarningsWorksWithAutomaticResetDisabledAndSaves() {
        mine.setResetDelay(0);
        commands.flag(sender, new String[]{"test", "resetWarnings", "0"});
        assertTrue(mine.getResetWarnings().isEmpty());
        verify(plugin).buffSave();
    }
    @Test void warningDuplicatesDoNotProduceRepeatedBroadcasts() {
        commands.flag(sender, new String[]{"test", "resetWarnings", "1,5,1"});
        assertEquals(List.of(5,1), mine.getResetWarnings());
        verify(plugin).buffSave();
    }
    @Test void reducingIntervalRemovesUnreachableWarnings() {
        mine.setResetWarnings(List.of(5,1));
        mine.setResetDelay(3);
        assertEquals(List.of(1),mine.getResetWarnings());
        assertEquals(3,mine.getTimeUntilReset());
    }
    @Test void invalidBooleanProducesOneError() {
        commands.flag(sender, new String[]{"test", "fillMode", "bad"});
        verify(sender, times(1)).sendMessage(anyString());
        verify(plugin, never()).buffSave();
    }
    @Test void resetFlagIsCaseInsensitiveAndOnlyStrippedAtTheEnd() {
        Mine target = mock(Mine.class);
        when(plugin.matchMines("test")).thenReturn(new Mine[]{target});
        commands.resetMine(sender, new String[]{"test", "-S"});
        verify(target).reset();
    }
    @Test void failedDeletionIsNotReportedAsSuccess() {
        when(plugin.eraseMine(mine)).thenReturn(false);
        commands.erase(sender,new String[]{"test"});
        verify(sender).sendMessage(Phrases.phrase("mineEraseFailed",mine));
    }
    @Test void cancelledTeleportPreventsBlockChanges() {
        Player player = mock(Player.class);
        when(world.getPlayers()).thenReturn(List.of(player));
        when(player.getLocation()).thenReturn(new Location(world,0,0,0,45,20));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createBlockData(anyString())).thenAnswer(inv -> data(inv.getArgument(0)));
            assertThrows(IllegalStateException.class,mine::reset);
            verify(world,never()).getBlockAt(anyInt(),anyInt(),anyInt());
        }
    }
    @Test void explicitAirAndUnassignedAirAreAddedTogether() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createBlockData(anyString())).thenAnswer(inv -> data(inv.getArgument(0)));
            SerializableBlock air = new SerializableBlock("minecraft:air");
            SerializableBlock stone = new SerializableBlock("minecraft:stone");
            var distribution = Mine.mapComposition(Map.of(air,0.2,stone,0.3));
            double previous=0, airChance=0, stoneChance=0;
            for (var entry:distribution) {
                double chance=entry.getChance()-previous;
                if(entry.getBlock().equals(air)) airChance+=chance;
                else stoneChance+=chance;
                previous=entry.getChance();
            }
            assertEquals(0.7,airChance,1e-12);
            assertEquals(0.3,stoneChance,1e-12);
            assertEquals(1.0,previous);
        }
    }
    @Test void percentagesRoundOnlyForDisplay() {
        assertEquals("33.33",Phrases.findName(100.0/3));
        assertEquals("50",Phrases.findName(50.0));
        assertEquals("12.5",Phrases.findName(12.5));
    }
    @Test void timersUseSecondsAndRoundMinutesUp() {
        mine.setTimeUntilResetSeconds(61);
        assertEquals(2,mine.getTimeUntilReset());
        mine.cron();
        assertEquals(1,mine.getTimeUntilReset());
        assertThrows(IllegalArgumentException.class,()->mine.setTimeUntilResetSeconds(601));
    }
    @Test void creationAllowsPartialNamesAndNormalizesReversedSelection() throws Exception {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getLocation()).thenReturn(new Location(world,3,4,5), new Location(world,0,1,2));
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(plugin.isMineNameAvailable("tes")).thenReturn(true);
        commands.setPoint1(player,new String[]{"-feet"});
        commands.setPoint2(player,new String[]{"-feet"});
        commands.createMine(player,new String[]{"tes"});
        assertEquals(2,plugin.mines.size());
        Mine created=plugin.mines.get(1);
        assertEquals("tes",created.getName());
        assertTrue(created.isInside(new Location(world,0,1,2)));
        assertTrue(created.isInside(new Location(world,3,4,5)));
        assertFalse(created.isInside(new Location(world,4,4,5)));
        verify(plugin).buffSave();
    }
    @Test void validWorldEditSelectionWinsOverOldManualSelection() throws Exception {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getLocation()).thenReturn(new Location(world,0,0,0),new Location(mock(World.class),0,0,0));
        commands.setPoint1(player,new String[]{"-feet"});
        commands.setPoint2(player,new String[]{"-feet"});
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(plugin.isMineNameAvailable("other")).thenReturn(true);
        try (MockedStatic<WorldEditSelection> selections=mockStatic(WorldEditSelection.class)) {
            selections.when(()->WorldEditSelection.get(plugin,player)).thenReturn(new Location[]{
                new Location(world,1,1,1),new Location(world,2,2,2)});
            commands.createMine(player,new String[]{"other"});
            assertEquals(2,plugin.mines.size());
            assertTrue(plugin.mines.get(1).isInside(new Location(world,1,1,1)));
        }
    }
    @Test void localAnnouncementReachesTeleportedPlayer() {
        Player player=mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(world,0,1,0));
        when(world.getPlayers()).thenReturn(List.of(player));
        assertFalse(mine.isInside(player.getLocation()));
        try (MockedStatic<Config> config=mockStatic(Config.class);
             MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)) {
            config.when(Config::getBroadcastNearbyOnly).thenReturn(true);
            bukkit.when(Bukkit::getLogger).thenReturn(mock(java.util.logging.Logger.class));
            MineResetLite.broadcast("done",mine);
            verify(player).sendMessage("done");
        }
    }
    @Test void automaticResetRestartsTimer() {
        Mine scheduled=spy(mine);
        scheduled.setSilence(true);
        scheduled.setTimeUntilResetSeconds(1);
        doNothing().when(scheduled).reset();
        scheduled.cron();
        assertEquals(10,scheduled.getTimeUntilReset());
        scheduled.cron();
        verify(scheduled,times(1)).reset();
    }
    @Test void failedAutomaticResetDoesNotRetryEverySecond() {
        Mine scheduled=spy(mine);
        scheduled.setTimeUntilResetSeconds(1);
        doThrow(new IllegalStateException("cancelled")).when(scheduled).reset();
        assertThrows(IllegalStateException.class,scheduled::cron);
        assertDoesNotThrow(scheduled::cron);
        verify(scheduled,times(1)).reset();
    }
    @Test void staggerReusesSlotsWithoutChangingOtherIntervals() {
        plugin.mines.clear();
        for(int i=0;i<4;i++) {
            Mine m=new Mine(0,0,0,0,0,0,"mine"+i,world);
            m.setResetDelay(1);
            plugin.mines.add(m);
        }
        plugin.mines.add(mine);
        commands.stagger(sender,new String[]{"20","1"});
        assertEquals(List.of(20L,40L,60L,20L,600L), plugin.mines.stream()
                .map(m->m.serialize().get("resetClockSeconds")).toList());
        verify(plugin).buffSave();
    }
    @Test void updatingCompositionReplacesExistingShare() {
        try (MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)) {
            bukkit.when(()->Bukkit.createBlockData(anyString())).thenAnswer(inv->data(inv.getArgument(0)));
            SerializableBlock stone=new SerializableBlock("stone");
            mine.getComposition().put(stone,0.8);
            commands.setComposition(sender,new String[]{"test","stone","90%"});
            assertEquals(0.9,mine.getComposition().get(stone));
            assertEquals(1,mine.getComposition().size());
            verify(plugin).buffSave();
        }
    }
    @Test void surfaceAndFillModePreserveExistingBlocks() {
        try (MockedStatic<Bukkit> bukkit=mockStatic(Bukkit.class)) {
            bukkit.when(()->Bukkit.createBlockData(anyString())).thenAnswer(inv->data(inv.getArgument(0)));
            SerializableBlock surface=new SerializableBlock("stone");
            mine.setSurface(surface);
            Block block=mock(Block.class);
            when(world.getBlockAt(0,0,0)).thenReturn(block);
            when(world.getPlayers()).thenReturn(List.of());
            Material solid=mock(Material.class);
            Material air=mock(Material.class);
            when(air.isAir()).thenReturn(true);
            when(block.getType()).thenReturn(solid);
            mine.setFillMode(true);
            mine.reset();
            verify(block,never()).setBlockData(any(BlockData.class),anyBoolean());
            when(block.getType()).thenReturn(air);
            mine.reset();
            verify(block).setBlockData(any(BlockData.class),eq(false));
        }
    }
    private static BlockData data(String state) {
        BlockData data=mock(BlockData.class);
        when(data.getAsString()).thenReturn(state);
        return data;
    }
}
