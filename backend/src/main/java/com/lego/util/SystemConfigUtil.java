package com.lego.util;

import com.lego.pojo.SystemConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ClassName: SystemConfigUtil
 * Package: com.lego.util
 * Description:
 *
 * @Author michael.zhu
 * @Create 3/30/2026 12:57 PM
 * @Version 1.0
 */
public class SystemConfigUtil {

    private static volatile SystemConfigUtil instance;

    // 前端管理员密码
    private String adminPassword;

    // 前端最大server数量
    private Integer maxServerCount;

    // 前端模版最大table数量
    private Integer maxTableCount;

    // 前端模版table最大字段数量
    private Integer maxTableFieldCount;

    // 数据库保留天数
    private Integer databaseRetentionDays;

    // 数据采集线程数量
    private Integer dataCollectionThreads;

    // 数据保存线程数量
    private Integer dataSaveThreads;

    // 数据保存间隔时间（秒）
    private Integer dataSaveInterval;

    // 数据保存批量写入大小
    private Integer dataSaveBatchSize;

    // 日志级别
    private Integer logLevel;

    private SystemConfigUtil() {
        loadConfig();
    }

    // 双重检查锁优化（可选）
    public static SystemConfigUtil getInstance() {
        if (instance == null) {
            synchronized (SystemConfigUtil.class) {
                if (instance == null) {
                    instance = new SystemConfigUtil();
                }
            }
        }
        return instance;
    }


    /**
     * 检查系统配置是否就绪
     *
     * @return true 表示配置已就绪，false 表示未就绪
     */
    public boolean isSystemConfigReady() {
        File configFile = new File(FIlepathUtil.getSystemConfigPath());
        if (!configFile.exists()) {
            return false;
        }

        return this.adminPassword != null &&
                this.dataCollectionThreads != null &&
                this.dataSaveThreads != null &&
                this.dataSaveInterval != null;
    }

    /**
     * 删除系统配置文件
     */
    public static synchronized boolean deleteSystemConfig() {
        File configFile = new File(FIlepathUtil.getSystemConfigPath());
        if (configFile.exists()) {
            if (configFile.delete()) {
                System.out.println("System config file deleted successfully");
                DBUtil.logInfo("app", "System config file deleted");

                // 重置实例和配置
                synchronized (SystemConfigUtil.class) {
                    if (instance != null) {
                        instance = null;
                    }
                }
                return true;
            } else {
                System.err.println("Failed to delete system config file");
                DBUtil.logError("app", "Failed to delete system config file");
            }
        }
        return true;
    }

    /**
     * 获取数据库保留天数的方法
     * 该方法用于从配置或数据库中获取数据保留的天数设置
     *
     * @return Integer 返回数据库保留的天数，如果获取失败则返回null
     */
    public static Integer getDatabaseRetentionDays() {
        return getInstance().databaseRetentionDays;
    }

    /**
     * 获取数据采集线程数量
     *
     * @return 数据采集线程数量
     */
    public static Integer getDataCollectionThreads() {
        return getInstance().dataCollectionThreads;
    }

    /**
     * 获取数据保存线程数量
     *
     * @return 数据保存线程数量
     */
    public static Integer getDataSaveThreads() {
        return getInstance().dataSaveThreads;
    }

    /**
     * 获取数据保存间隔时间（秒）
     *
     * @return 数据保存间隔时间
     */
    public static Integer getDataSaveInterval() {
        return getInstance().dataSaveInterval;
    }

    /**
     * 获取数据保存批量写入大小
     *
     * @return 数据保存批量写入大小
     */
    public static Integer getDataSaveBatchSize() {
        return getInstance().dataSaveBatchSize;
    }

    /**
     * 获取日志级别
     *
     * @return 日志级别
     */
    public static Integer getLogLevel() {
        return getInstance().logLevel;
    }

    /**
     * 保存系统配置到文件（使用传入的配置对象）
     *
     * @param config 系统配置对象
     * @return true 表示保存成功，false 表示失败
     */
    public synchronized boolean saveSystemConfig(SystemConfig config) {
        if (config == null) {
            return false;
        }

        // 先更新内存中的配置
        // 前端配置从前端获取
        this.adminPassword = config.getAdminPassword();
        this.maxServerCount = config.getMaxServerCount();
        this.maxTableCount = config.getMaxTableCount();
        this.maxTableFieldCount = config.getMaxTableFieldCount();
        // 后端配置默认，可以在配置文件中修改
        this.databaseRetentionDays = 15;
        this.dataCollectionThreads = 20;
        this.dataSaveThreads = 1;
        this.dataSaveInterval = 20;
        this.dataSaveBatchSize = 2000;
        this.logLevel = 0;

        Properties props = new Properties();

        if (this.adminPassword != null) {
            props.setProperty("admin.password", this.adminPassword);
        }
        if (this.maxServerCount != null) {
            props.setProperty("max.server.count", this.maxServerCount.toString());
        }
        if (this.maxTableCount != null) {
            props.setProperty("max.table.count", this.maxTableCount.toString());
        }
        if (this.maxTableFieldCount != null) {
            props.setProperty("max.table.field.count", this.maxTableFieldCount.toString());
        }
        if (this.databaseRetentionDays != null) {
            props.setProperty("database.retenion.days", this.databaseRetentionDays.toString());
        }
        if (this.dataCollectionThreads != null) {
            props.setProperty("data.collection.threads", this.dataCollectionThreads.toString());
        }
        if (this.dataSaveThreads != null) {
            props.setProperty("data.save.threads", this.dataSaveThreads.toString());
        }
        if (this.dataSaveInterval != null) {
            props.setProperty("data.save.interval", this.dataSaveInterval.toString());
        }
        if (this.dataSaveBatchSize != null) {
            props.setProperty("data.save.batch.size", this.dataSaveBatchSize.toString());
        }
        if (this.logLevel != null) {
            props.setProperty("log.level", this.logLevel.toString());
        }

        File configFile = new File(FIlepathUtil.getSystemConfigPath());

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "System Configuration - Saved via Web UI");

            System.out.println("System config saved: adminPassword=" +
                    maskPassword(this.adminPassword) +
                    ", maxServerCount=" + this.maxServerCount +
                    ", maxTableCount=" + this.maxTableCount +
                    ", maxTableFieldCount=" + this.maxTableFieldCount +
                    ", databaseRetenionDays=" + this.databaseRetentionDays + "days" +
                    ", collectionThreads=" + this.dataCollectionThreads +
                    ", saveThreads=" + this.dataSaveThreads +
                    ", interval=" + this.dataSaveInterval + "s" +
                    ", batchSize=" + this.dataSaveBatchSize +
                    ", logLevel=" + this.logLevel);
            DBUtil.logInfo("app", "System config saved to file");

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            DBUtil.logError("app", "Failed to save system config: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从配置文件加载配置
     */
    private synchronized void loadConfig() {
        Properties props = new Properties();
        File configFile = new File(FIlepathUtil.getSystemConfigPath());

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);

                this.adminPassword = props.getProperty("admin.password", "admin");
                this.maxServerCount = Integer.parseInt(
                        props.getProperty("max.server.count", "10")
                );
                this.maxTableCount = Integer.parseInt(
                        props.getProperty("max.table.count", "20")
                );
                this.maxTableFieldCount = Integer.parseInt(
                        props.getProperty("max.table.field.count", "20")
                );
                this.databaseRetentionDays = Integer.parseInt(
                        props.getProperty("database.retenion.days", "15")
                );
                this.dataCollectionThreads = Integer.parseInt(
                        props.getProperty("data.collection.threads", "20")
                );
                this.dataSaveThreads = Integer.parseInt(
                        props.getProperty("data.save.threads", "1")
                );
                this.dataSaveInterval = Integer.parseInt(
                        props.getProperty("data.save.interval", "20")
                );
                this.dataSaveBatchSize = Integer.parseInt(
                        props.getProperty("data.save.batch.size", "5000")
                );
                this.logLevel = Integer.parseInt(
                        props.getProperty("log.level", "0")
                );

                String configLog = String.format(
                        "System config loaded: adminPassword=%s, maxServerCount=%d, maxTableCount=%d, maxTableFieldCount=%d, databaseRetentionDays=%d, dataCollectionThreads=%d, dataSaveThreads=%d, dataSaveInterval=%ds, batchSize=%d, logLevel=%d",
                        maskPassword(this.adminPassword),
                        this.maxServerCount,
                        this.maxTableCount,
                        this.maxTableFieldCount,
                        this.databaseRetentionDays,
                        this.dataCollectionThreads,
                        this.dataSaveThreads,
                        this.dataSaveInterval,
                        this.dataSaveBatchSize,
                        this.logLevel
                );
                System.out.println(configLog);
                DBUtil.logInfo("app", configLog);

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load config, using defaults");
                setDefaultConfig();
            }
        } else {
            System.out.println("System config file not found, using default config");
            setDefaultConfig();
        }
    }

    private void setDefaultConfig() {
        this.adminPassword = "admin";
        this.maxServerCount = 10;
        this.maxTableCount = 20;
        this.maxTableFieldCount = 20;
        this.databaseRetentionDays = 15;
        this.dataCollectionThreads = 20;
        this.dataSaveThreads = 1;
        this.dataSaveInterval = 20;
        this.dataSaveBatchSize = 5000;
        this.logLevel = 0;
    }

    /**
     * 获取系统配置对象
     *
     * @return 系统配置对象
     */
    public SystemConfig getSystemConfig() {
        return new SystemConfig(
                this.adminPassword,
                this.maxServerCount,
                this.maxTableCount,
                this.maxTableFieldCount,
                this.databaseRetentionDays,
                this.dataCollectionThreads,
                this.dataSaveThreads,
                this.dataSaveInterval,
                this.dataSaveBatchSize,
                this.logLevel
        );
    }

    /**
     * 密码脱敏显示
     *
     * @param password 原始密码
     * @return 脱敏后的密码
     */
    private static String maskPassword(String password) {
        if (password == null || password.length() <= 2) {
            return "***";
        }
        return password.substring(0, 2) + "***";
    }
}
