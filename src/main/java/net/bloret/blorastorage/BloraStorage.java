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
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String database = config.getString("database.database");
        String username = config.getString("database.username");
        String password = config.getString("database.password");
        int storageRows = config.getInt("storage.gui.rows", 6);
        String reloadMsg = config.getString("messages.reload", "&aBloraStorage 已重载");
        Commands.setReloadMsg(reloadMsg);
        // 初始化数据库
        DatabaseHandler.initializeDatabase(host, port, database, username, password);

        // 获取并设置存储 GUI 标题和只取出模式
        String guiTitle = config.getString("storage.gui.title", "&aYour Personal Storage");
        boolean takeoutOnly = config.getBoolean("storage.takeoutOnly", false);
        StorageGUI.setupGuiTitle(guiTitle);
        StorageGUI.setTakeoutOnly(takeoutOnly);
        StorageGUI.setStorageRows(storageRows);
    }
}
