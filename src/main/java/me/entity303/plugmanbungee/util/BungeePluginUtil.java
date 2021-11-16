package me.entity303.plugmanbungee.util;

import me.entity303.plugmanbungee.main.PlugManBungee;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.logging.Level;

public class BungeePluginUtil {

    public static Map.Entry<PluginResult, PluginResult> reloadPlugin(Plugin plugin) {
        File file = plugin.getFile();


        PluginResult result1 = unloadPlugin(plugin);

        PluginResult result2 = loadPlugin(file);

        return new Map.Entry<PluginResult, PluginResult>() {
            @Override
            public PluginResult getKey() {
                return result1;
            }

            @Override
            public PluginResult getValue() {
                return result2;
            }

            @Override
            public PluginResult setValue(PluginResult value) {
                return result2;
            }
        };
    }

    public static PluginResult unloadPlugin(Plugin plugin) {
        boolean exception = false;
        PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();
        try {
            plugin.onDisable();
            for (Handler handler : plugin.getLogger().getHandlers()) {
                handler.close();
            }
        } catch (Throwable t) {
            PlugManBungee.getInstance().getLogger().log(Level.SEVERE, "禁用插件异常 '" + plugin.getDescription().getName() + "'", t);
            exception = true;
        }

        pluginManager.unregisterCommands(plugin);

        pluginManager.unregisterListeners(plugin);

        ProxyServer.getInstance().getScheduler().cancel(plugin);

        plugin.getExecutorService().shutdownNow();

        Field pluginsField = null;
        try {
            pluginsField = PluginManager.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return new PluginResult("§c尝试卸载插件时出现异常: §4无法加载 'plugins'部分§c, 查看控制台获得详细信息!", false);
        }

        Map<String, Plugin> plugins;

        try {
            plugins = (Map<String, Plugin>) pluginsField.get(pluginManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new PluginResult("§c尝试卸载插件时出现异常: §4无法加载 'plugins'部分§c, 查看控制台获得详细信息!", false);
        }

        plugins.remove(plugin.getDescription().getName());

        ClassLoader cl = plugin.getClass().getClassLoader();

        if (cl instanceof URLClassLoader) {

            try {

                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, null);

                Field pluginInitField = cl.getClass().getDeclaredField("desc");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, null);

                Field allLoadersField = cl.getClass().getDeclaredField("allLoaders");
                allLoadersField.setAccessible(true);
                Set allLoaders = (Set) allLoadersField.get(cl);
                allLoaders.remove(cl);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                PlugManBungee.getInstance().getLogger().log(Level.SEVERE, null, ex);

                return new PluginResult("§c尝试卸载插件时遇到错误: §4无法卸载类加载器§c, 查看控制台获得详细信息!", false);
            }

            try {

                ((URLClassLoader) cl).close();
            } catch (IOException ex) {
                PlugManBungee.getInstance().getLogger().log(Level.SEVERE, null, ex);
                return new PluginResult("§c尝试卸载插件时遇到错误: §4无法关闭类加载器§c, 查看控制台获得详细信息!", false);
            }

        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but lets try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();
        if (exception) {
            return new PluginResult("§c尝试卸载时出现未知错误 查看控制台获得详细信息!", false);
        } else {
            return new PluginResult("§7插件成功卸载!", true);
        }
    }

    public static PluginResult loadPlugin(File file) {
        PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();

        Field yamlField = null;
        try {
            yamlField = PluginManager.class.getDeclaredField("yaml");
            yamlField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return new PluginResult("§c尝试加载插件时出现错误: §4不能加载 'yaml' 部分§c, 查看控制台获得详细信息!", false);
        }

        Yaml yaml = null;
        try {
            yaml = (Yaml) yamlField.get(pluginManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new PluginResult("§c尝试加载插件时出现错误: §4不能加载 'yaml' 部分§c, 查看控制台获得详细信息!", false);
        }

        Field toLoadField = null;
        try {
            toLoadField = PluginManager.class.getDeclaredField("toLoad");
            toLoadField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return new PluginResult("§c尝试加载插件时出现错误: §4不能加载 'toLoad' 部分§c, 查看控制台获得详细信息!", false);
        }

        HashMap<String, PluginDescription> toLoad = null;
        try {
            toLoad = (HashMap<String, PluginDescription>) toLoadField.get(pluginManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new PluginResult("§c尝试加载插件时出现错误: §4不能加载 'toLoad' 部分§c, 查看控制台获得详细信息!", false);
        }

        if (toLoad == null) {
            toLoad = new HashMap<>();
        }

        if (file.isFile()) {
            PluginDescription desc;

            try (JarFile jar = new JarFile(file)) {
                JarEntry pdf = jar.getJarEntry("bungee.yml");
                if (pdf == null) {
                    pdf = jar.getJarEntry("plugin.yml");
                }

                if (pdf == null) {
                    return new PluginResult("§c尝试加载插件时出现错误: §4插件不含有 plugin.yml 或 bungee.yml!", false);
                }

                //Preconditions.checkNotNull(pdf, "Plugin must have a plugin.yml or bungee.yml");

                try (InputStream in = jar.getInputStream(pdf)) {
                    desc = yaml.loadAs(in, PluginDescription.class);

                    if (desc.getName() == null) {
                        return new PluginResult("§c尝试加载插件时出现错误: §4插件不含有 plugin.yml/bungee.yml!", false);
                    }

                    if (desc.getMain() == null) {
                        return new PluginResult("§c尝试加载插件时出现错误: §4插件的主类并没有写在 plugin.yml/bungee.yml 中!", false);
                    }

                    if (pluginManager.getPlugin(desc.getName()) != null) {
                        return new PluginResult("§c尝试加载插件时出现错误: §4插件名称 '" + desc.getName() + "' 已经被使用!", false);
                    }

                    desc.setFile(file);

                    toLoad.put(desc.getName(), desc);
                }

                toLoadField.set(pluginManager, toLoad);

                pluginManager.loadPlugins();

                Plugin plugin = pluginManager.getPlugin(desc.getName());
                if (plugin == null)
                    return new PluginResult("§c尝试加载插件时出现错误: §4未知错误§c, 查看控制台获得详细信息!", false);
                plugin.onEnable();
            } catch (Exception ex) {
                ProxyServer.getInstance().getLogger().log(Level.WARNING, "不能加载此插件 " + file, ex);
                return new PluginResult("§c尝试加载插件时出现错误: §4未知错误§c, 查看控制台获得详细信息!", false);
            }
        }
        return new PluginResult("§7插件成功加载!", true);
    }
}
