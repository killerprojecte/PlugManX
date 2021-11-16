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

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.rylinaux.plugman.messaging.MessageFormatter;
import com.rylinaux.plugman.util.BukkitCommandWrap;
import com.rylinaux.plugman.util.BukkitCommandWrap_Useless;
import com.rylinaux.plugman.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipException;

/**
 * Plugin manager for Bukkit servers.
 *
 * @author rylinaux
 */
public class PlugMan extends JavaPlugin {
    /**
     * The instance of the plugin
     */
    private static PlugMan instance = null;
    /**
     * HashMap that contains all mappings from resourcemaps.yml
     */
    private final HashMap<String, Map.Entry<Long, Boolean>> resourceMap = new HashMap<>();
    /**
     * Stores all file names + hashes for auto (re/un)load
     */
    private final HashMap<String, String> fileHashMap = new HashMap<>();
    /**
     * Stores all file names + plugin names for auto unload
     */
    private final HashMap<String, String> filePluginMap = new HashMap<>();
    /**
     * The command manager which adds all command we want so 1.13+ players can instantly tab-complete them
     */
    private BukkitCommandWrap bukkitCommandWrap = null;
    /**
     * List of plugins to ignore, partially.
     */
    private List<String> ignoredPlugins = null;
    /**
     * The message manager
     */
    private MessageFormatter messageFormatter = null;

    /**
     * Returns the instance of the plugin.
     *
     * @return the instance of the plugin
     */
    public static PlugMan getInstance() {
        return PlugMan.instance;
    }

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("PlugMan") == null) this.addPluginToList();
    }

    /**
     * For older server versions: Adds "PlugManX" as "PlugMan" to "lookupNames" field of "SimplePluginManager"
     * This is needed because of plugins which depend on "PlugMan", but server has "PlugManX" installed
     * Not needed on newer versions, because of new "provides" keyword in plugin.yml
     */
    private void addPluginToList() {
        Field lookupNamesField = null;

        try {
            lookupNamesField = SimplePluginManager.class.getDeclaredField("lookupNames");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (lookupNamesField == null) {
            Bukkit.getLogger().severe("无法加载PlugManX!\n请检查您是否在Bukkit环境下(包括Spigot,Paper Fork)");
            return;
        }

        lookupNamesField.setAccessible(true);

        HashMap<String, Plugin> lookupNames = null;
        try {
            lookupNames = (HashMap<String, Plugin>) lookupNamesField.get(Bukkit.getPluginManager());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (lookupNames == null) {
            Bukkit.getLogger().severe("无法加载PlugManX!\n请检查您是否在Bukkit环境下(包括Spigot,Paper Fork)");
            return;
        }

        lookupNames.put("PlugMan", this);
    }

    @Override
    public void onEnable() {
        PlugMan.instance = this;

        File messagesFile = new File("plugins" + File.separator + "PlugMan", "messages.yml");

        if (!messagesFile.exists()) this.saveResource("messages.yml", true);

        this.messageFormatter = new MessageFormatter();

        this.getCommand("plugman").setExecutor(new PlugManCommandHandler());
        this.getCommand("plugman").setTabCompleter(new PlugManTabCompleter());

        this.initConfig();

        String version;
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3].replace("_", ".");
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return;
        }

        /*if (version.contains("1.17") || version.contains("1.16") || version.contains("1.15") || version.contains("1.14") || version.contains("1.13")) {
            bukkitCommandWrap = new BukkitCommandWrap();
        } else {
            bukkitCommandWrap = new BukkitCommandWrap_Useless();
        }*/
        try {
            Class.forName("com.mojang.brigadier.CommandDispatcher");
            this.bukkitCommandWrap = new BukkitCommandWrap();
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            this.bukkitCommandWrap = new BukkitCommandWrap_Useless();
        }

        for (File file : new File("plugins").listFiles()) {
            if (file.isDirectory()) continue;
            if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) continue;
            String hash = null;
            try {
                hash = Files.asByteSource(file).hash(Hashing.md5()).toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.fileHashMap.put(file.getName(), hash);

            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
            } catch (IOException e) {
                if (e instanceof ZipException) {
                    System.out.println("检测到此插件可能已经损坏: " + file.getName());
                    continue;
                }
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

            this.filePluginMap.put(file.getName(), descriptionFile.getName());
        }

        boolean alerted = false;

        if (this.getConfig().getBoolean("auto-load.enabled", false)) {
            Bukkit.getLogger().warning("!!! 自动(重载/卸载)插件功能可能会导致插件损坏 请谨慎使用 !!!");
            Bukkit.getLogger().warning("如果出现问题 重启服务器可能可以修复!");
            alerted = true;
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> {
                if (!new File("plugins").isDirectory()) return;
                for (File file : Arrays.stream(new File("plugins").listFiles()).filter(File::isFile).filter(file -> file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")).collect(Collectors.toList()))
                    if (!this.fileHashMap.containsKey(file.getName())) {
                        Bukkit.getScheduler().runTask(this, () -> {
                            Bukkit.getConsoleSender().sendMessage(PluginUtil.load(file.getName().replace(".jar", "")));
                        });
                        String hash = null;
                        try {
                            hash = Files.asByteSource(file).hash(Hashing.md5()).toString();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        this.fileHashMap.put(file.getName(), hash);
                    }
            }, this.getConfig().getLong("auto-load.check-every-seconds") * 20, this.getConfig().getLong("auto-load.check-every-seconds") * 20);
        }

        if (this.getConfig().getBoolean("auto-unload.enabled", false)) {
            if (!alerted) {
                Bukkit.getLogger().warning("!!! 自动(重载/卸载)插件功能可能会导致插件损坏 请谨慎使用 !!!");
                Bukkit.getLogger().warning("如果出现问题 重启服务器可能可以修复!");
                alerted = true;
            }
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> {
                if (!new File("plugins").isDirectory()) return;
                for (String fileName : this.fileHashMap.keySet())
                    if (!new File("plugins", fileName).exists()) {
                        Plugin plugin = Bukkit.getPluginManager().getPlugin(this.filePluginMap.get(fileName));
                        if (plugin == null) {
                            this.fileHashMap.remove(fileName);
                            this.filePluginMap.remove(fileName);
                            continue;
                        }
                        if (PluginUtil.isIgnored(plugin)) continue;
                        this.fileHashMap.remove(fileName);
                        Bukkit.getScheduler().runTask(this, () -> {
                            Bukkit.getConsoleSender().sendMessage(PluginUtil.unload(plugin));
                        });
                    }
            }, this.getConfig().getLong("auto-unload.check-every-seconds") * 20, this.getConfig().getLong("auto-unload.check-every-seconds") * 20);
        }

        if (this.getConfig().getBoolean("auto-reload.enabled", false)) {
            if (!alerted) {
                Bukkit.getLogger().warning("!!! 自动(重载/卸载)插件功能可能会导致插件损坏 请谨慎使用 !!!");
                Bukkit.getLogger().warning("如果出现问题 重启服务器可能可以修复!");
                alerted = true;
            }
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> {
                if (!new File("plugins").isDirectory()) return;
                for (File file : Arrays.stream(new File("plugins").listFiles()).filter(File::isFile).filter(file -> file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")).collect(Collectors.toList())) {
                    if (!this.fileHashMap.containsKey(file.getName())) continue;
                    String hash = null;
                    try {
                        hash = Files.asByteSource(file).hash(Hashing.md5()).toString();
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    if (!hash.equalsIgnoreCase(this.fileHashMap.get(file.getName()))) {
                        Plugin plugin = Bukkit.getPluginManager().getPlugin(this.filePluginMap.get(file.getName()));
                        if (plugin == null) {
                            this.fileHashMap.remove(file.getName());
                            this.filePluginMap.remove(file.getName());
                            continue;
                        }

                        if (PluginUtil.isIgnored(plugin)) continue;

                        this.fileHashMap.remove(file.getName());
                        this.fileHashMap.put(file.getName(), hash);

                        Bukkit.getScheduler().runTask(this, () -> {
                            Bukkit.getConsoleSender().sendMessage(PluginUtil.unload(plugin));
                            Bukkit.getConsoleSender().sendMessage(PluginUtil.load(plugin.getName()));
                        });
                    }
                }
            }, this.getConfig().getLong("auto-reload.check-every-seconds") * 20, this.getConfig().getLong("auto-reload.check-every-seconds") * 20);
        }
    }

    @Override
    public void onDisable() {
        PlugMan.instance = null;
        this.messageFormatter = null;
        this.ignoredPlugins = null;
    }

    /**
     * Copy default config values
     */
    private void initConfig() {
        this.saveDefaultConfig();

        if (!this.getConfig().isSet("auto-load.enabled") || !this.getConfig().isSet("auto-unload.enabled") || !this.getConfig().isSet("auto-reload.enabled") || !this.getConfig().isSet("ignored-plugins")) {
            Bukkit.getLogger().severe("PlugManX的配置文件出现错误 正在重新生成...");
            new File("plugins" + File.separator + "PlugMan", "config.yml").renameTo(new File("plugins" + File.separator + "PlugMan", "config.yml.old-" + System.currentTimeMillis()));
            this.saveDefaultConfig();
            Bukkit.getLogger().info("新的配置文件已经创建!");
        }

        this.ignoredPlugins = this.getConfig().getStringList("ignored-plugins");

        File resourcemapFile = new File(this.getDataFolder(), "resourcemaps.yml");
        if (!resourcemapFile.exists()) this.saveResource("resourcemaps.yml", true);

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(resourcemapFile);
        this.resourceMap.clear();
        for (String name : cfg.getConfigurationSection("Resources").getKeys(false)) {
            if (name.equalsIgnoreCase("PlugMan")) continue;
            try {
                long id = cfg.getLong("Resources." + name + ".ID");
                boolean spigotmc = cfg.getBoolean("Resources." + name + ".spigotmc");
                this.resourceMap.put(name.toLowerCase(Locale.ROOT), new Map.Entry<Long, Boolean>() {
                    @Override
                    public Long getKey() {
                        return id;
                    }

                    @Override
                    public Boolean getValue() {
                        return spigotmc;
                    }

                    @Override
                    public Boolean setValue(Boolean value) {
                        return spigotmc;
                    }
                });
            } catch (Exception e) {
                this.getLogger().severe("尝试加载映射时出现错误 '" + name + "'");
                e.printStackTrace();
            }

        }

        this.resourceMap.put("plugman", new Map.Entry<Long, Boolean>() {
            @Override
            public Long getKey() {
                return 88135L;
            }

            @Override
            public Boolean getValue() {
                return true;
            }

            @Override
            public Boolean setValue(Boolean value) {
                return true;
            }
        });
    }

    /**
     * Returns the list of ignored plugins.
     *
     * @return the ignored plugins
     */
    public List<String> getIgnoredPlugins() {
        return this.ignoredPlugins;
    }

    /**
     * Returns the message manager.
     *
     * @return the message manager
     */
    public MessageFormatter getMessageFormatter() {
        return this.messageFormatter;
    }

    /**
     * Returns the command manager.
     *
     * @return the command manager
     */
    public BukkitCommandWrap getBukkitCommandWrap() {
        return this.bukkitCommandWrap;
    }

    public HashMap<String, Map.Entry<Long, Boolean>> getResourceMap() {
        return this.resourceMap;
    }

    public HashMap<String, String> getFilePluginMap() {
        return this.filePluginMap;
    }
}
