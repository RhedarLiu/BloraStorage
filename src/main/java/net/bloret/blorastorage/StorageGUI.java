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
    private static int storageRows = 3; // 默认3行
    // 配置 GUI 标题
    public static void setupGuiTitle(String title) {
        guiTitle = ChatColor.translateAlternateColorCodes('&', title);
    }

    // 设置只取出模式
    public static void setTakeoutOnly(boolean takeoutOnlyStatus) {
        takeoutOnly = takeoutOnlyStatus;
    }

    // 设置存储行数
    public static void setStorageRows(int rows) {
        storageRows = rows;
    }

    // 获取存储行数
    public static int getStorageRows() {
        return storageRows;
    }

    // 获取 GUI 标题
    public static String getGuiTitle() {
        return guiTitle;
    }

    // 打开玩家的个人存储界面
    public static void openPlayerStorage(Player player) {
        Inventory storage = DatabaseHandler.loadItems(player);
        player.openInventory(storage);
    }

    // 处理界面点击事件
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (takeoutOnly && event.getView().getTitle().equals(guiTitle)) {
            // 检查操作发生在顶部库存（存储界面）或者是通过Shift点击尝试从背包移到顶部库存
            if (event.getClickedInventory() == event.getView().getTopInventory() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // 取消所有尝试向存储界面放入物品的操作，包括通过Shift点击
                if (event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_ONE || event.getAction() == InventoryAction.PLACE_SOME ||
                        event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
            }
        }
    }





    // 处理玩家关闭存储界面的事件
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(guiTitle)) {
            Player player = (Player) event.getPlayer();
            Inventory inventory = event.getInventory();
            // 无论是否开启只取出模式，都保存物品到数据库
            Bukkit.getScheduler().runTaskAsynchronously(BloraStorage.getInstance(), () -> {
                DatabaseHandler.saveItems(player, inventory);
            });
        }
    }

    // 注册事件监听器
    public static void registerEvents(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new StorageGUI(), plugin);
    }
}
