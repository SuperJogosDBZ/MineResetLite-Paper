package com.koletar.jj.mineresetlite;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

public class CommandManager {
    private Map<String, Method> commands;
    private Map<Method, Object> instances;

    public CommandManager() {
        commands = new HashMap<String, Method>();
        instances = new HashMap<Method, Object>();
    }

    public void register(Class<?> cls, Object obj) {
        for (Method method : cls.getMethods()) {
            if (!method.isAnnotationPresent(Command.class)) {
                continue;
            }

            Command command = method.getAnnotation(Command.class);

            for (String alias : command.aliases()) {
                commands.put(alias, method);
            }
            instances.put(method, obj);
        }
    }

    @Command(aliases = {"help", "?"},
            description = "command.help.description",
            help = {"command.help.help.0"},
            usage = "command.help.usage",
            min = 0, max = -1)
    public void help(CommandSender sender, String[] args) {
        if (args.length >= 1) {

            if (commands.containsKey(args[0].toLowerCase())) {
                Command command = commands.get(args[0].toLowerCase()).getAnnotation(Command.class);
                sender.sendMessage(phrase("helpUsage", command.aliases()[0], usage(command)));
                for (String help : command.help()) {
                    sender.sendMessage(ChatColor.GRAY + phrase(help));
                }
                return;
            }
        }
        List<Method> seenMethods = new LinkedList<Method>();
        for (Map.Entry<String, Method> entry : commands.entrySet()) {
            if (!seenMethods.contains(entry.getValue())) {
                seenMethods.add(entry.getValue());
                Command command = entry.getValue().getAnnotation(Command.class);

                if ((command.onlyPlayers() && !(sender instanceof Player))) {
                    continue;
                }
                boolean may = command.permissions().length == 0;
                for (String perm : command.permissions()) {
                    if (sender.hasPermission(perm)) {
                        may = true;
                    }
                }
                if (!may) {
                    continue;
                }
                sender.sendMessage(phrase("helpUsage", command.aliases()[0], usage(command)));
                sender.sendMessage(phrase("helpDesc", phrase(command.description())));
            }
        }

    }

    private String usage(Command command) {
        return command.usage().isEmpty() ? "" : phrase(command.usage());
    }

    public void callCommand(String cmdName, CommandSender sender, String[] args) {

        Method method = commands.get(cmdName.toLowerCase());
        if (method == null) {
            sender.sendMessage(phrase("unknownCommand"));
            return;
        }

        Command command = method.getAnnotation(Command.class);

        if (!(command.min() <= args.length && (command.max() == -1 || command.max() >= args.length))) {
            sender.sendMessage(phrase("invalidArguments"));
            sender.sendMessage(phrase("invalidArgsUsage", command.aliases()[0], usage(command)));
            return;
        }

        if (command.onlyPlayers() && !(sender instanceof Player)) {
            sender.sendMessage(phrase("notAPlayer"));
            return;
        }

        boolean may = command.permissions().length == 0;
        for (String perm : command.permissions()) {
            if (sender.hasPermission(perm)) {
                may = true;
            }
        }
        if (!may) {
            sender.sendMessage(phrase("noPermission"));
            return;
        }

        Object[] methodArgs = {sender, args};
        try {
            method.invoke(instances.get(method), methodArgs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid methods on command!");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InvalidCommandArgumentsException) {
                sender.sendMessage(phrase("invalidArguments"));
                sender.sendMessage(phrase("invalidArgsUsage", command.aliases()[0], usage(command)));
            } else {
                e.printStackTrace();
                throw new RuntimeException("Invalid methods on command!");
            }
        }
    }
}
