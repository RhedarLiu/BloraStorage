package net.bloret.blorastorage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class StorageGUI implements Listener {
    private static String guiTitle;
    private static boolean takeoutOnly = false;
    private static int storageRows = 6;
    private static String againstTakeoutOnlyMsg;

    public static void setAgainstTakeoutOnlyMsg(String msg) {
        againstTakeoutOnlyMsg = ChatColor.translateAlternateColorCodes('&', msg);
    }

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

    // 禁止 takeoutOnly = true 时玩家的物品放入操作
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!takeoutOnly) return;

        if (!event.getView().getTitle().equals(guiTitle)) return;

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();


        InventoryAction action = event.getAction();
        boolean isTopInventory = event.getRawSlot() < event.getView().getTopInventory().getSize();

        if (isTopInventory && (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR)) {
            event.setCancelled(true);
            player.sendMessage(againstTakeoutOnlyMsg);
            return;
        }

        if (!isTopInventory && action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            player.sendMessage(againstTakeoutOnlyMsg);
            return;
        }

        if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) {
            if (isTopInventory) {
                int hotbarButton = event.getHotbarButton();
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                    event.setCancelled(true);
                    player.sendMessage(againstTakeoutOnlyMsg);
                }
            }
        }
    }


    // 禁止 takeoutOnly = true 时玩家的物品拖动放入操作
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!takeoutOnly) return;

        if (!event.getView().getTitle().equals(guiTitle)) return;

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        int inventorySize = event.getView().getTopInventory().getSize();

        for (Integer slot : event.getRawSlots()) {
            if (slot < inventorySize) {
                event.setCancelled(true);
                player.sendMessage(againstTakeoutOnlyMsg);
                return;
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
