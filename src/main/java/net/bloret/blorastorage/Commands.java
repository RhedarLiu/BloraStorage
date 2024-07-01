package net.bloret.blorastorage;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {
    private final BloraStorage plugin;
    private static Component reloadMsg;
    private static Component noPermissionMsg;
    private static final MiniMessage mm = MiniMessage.miniMessage();


    public Commands(BloraStorage plugin) {
        this.plugin = plugin;
    }

    public static void setReloadMsg(String msg) {
        reloadMsg = mm.deserialize(msg);
    }

    public static void setNoPermissionMsg(String msg) {
        noPermissionMsg = mm.deserialize(msg);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        Audience audience = (Audience) sender;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("open")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                StorageGUI.openPlayerStorage(player);
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("blorastorage.reload")) {
                    plugin.reloadConfig();
                    BloraStorage.getInstance().loadConfig();
                    audience.sendMessage(reloadMsg);
                } else {
                    audience.sendMessage(noPermissionMsg);
                }
                return true;
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
