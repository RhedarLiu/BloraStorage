package net.bloret.blorastorage;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {
    private final BloraStorage plugin;
    private static String reloadMsg;
    private static String noPermissionMsg;

    public Commands(BloraStorage plugin) {
        this.plugin = plugin;
    }

    public static void setReloadMsg(String msg) {
        reloadMsg = ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static void setNoPermissionMsg(String msg) {
        noPermissionMsg = ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("open")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                Player player = (Player) sender;
                StorageGUI.openPlayerStorage(player);
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("blorastorage.reload")) {
                    plugin.reloadConfig();
                    BloraStorage.getInstance().loadConfig();
                    sender.sendMessage(reloadMsg);
                    return true;
                } else {
                    sender.sendMessage(noPermissionMsg);
                    return true;
                }
            }
        }
        sender.sendMessage("Usage: /" + label + " [open|reload]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("open");
            subcommands.add("reload");
            return subcommands;
        }
        return null;
    }
}
