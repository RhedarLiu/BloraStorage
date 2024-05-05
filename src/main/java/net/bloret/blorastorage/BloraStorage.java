package net.bloret.blorastorage;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class BloraStorage extends JavaPlugin {
    private static BloraStorage instance;

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        // 注册 StorageGUI 事件监听器
        StorageGUI.registerEvents(this);

        // 设置命令相关
        this.getCommand("blorastorage").setExecutor(new Commands(this));
        this.getCommand("blorastorage").setTabCompleter(new Commands(this));

        // 确保数据库结构是最新的
        DatabaseHandler.updateDatabaseStructure();
    }

    @Override
    public void onDisable() {
        DatabaseHandler.closeConnection();  // 关闭数据库连接
        instance = null;
    }

    public static BloraStorage getInstance() {
        return instance;
    }

    public void loadConfig() {
        instance = this;
        this.saveDefaultConfig();  // 保存默认配置文件（如果尚不存在）

        // 从config.yml读取数据库配置
        FileConfiguration config = this.getConfig();

        // 数据库相关配置
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String database = config.getString("database.database");
        String username = config.getString("database.username");
        String password = config.getString("database.password");
        DatabaseHandler.initializeDatabase(host, port, database, username, password);

        // 存储箱相关配置
        int storageRows = config.getInt("storage.gui.rows", 6);
        String guiTitle = config.getString("storage.gui.title", "&x&1&8&1&8&2&5&l云端存储");
        boolean takeoutOnly = config.getBoolean("storage.takeoutOnly", false);
        StorageGUI.setupGuiTitle(guiTitle);
        StorageGUI.setTakeoutOnly(takeoutOnly);
        StorageGUI.setStorageRows(storageRows);

        // 消息相关配置
        String reloadMsg = config.getString("messages.reload", "<color:#f38ba8>BloraStorage 已重载！</color>");
        Commands.setReloadMsg(reloadMsg);
        String noPermissionMsg = config.getString("messages.noPermission", "<color:#f38ba8>你没有权限执行此命令！</color>");
        Commands.setNoPermissionMsg(noPermissionMsg);
        String againstTakeoutOnlyMsg = config.getString("messages.againstTakeoutOnly", "<color:#f38ba8>你不能将物品放入存储界面！</color>");
        StorageGUI.setAgainstTakeoutOnlyMsg(againstTakeoutOnlyMsg);
    }
}
