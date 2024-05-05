package net.bloret.blorastorage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

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

            // 创建表结构或更新数据库结构
            createOrUpdateTables();
            connection.commit();
        } catch (SQLException e) {
            handleSQLException(e);
        }
    }

    // 创建或更新数据库表结构
    private static void createOrUpdateTables() throws SQLException {
        String sqlPlayerStorage = "CREATE TABLE IF NOT EXISTS player_storage (" +
                "player_id VARCHAR(36) NOT NULL," +
                "slot INT NOT NULL," +
                "material VARCHAR(255) NOT NULL," +
                "amount INT NOT NULL," +
                "item_meta TEXT," +
                "PRIMARY KEY (player_id, slot)" +
                ");";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sqlPlayerStorage);
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
        String sqlReplace = "REPLACE INTO player_storage (player_id, slot, material, amount, item_meta) VALUES (?, ?, ?, ?, ?)";
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
                    psReplace.setString(5, itemStackToBase64(item));
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

    public static Inventory loadItems(Player player) {
        Inventory inventory = Bukkit.createInventory(player, StorageGUI.getStorageRows() * 9, StorageGUI.getGuiTitle());
        String sql = "SELECT slot, material, amount, item_meta FROM player_storage WHERE player_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int slot = rs.getInt("slot");
                    Material material = Material.getMaterial(rs.getString("material"));
                    int amount = rs.getInt("amount");
                    String itemMetaBase64 = rs.getString("item_meta");
                    if (material != null) {
                        ItemStack item = itemStackFromBase64(itemMetaBase64);
                        if (item != null) {
                            item.setAmount(amount);
                            inventory.setItem(slot, item);
                        }
                    }
                }
            }
        } catch (SQLException | IOException e) {
            handleSQLException(e);
        }
        return inventory;
    }

    public static void updateDatabaseStructure() {
        try (Statement statement = connection.createStatement()) {
            // 添加 item_meta 字段到 player_storage 表中
            String sql = "ALTER TABLE player_storage ADD COLUMN item_meta TEXT AFTER amount";
            statement.executeUpdate(sql);
            connection.commit();
        } catch (SQLException e) {
            handleSQLException(e);
        }
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

    private static void handleSQLException(Exception e) {
        e.printStackTrace();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
    }

    public static String itemStackToBase64(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stack.", e);
        }
    }

    public static ItemStack itemStackFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                return (ItemStack) dataInput.readObject();
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
