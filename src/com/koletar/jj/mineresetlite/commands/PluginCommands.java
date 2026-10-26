package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.Command;
import com.koletar.jj.mineresetlite.MineResetLite;
import org.bukkit.command.CommandSender;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

public class PluginCommands {
    private MineResetLite plugin;

    public PluginCommands(MineResetLite plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"about"},
            description = "command.about.description",
            help = {"command.about.help.0"},
            permissions = {},
            min = 0, max = 0, onlyPlayers = false)
    public void about(CommandSender sender, String[] args) {
        sender.sendMessage(phrase("aboutTitle"));
        sender.sendMessage(phrase("aboutAuthors"));
        sender.sendMessage(phrase("aboutVersion", plugin.getDescription().getVersion()));
    }
}
