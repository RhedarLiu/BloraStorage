package net.bloret.blorastorage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class StorageGUI implements Listener {
    private static String guiTitle;
    private static boolean takeoutOnly = false;
    private static int storageRows = 6;

    public static void setupGuiTitle(String title) {
        guiTitle = ChatColor.translateAlternateColorCodes('&', title);
    }

    public static void setTakeoutOnly(boolean takeoutOnlyStatus) {
        takeoutOnly = takeoutOnlyStatus;
    }

    public static void setStorageRows(int rows) {
        storageRows = rows;
    }

    public static int getStorageRows() {
        return storageRows;
    }

    public static String getGuiTitle() {
        return guiTitle;
    }

    public static void openPlayerStorage(Player player) {
        Inventory storage = DatabaseHandler.loadItems(player);
        player.openInventory(storage);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (takeoutOnly && event.getView().getTitle().equals(guiTitle)) {
            if (event.getClickedInventory() == event.getView().getTopInventory() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_ONE || event.getAction() == InventoryAction.PLACE_SOME ||
                        event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(guiTitle)) {
            Player player = (Player) event.getPlayer();
            Inventory inventory = event.getInventory();
            Bukkit.getScheduler().runTaskAsynchronously(BloraStorage.getInstance(), () -> {
                DatabaseHandler.saveItems(player, inventory);
            });
        }
    }

    public static void registerEvents(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new StorageGUI(), plugin);
    }
}
