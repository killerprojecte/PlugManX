package me.entity303.plugmanbungee.main;

import me.entity303.plugmanbungee.commands.PlugManBungeeCommand;
import me.entity303.plugmanbungee.commands.PluginsCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class PlugManBungee extends Plugin implements Listener {
    private static PlugManBungee instance;

    @Override
    public void onEnable() {
        instance = this;

        ProxyServer.getInstance().getPluginManager().registerCommand(this, new PluginsCommand());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new PlugManBungeeCommand());

        for (int i = 1; i < 4; i++) {
            ProxyServer.getInstance().getScheduler().schedule(this, () -> {
                getLogger().severe("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                getLogger().severe("此版本的PlugManBungee还处于测试阶段 请不要指望此插件完美运行!");
                getLogger().severe("也不要指望每个功能都能正常使用!");
                getLogger().severe("汉化作者: unlimted");
                getLogger().severe("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            }, i, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onDisable() {
    }

    public static PlugManBungee getInstance() {
        return instance;
    }
}
