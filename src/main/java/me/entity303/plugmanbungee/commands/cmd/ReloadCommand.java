package me.entity303.plugmanbungee.commands.cmd;

import me.entity303.plugmanbungee.util.BungeePluginUtil;
import me.entity303.plugmanbungee.util.PluginResult;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReloadCommand {

    public void execute(CommandSender sender, String[] args) {
        if (args.length <= 0) {
            sendMessage(sender, "§c用法: §4/PlugManBungee reload <插件>");
            return;
        }

        String pluginName = args[0];

        PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();

        if (pluginManager.getPlugin(pluginName) == null) {
            sendMessage(sender, "§c没有找到同名的文件 §4" + pluginName + "§c!");
            return;
        }

        Map.Entry<PluginResult, PluginResult> pluginResults = BungeePluginUtil.reloadPlugin(pluginManager.getPlugin(pluginName));
        sendMessage(sender, pluginResults.getKey().getMessage());
        sendMessage(sender, pluginResults.getValue().getMessage());
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(new TextComponent("§8[§2PlugManBungee§8] §7" + message));
    }

    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = ProxyServer.getInstance().getPluginManager().getPlugins().stream().map(plugin -> plugin.getDescription().getName()).collect(Collectors.toList());

            List<String> realCompletions = new ArrayList<>();

            for (String com : completions) {
                if (com.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    realCompletions.add(com);
                }
            }

            return realCompletions.size() > 0 ? realCompletions : completions;
        }
        return new ArrayList<>();
    }
}
