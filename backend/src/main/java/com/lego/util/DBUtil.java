package com.lego.util;


import com.alibaba.druid.pool.DruidDataSource;
import com.lego.pojo.DBConfig;
import com.lego.serverTask.DataWriteTask;
import com.lego.serverTask.protocols.libplctag.CIPTag;
import com.lego.serverTask.protocols.libplctag.CIPTagGroup;
import com.lego.serverTask.protocols.opcua.OpcUaNode;
import com.lego.serverTask.protocols.opcua.OpcUaNodeGroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ClassName: MySqlUtil
 * Package: util
 * Description:
 * Author Michael Zhu
 * Create 2025/9/8 15:09
 * Version 1.0
 */

public class DBUtil {

    // JDBC URL:不指定数据库，用于创建数据库
    private static volatile DruidDataSource dataSource = null;
    private static volatile boolean configLoaded = false;

    private static final String logTableName = "log_message";

    /**
     * 初始化连接池（只调用一次）
     */
    public static synchronized boolean init() {
        // 避免重复初始化
        if (configLoaded) {
            return configLoaded;
        }

        try {
            Properties props = new Properties();
            File file = new File(FIlepathUtil.getDatabaseConfigPath());
            if (!file.exists()) {
                configLoaded = false;
                return configLoaded;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                //jdbc:postgresql://localhost:5432/datalogger
                //"postgres"
                //"123456"
                String url = props.getProperty("url");
                String user = props.getProperty("username");
                String password = props.getProperty("password");

                //创建 Druid 连接池
                DruidDataSource ds = new DruidDataSource();
                ds.setUrl(url);
                ds.setUsername(user);
                ds.setPassword(password);

                // ⚙️ 推荐配置（根据你的场景调整）
                ds.setInitialSize(2);          // 初始连接数
                ds.setMinIdle(2);              // 最小空闲连接
                ds.setMaxActive(10);           // 最大活跃连接（新版用 maxPoolPreparedStatementPerConnectionSize）
                ds.setMaxWait(5000);           // 获取连接最大等待时间（ms）
                ds.setValidationQuery("SELECT 1"); // 连接有效性检查
                ds.setTestWhileIdle(true);     // 空闲时检查连接
                ds.setTestOnBorrow(false);     // 借出时不检查（影响性能）
                ds.setTestOnReturn(false);     // 归还时不检查

                // 可选：开启监控（需 web 集成）
                // ds.setFilters("stat,wall");

                ds.init(); // 初始化连接池

                dataSource = ds;
                configLoaded = true;

                // 创建数据库app, 用于存储app日志
                createSchemaIfNotExists("app");

                writeLogToDB("app", "TimescaleDB connection pool initialized");
                System.out.println("TimescaleDB connection pool initialized");

                return configLoaded;
            }
        } catch (Exception e) {
            configLoaded = false;

            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (Exception ex) {
                }
                dataSource = null;
            }
        }
        return false;
    }

    /**
     * 检查是否configReady
     *
     * @return
     */
    public static boolean isConfigReady() {
        return configLoaded;
    }

    /**
     * 创建数据库
     */
    public static void createSchemaIfNotExists(String schemaName) throws SQLException {
        if (!isValidSchemaName(schemaName)) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            stmt.execute(sql);
            createLogTable(schemaName);
            System.out.println("Schema created: " + schemaName);
            writeLogToDB(schemaName, "schema created for server: " + schemaName);

        }
    }

    public static void deleteSchema(String schemaName) throws SQLException {
        if (!isValidSchemaName(schemaName)) {
            throw new IllegalArgumentException("Invalid DB name: " + schemaName);
        }

        // 先检查数据库是否存在
        if (!schemaExists(schemaName)) {
            writeLogToDB("app", "TimescaleDB database does not exist, skip deletion: " + schemaName);
            return;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE";
            stmt.execute(sql);
            writeLogToDB("app", "TimescaleDB database deleted for server: " + schemaName);

        }
    }

    /**
     * 检查数据库是否存在
     *
     * @param schemaName 数据库名称
     * @return true 如果数据库存在
     */
    private static boolean schemaExists(String schemaName) throws SQLException {
        String sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, schemaName.toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // 如果有结果，说明数据库存在
            }
        }
    }

    private static boolean isValidSchemaName(String name) {
        return name != null && name.matches("[a-zA-Z_][a-zA-Z0-9_]{0,63}");
    }

    /**
     * 创建表
     */
    public static void createTable(String schemaName, OpcUaNodeGroup nodeGroup) {
        String safeDbName = sanitizeIdentifier(schemaName);
        String safeTableName = sanitizeIdentifier(nodeGroup.getName());
        String fullTableName = safeDbName + "." + safeTableName;

        StringBuilder columns = new StringBuilder();
        for (OpcUaNode node : nodeGroup.getNodeList()) {
            String safeColName = sanitizeIdentifier(node.getName());
            String sqlType = mapDataTypeToSql(node.getDataType());
            columns.append(safeColName).append(" ").append(sqlType).append(" NOT NULL, ");
        }
        if (columns.length() > 0) {
            columns.setLength(columns.length() - 2);
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + fullTableName +
                "(id SERIAL PRIMARY KEY, " +
                "timestamp TIMESTAMPTZ DEFAULT NOW(), " +
                columns + ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table " + fullTableName + " created");
            writeLogToDB(schemaName, "Table " + fullTableName + " created");

        } catch (SQLException e) {
            System.err.println("Create table failed: " + e.getMessage());
            writeLogToDB(schemaName, "Create table failed: " + e.getMessage());

        }
    }

    /**
     * 创建 hyper 表，存储时序数据
     */
    public static void createHyperTable(String schemaName, OpcUaNodeGroup nodeGroup) {
        String safeDbName = sanitizeIdentifier(schemaName);
        String safeTableName = sanitizeIdentifier(nodeGroup.getName());
        String fullTableName = safeDbName + "." + safeTableName;

        StringBuilder columns = new StringBuilder();
        for (OpcUaNode node : nodeGroup.getNodeList()) {
            String safeColName = sanitizeIdentifier(node.getName());
            String sqlType = mapDataTypeToSql(node.getDataType());
            columns.append(safeColName).append(" ").append(sqlType).append(" NOT NULL, ");
        }
        if (columns.length() > 0) {
            columns.setLength(columns.length() - 2);
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + fullTableName +
                "(time TIMESTAMPTZ NOT NULL," +
                columns + ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. 检查表在创建前是否存在
            boolean tableExistedBefore = tableExists(conn, schemaName, nodeGroup.getName());

            // 2. 执行创建表的 SQL
            stmt.execute(sql);

            // 3. 检查表是否真的被创建了
            boolean tableJustCreated = !tableExistedBefore && tableExists(conn, schemaName, nodeGroup.getName());

            // 4. 只有真正创建了新表才记录日志
            if (tableJustCreated) {
                System.out.println(schemaName + " : " + fullTableName + " created");
                writeLogToDB(schemaName, fullTableName + " created");
            }

            // 如果是新表，尝试创建 hypertable（需 superuser 权限）
            try {
                String hypertableSql = "SELECT create_hypertable('" + fullTableName + "', 'time', if_not_exists => true)";

                // 检查 hypertable 在创建前是否存在
                boolean hypertableExistedBefore = hypertableExists(conn, fullTableName);

                stmt.execute(hypertableSql);

                // 检查 hypertable 是否刚被创建
                boolean hypertableJustCreated = !hypertableExistedBefore && hypertableExists(conn, fullTableName);

                if (hypertableJustCreated) {
                    System.out.println(schemaName + " : " + fullTableName + " Hypertable created");
                    writeLogToDB(schemaName, fullTableName + " Hypertable created");
                }

                // 设置数据保留策略为 30 天
                try {
                    String retentionSql = "SELECT add_retention_policy('" + fullTableName + "', INTERVAL '30 days', if_not_exists => true)";
                    stmt.execute(retentionSql);

                    // 只有新表才记录保留策略日志
                    if (tableJustCreated) {
                        System.out.println(schemaName + "." + safeTableName + " retention policy set to 30 days");
                        writeLogToDB(schemaName, safeTableName + " retention policy set to 30 days");
                    }
                } catch (SQLException e) {
                    // 可能已存在保留策略或权限不足
                    if (tableJustCreated) {
                        System.out.println(schemaName + " Failed to set retention policy: " + e.getMessage());
                    }
                }
            } catch (SQLException ignored) {
                // 可能已存在 hypertable
            }

        } catch (SQLException e) {
            System.out.println(schemaName + " Create table failed: " + e.getMessage());
            writeLogToDB(schemaName, "Create table failed: " + e.getMessage());
        }
    }

    private static String mapDataTypeToSql(com.lego.pojo.template.custom.DataType dataType) {
        if (dataType == null) {
            return "DOUBLE PRECISION";
        }

        switch (dataType) {
            case BOOL:
                return "BOOLEAN";
            case INT:
                return "INTEGER";
            case DOUBLE:
                return "DOUBLE PRECISION";
            case STRING:
                return "TEXT";
            default:
                return "DOUBLE PRECISION";
        }
    }

    /**
     * 创建日志表
     *
     * @param schemaName
     */
    public static void createLogTable(String schemaName) {
        String safeDbName = sanitizeIdentifier(schemaName);
        String fullTableName = safeDbName + "." + logTableName;
        String sql = "CREATE TABLE IF NOT EXISTS " + fullTableName + " (" +
                "time TIMESTAMPTZ NOT NULL, " +
                "message TEXT" +
                ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查表在创建前是否存在
            boolean tableExistedBefore = tableExists(conn, schemaName, logTableName);

            stmt.execute(sql);

            // 检查表是否真的被创建了
            boolean tableJustCreated = !tableExistedBefore && tableExists(conn, schemaName, logTableName);

            // 尝试将 log 表转换为 hypertable（需 superuser 权限）
            try {
                String hypertableSql = "SELECT create_hypertable('" + fullTableName + "', 'time', if_not_exists => true)";

                // 检查 hypertable 在创建前是否存在
                boolean hypertableExistedBefore = hypertableExists(conn, fullTableName);

                stmt.execute(hypertableSql);

                // 检查 hypertable 是否刚被创建
                boolean hypertableJustCreated = !hypertableExistedBefore && hypertableExists(conn, fullTableName);

                if (hypertableJustCreated) {
                    System.out.println(schemaName + " Log hypertable created");
                    writeLogToDB(schemaName, "Log hypertable created");
                }

                // 设置数据保留策略为 30 天
                try {
                    String retentionSql = "SELECT add_retention_policy('" + fullTableName + "', INTERVAL '30 days', if_not_exists => true)";
                    stmt.execute(retentionSql);

                    // 只有新表才记录保留策略日志
                    if (tableJustCreated) {
                        System.out.println(schemaName + "." + logTableName + " retention policy set to 30 days");
                        writeLogToDB(schemaName, logTableName + " retention policy set to 30 days");
                    }
                } catch (SQLException e) {
                    // 可能已存在保留策略或权限不足
                    if (tableJustCreated) {
                        System.out.println(schemaName + " Failed to set retention policy: " + e.getMessage());
                    }
                }

            } catch (SQLException e) {
                // 可能已存在 hypertable 或权限不足
                if (tableJustCreated) {
                    System.out.println(schemaName + " Log table is already a hypertable or insufficient permissions: " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.out.println(schemaName + " Create log table failed: " + e.getMessage());
            writeLogToDB(schemaName, "Create log table failed: " + e.getMessage());
        }
    }





    /**
     * 插入数据
     */
    public static void insertData(String schemaName, CIPTagGroup tagGroup) {
        String safeDbName = sanitizeIdentifier(schemaName);
        String safeTableName = sanitizeIdentifier(tagGroup.getName());
        String fullTableName = safeDbName + "." + safeTableName;
        List<CIPTag> tagList = tagGroup.getCipTagArrayList();

        List<String> columns = tagGroup.getTagNames().stream()
                .map(DBUtil::sanitizeIdentifier)
                .collect(Collectors.toList());

        String colStr = String.join(", ", columns);
        String placeholderStr = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + fullTableName + " (" + colStr + ") VALUES (" + placeholderStr + ")";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < tagList.size(); i++) {
                pstmt.setObject(i + 1, tagList.get(i).getValAsDouble());
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            writeLogToDB(schemaName, "Insert failed into " + fullTableName + ": " + e.getMessage());

        }
    }

    /**
     * 批量插入数据到指定表
     *
     * @param schemaName schema 名称
     * @param tableName  表名
     * @param taskList   任务列表
     */
    public static void insertBatchToTable(String schemaName, String tableName, List<DataWriteTask> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            return;
        }

        String safeDbName = sanitizeIdentifier(schemaName);
        String safeTableName = sanitizeIdentifier(tableName);
        String fullTableName = safeDbName + "." + safeTableName;

        // 获取第一个 task 的列名
        DataWriteTask firstTask = taskList.get(0);
        List<String> columns = firstTask.getData().keySet().stream()
                .map(DBUtil::sanitizeIdentifier)
                .collect(Collectors.toList());

        // 构建 SQL: INSERT INTO table (time, col1, col2, ...) VALUES (?, ?, ...), (?, ?, ...), ...
        String colStr = "time, " + String.join(", ", columns);
        String valuePlaceholder = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String rowPlaceholder = "(?, " + valuePlaceholder + ")";

        StringBuilder valuesClause = new StringBuilder();
        for (int i = 0; i < taskList.size(); i++) {
            if (i > 0) {
                valuesClause.append(", ");
            }
            valuesClause.append(rowPlaceholder);
        }

        String sql = "INSERT INTO " + fullTableName + " (" + colStr + ") VALUES " + valuesClause;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            for (DataWriteTask task : taskList) {
                // 设置 time 参数
                pstmt.setTimestamp(paramIndex++, Timestamp.from(task.getTimestamp()));

                // 设置数据列参数
                for (Double value : task.getData().values()) {
                    pstmt.setObject(paramIndex++, value);
                }
            }

            pstmt.executeUpdate();

        } catch (SQLException e) {
            writeLogToDB(schemaName, "Batch insert failed into " + fullTableName + ": " + e.getMessage());
        }
    }

    /**
     * 写入日志数据到 TimescaleDB
     */
    public static void writeLogToDB(String schemaName, String log) {
        // 如果数据库未初始化，只打印控制台日志，不抛异常
        if (!configLoaded || dataSource == null) {
            System.out.println("[LOG][" + schemaName + "] " + log);
            return;
        }

        String safeDbName = sanitizeIdentifier(schemaName);
        String fullTableName = safeDbName + "." + logTableName;

        String sql = "INSERT INTO " + fullTableName + " (time, message) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, log);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Insert log failed into " + fullTableName + ": " + e.getMessage() + " log content" + log);
        }
    }


    //查询数据
    public static List<Map<String, Object>> queryData(String schemaName, String tableName, String startTime, String endTime) {
        List<Map<String, Object>> results = new ArrayList<>();

        // 先查询表结构获取列名
        List<String> columnNames = getTableColumnNames(schemaName, tableName);

        // 构建 SQL 语句，添加显式类型转换
        String columns = String.join(", ", columnNames);
        String sql = "SELECT " + columns + " FROM " + sanitizeIdentifier(schemaName) + "." + sanitizeIdentifier(tableName) + " " +
                "WHERE time >= ?::timestamptz AND time <= ?::timestamptz " +
                "ORDER BY time DESC " +
                "LIMIT 100000";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startTime);
            pstmt.setString(2, endTime);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String columnName : columnNames) {
                        Object value = rs.getObject(columnName);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("TimescaleDB query error: " + e.getMessage());
            writeLogToDB(schemaName, "TimescaleDB query error: " + e.getMessage());

        }

        return results;
    }

    // 辅助方法：获取表的所有列名
    private static List<String> getTableColumnNames(String schemaName, String tableName) {
        List<String> columns = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT column_name FROM information_schema.columns " +
                             "WHERE table_schema = ? AND table_name = ? " +
                             "ORDER BY ordinal_position")) {

            pstmt.setString(1, schemaName.toLowerCase());
            pstmt.setString(2, tableName.toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString("column_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columns;
    }


    /**
     * 从连接池获取连接（内部使用）
     */
    private static Connection getConnection() throws SQLException {
        // 如果数据库未初始化，只打印控制台日志，不抛异常
        if (!configLoaded || dataSource == null) {
            System.out.println("Trying to get connection, but database not initialized");
            return null;
        }
        return dataSource.getConnection(); // 从池中借出连接
    }

    private static String sanitizeIdentifier(String name) {
        if (name == null || !name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid identifier: " + name);
        }
        // PostgreSQL 使用双引号包裹标识符
        return "\"" + name + "\"";
    }

    /**
     * 关闭连接池（应用关闭时调用）
     */
    public static void close() {
        if (dataSource != null) {
            try {
                dataSource.close(); // Druid 会关闭连接池，并反注册其代理驱动
                System.out.println("TimescaleDB datasource shut down");
            } catch (Exception e) {
                System.err.println("Error closing Druid DataSource: " + e.getMessage());
                writeLogToDB("app", "Error closing Druid DataSource: " + e.getMessage());

            }
            dataSource = null;
            configLoaded = false;
        }

        // 【关键】反注册由本应用加载的 PostgreSQL 原生 JDBC 驱动
        deregisterPostgresDriver();
    }

    /**
     * 卸载已注册的 JDBC 驱动程序（用于解决 Tomcat 启动时重复注册 JDBC 驱动程序的问题）
     */
    private static void deregisterPostgresDriver() {
        ClassLoader currentClassLoader = DBUtil.class.getClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();

        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            // 只反注册由当前 WebApp ClassLoader 加载的驱动（通常是 PostgreSQL、Druid 等）
            if (driver.getClass().getClassLoader() == currentClassLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                    System.out.println("Deregistering JDBC driver: " + driver.getClass().getName());
                    writeLogToDB("app", "Deregistered JDBC driver: " + driver.getClass().getName());

                    System.out.println("Deregistered JDBC driver completed");
                } catch (SQLException e) {
                    System.err.println("Failed to deregister driver: " + e.getMessage());
                    writeLogToDB("app", "Failed to deregister driver: " + e.getMessage());

                }
            }
        }
    }

    /**
     * 使用传入的配置参数测试 TimescaleDB 连接（不依赖已初始化的 dataSource）
     *
     * @param config 前端传入的 TimescaleDB 配置
     * @return true 表示连接成功
     */
    public static boolean testConnection(DBConfig config) {
        if (config == null ||
                config.getUrl() == null || config.getUrl().isEmpty() ||
                config.getUsername() == null || config.getUsername().isEmpty() ||
                config.getPassword() == null ||
                config.getDriverClassName() == null || config.getDriverClassName().isEmpty()) {
            return false;
        }

        Connection conn = null;
        try {
            // 1. 加载驱动（可选，JDBC 4+ 通常自动加载，但显式调用更可靠）
            Class.forName(config.getDriverClassName());

            // 2. 建立临时连接
            conn = DriverManager.getConnection(
                    config.getUrl(),
                    config.getUsername(),
                    config.getPassword()
            );

            // 3. 执行轻量查询验证
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                return rs.next(); // 能读到结果即视为连通
            }
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL driver not found: " + e.getMessage());
            writeLogToDB("app", "PostgreSQL driver not found: " + e.getMessage());

            return false;
        } catch (SQLException e) {
            System.err.println("PostgreSQL test connection failed: " + e.getMessage());
            writeLogToDB("app", "PostgreSQL test connection failed: " + e.getMessage());

            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // 关闭临时连接
                } catch (SQLException ignored) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * re-initialize system, by deleting config file
     *
     * @param
     * @return
     */
    public static boolean deleteConfigFile() {
        String path = FIlepathUtil.getDatabaseConfigPath();
        File file = new File(path);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("Deleted TimescaleDB config file: " + path);
                writeLogToDB("app", "Deleted TimescaleDB config file: " + path);

            } else {
                System.err.println("Failed to delete TimescaleDB config file: " + path);
                writeLogToDB("app", "Failed to delete TimescaleDB config file: " + path);

            }
            configLoaded = false;
            return deleted;
        }
        // 文件不存在也算"已重置"
        configLoaded = false;
        return true;
    }

    public static boolean saveConfigFile(DBConfig config) {
        //写入文件
        Properties props = new Properties();
        props.setProperty("username", config.getUsername());
        props.setProperty("password", config.getPassword());
        props.setProperty("driverClassName", config.getDriverClassName());
        props.setProperty("url", config.getUrl());

        File configFile = new File(FIlepathUtil.getDatabaseConfigPath());
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Database Configuration - Saved via Web UI");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * query logs from database
     *
     * @param schemaName schema 名称
     * @param days       查询最近 N 天的日志
     * @return 日志消息列表
     */
    public static List<String> queryLogs(String schemaName, int days) {
        List<String> logMessages = new ArrayList<>();

        if (schemaName == null || days <= 0) {
            return logMessages;
        }

        String safeDbName = sanitizeIdentifier(schemaName);
        String fullTableName = safeDbName + "." + logTableName;

        // 构建 SQL 查询：查询最近 N 天的日志，按时间倒序排列
        String sql = "SELECT time, message FROM " + fullTableName +
                " WHERE time >= NOW() - INTERVAL '" + days + " DAYS' " +
                " ORDER BY time DESC " +
                " LIMIT 10000";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("time");
                String message = rs.getString("message");

                // 格式化输出：[时间] 消息内容
                String formattedLog = String.format("%s:  %s",
                        timestamp != null ? timestamp.toString() : "N/A",
                        message != null ? message : "");

                logMessages.add(formattedLog);
            }
        } catch (SQLException e) {
            writeLogToDB("app", "Query logs failed for schema " + schemaName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return logMessages;
    }

    /**
     * 检查表是否已存在
     */
    private static boolean tableExists(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = "SELECT EXISTS (" +
                "SELECT FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_name = ?" +
                ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schemaName.toLowerCase());
            pstmt.setString(2, tableName.toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    /**
     * 检查 hypertable 是否已存在
     */
    private static boolean hypertableExists(Connection conn, String fullTableName) throws SQLException {
        String sql = "SELECT EXISTS (" +
                "SELECT FROM timescaledb_information.hypertables " +
                "WHERE hypertable_name = ?" +
                ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullTableName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }
}
