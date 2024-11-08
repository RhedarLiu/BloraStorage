package net.bloret.blorastorage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;

public class DatabaseHandler {
    private static Connection connection;

    public static void initializeDatabase(String host, int port, String database, String username, String password) {
        if (connection != null) return;
        try {
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, database);
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);
            createOrUpdateTables();
            connection.commit();
        } catch (SQLException e) {
            handleSQLException(e);
        }
    }

    private static void createOrUpdateTables() {
        String sqlPlayerStorage = "CREATE TABLE IF NOT EXISTS player_storage (" +
                "player_id VARCHAR(36) NOT NULL," +
                "slot INT NOT NULL," +
                "material VARCHAR(255) NOT NULL," +
                "amount INT NOT NULL," +
                "item_meta TEXT," +
                "PRIMARY KEY (player_id, slot)" +
                ");";
        executeUpdate(sqlPlayerStorage);
    }

    public static void updateDatabaseStructure() {
        try {
            DatabaseMetaData dbMetaData = connection.getMetaData();
            try (ResultSet rs = dbMetaData.getColumns(null, null, "player_storage", "item_meta")) {
                if (!rs.next()) {
                    String sql = "ALTER TABLE player_storage ADD COLUMN item_meta TEXT AFTER amount";
                    executeUpdate(sql);
                    connection.commit();
                }
            }
        } catch (SQLException e) {
            handleSQLException(e);
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
                    prepareStatement(psReplace, player, i, item);
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

    private static void prepareStatement(PreparedStatement ps, Player player, int slot, ItemStack item) throws SQLException {
        ps.setString(1, player.getUniqueId().toString());
        ps.setInt(2, slot);
        ps.setString(3, item.getType().name());
        ps.setInt(4, item.getAmount());
        ps.setString(5, itemStackToBase64(item));
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

    private static void rollbackTransaction() {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            handleSQLException(e);
        }
    }

    private static void handleSQLException(Exception e) {
        e.printStackTrace();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
    }

    private static void executeUpdate(String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            handleSQLException(e);
        }
    }

    public static String itemStackToBase64(ItemStack item) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stack.", e);
        }
    }

    public static ItemStack itemStackFromBase64(String data) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
