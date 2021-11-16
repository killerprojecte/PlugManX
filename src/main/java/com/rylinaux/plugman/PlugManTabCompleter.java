package com.rylinaux.plugman;

/*
 * #%L
 * PlugMan
 * %%
 * Copyright (C) 2010 - 2014 PlugMan
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.rylinaux.plugman.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Completes partial matches in command and plugin names.
 *
 * @author rylinaux
 */
public class PlugManTabCompleter implements TabCompleter {

    /**
     * Valid command names.
     */
    private static final String[] COMMANDS = {"check", "disable", "dump", "enable", "help", "info", "list", "load", "lookup", "reload", "restart", "unload", "usage"};

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        if (sender.isOp() || sender.hasPermission("plugman.admin") || sender.hasPermission("plugman." + args[0])) {

            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                String partialCommand = args[0];
                List<String> commands = new ArrayList<>(Arrays.asList(COMMANDS));
                StringUtil.copyPartialMatches(partialCommand, commands, completions);
            }

            if (args.length == 2) if (args[0].equalsIgnoreCase("load")) {
                List<String> files = new ArrayList<>();
                String partialPlugin = args[1];

                for (File pluginFile : new File("plugins").listFiles()) {
                    try {
                        if (pluginFile.isDirectory()) continue;

                        if (!pluginFile.getName().toLowerCase().endsWith(".jar"))
                            if (!new File("plugins", pluginFile.getName() + ".jar").exists()) continue;

                        JarFile jarFile = null;
                        try {
                            jarFile = new JarFile(pluginFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }

                        if (jarFile.getEntry("plugin.yml") == null) continue;

                        InputStream stream;
                        try {
                            stream = jarFile.getInputStream(jarFile.getEntry("plugin.yml"));
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }

                        if (stream == null) continue;

                        PluginDescriptionFile descriptionFile = null;
                        try {
                            descriptionFile = new PluginDescriptionFile(stream);
                        } catch (InvalidDescriptionException e) {
                            e.printStackTrace();
                            continue;
                        }

                        files.add(pluginFile.getName().substring(0, pluginFile.getName().length() - ".jar".length()));

                        for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
                            if (plugin.getName().equalsIgnoreCase(descriptionFile.getName()))
                                files.remove(pluginFile.getName().substring(0, pluginFile.getName().length() - ".jar".length()));
                    } catch (Exception ignored) {
                    }
                }

                StringUtil.copyPartialMatches(partialPlugin, files, completions);
            } else if (args[0].equalsIgnoreCase("lookup")) {
                String partialCommand = args[1];
                List<String> commands = PluginUtil.getKnownCommands().keySet().stream().filter(s -> !s.toLowerCase().contains(":")).collect(Collectors.toList());
                commands.remove("/");
                StringUtil.copyPartialMatches(partialCommand, commands, completions);
            } else if (args[0].equalsIgnoreCase("enable")) {
                String partialPlugin = args[1];
                List<String> plugins = PluginUtil.getDisabledPluginNames(false);
                StringUtil.copyPartialMatches(partialPlugin, plugins, completions);
            } else if (args[0].equalsIgnoreCase("disable")) {
                String partialPlugin = args[1];
                List<String> plugins = PluginUtil.getEnabledPluginNames(false);
                StringUtil.copyPartialMatches(partialPlugin, plugins, completions);
            } else {
                String partialPlugin = args[1];
                List<String> plugins = PluginUtil.getPluginNames(false);
                StringUtil.copyPartialMatches(partialPlugin, plugins, completions);
            }

            Collections.sort(completions);

            return completions;

        }

        return null;

    }

}
