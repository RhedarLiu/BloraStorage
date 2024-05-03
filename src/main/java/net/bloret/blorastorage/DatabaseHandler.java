package net.bloret.blorastorage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class DatabaseHandler {
    private static Connection connection;

    public static void initializeDatabase(String host, int port, String database, String username, String password) {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, database);
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);

            // 创建表结构
            createTables();
            connection.commit();
        } catch (SQLException e) {
            handleSQLException(e);
        }
    }

    // 初始创建数据库表结构
    private static void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_storage (" +
                "player_id VARCHAR(36) NOT NULL," +
                "slot INT NOT NULL," +
                "material VARCHAR(255) NOT NULL," +
                "amount INT NOT NULL," +
                "PRIMARY KEY (player_id, slot)" +
                ");";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                handleSQLException(e);
            }
        }
    }

    public static void saveItems(Player player, Inventory inventory) {
        String sqlReplace = "REPLACE INTO player_storage (player_id, slot, material, amount) VALUES (?, ?, ?, ?)";
        String sqlDelete = "DELETE FROM player_storage WHERE player_id = ? AND slot = ?";
        try (PreparedStatement psReplace = connection.prepareStatement(sqlReplace);
             PreparedStatement psDelete = connection.prepareStatement(sqlDelete)) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    psReplace.setString(1, player.getUniqueId().toString());
                    psReplace.setInt(2, i);
                    psReplace.setString(3, item.getType().name());
                    psReplace.setInt(4, item.getAmount());
                    psReplace.addBatch();
                } else {
                    psDelete.setString(1, player.getUniqueId().toString());
                    psDelete.setInt(2, i);
                    psDelete.addBatch();
                }
            }
            psReplace.executeBatch();
            psDelete.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            rollbackTransaction();
            handleSQLException(e);
        }
    }

    // 删除数据库中不在存储界面中的物品
    public static void removeItemsNotInInventory(Player player, Inventory inventory) {
        String sqlDelete = "DELETE FROM player_storage WHERE player_id = ? AND slot = ?";
        try (PreparedStatement psDelete = connection.prepareStatement(sqlDelete)) {
            for (int i = 0; i < StorageGUI.getStorageRows() * 9; i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    psDelete.setString(1, player.getUniqueId().toString());
                    psDelete.setInt(2, i);
                    psDelete.addBatch();
                }
            }
            psDelete.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            rollbackTransaction();
            handleSQLException(e);
        }
    }

    // 从数据库加载玩家物品
    public static Inventory loadItems(Player player) {
        Inventory inventory = Bukkit.createInventory(player, StorageGUI.getStorageRows() * 9, StorageGUI.getGuiTitle());
        String sql = "SELECT slot, material, amount FROM player_storage WHERE player_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int slot = rs.getInt("slot");
                    Material material = Material.getMaterial(rs.getString("material"));
                    int amount = rs.getInt("amount");
                    if (material != null) {
                        ItemStack item = new ItemStack(material, amount);
                        inventory.setItem(slot, item);
                    }
                }
            }
        } catch (SQLException e) {
            handleSQLException(e);
        }
        return inventory;
    }

    private static void rollbackTransaction() {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 处理 SQLException
    private static void handleSQLException(SQLException e) {
        e.printStackTrace();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
    }
}
