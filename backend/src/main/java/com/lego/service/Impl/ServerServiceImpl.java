package com.lego.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lego.pojo.Server;
import com.lego.serverTask.ServerTaskManager;
import com.lego.service.ServerService;
import com.lego.util.FIlepathUtil;
import com.lego.util.JsonUtil;
import com.lego.util.DBUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ClassName: ServerImpl
 * Package: lego.service.Impl
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:57
 * @Version 1.0
 */
public class ServerServiceImpl implements ServerService {

    private static volatile ServerServiceImpl instance;
    private final List<Server> serverList = new CopyOnWriteArrayList<>();
    private final String JSON_FILE_PATH;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private ServerServiceImpl() {
        JSON_FILE_PATH = FIlepathUtil.getServerListPath();
        loadFromFile();
    }

    public static ServerServiceImpl getInstance() {
        if (instance == null) {
            synchronized (ServerServiceImpl.class) {
                if (instance == null) {
                    instance = new ServerServiceImpl();
                }
            }
        }
        return instance;
    }

    private void loadFromFile() {
        File file = new File(JSON_FILE_PATH);
        if (!file.exists()) {
            System.out.println("serverList.json not found, starting with empty list.");
            DBUtil.writeLogToDB("app", "serverList.json not found, starting with empty list.");

            return;
        }
        try {
            ObjectMapper mapper = JsonUtil.getObjectMapper();
            List<Server> loaded = mapper.readValue(file, new TypeReference<List<Server>>() {
            });
            serverList.clear();
            serverList.addAll(loaded);
            System.out.println("Loaded " + serverList.size() + " servers from " + JSON_FILE_PATH);
            DBUtil.writeLogToDB("app", "Loaded " + serverList.size() + " servers from " + JSON_FILE_PATH);

        } catch (IOException e) {
            System.err.println("Failed to load serverList.json: " + e.getMessage());
            DBUtil.writeLogToDB("app", "Failed to load serverList.json: " + e.getMessage());
        }
    }

    private void saveToFile() throws IOException {
        File file = new File(JSON_FILE_PATH);
        file.getParentFile().mkdirs();
        ObjectMapper mapper = JsonUtil.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, serverList);
    }

    @Override
    public List<Server> getAllServers() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(serverList);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Server> saveServer(Server newServer) {
        lock.writeLock().lock();
        try {
            // 按 id 判断是新增还是更新
            boolean found = false;
            for (int i = 0; i < serverList.size(); i++) {
                Server s = serverList.get(i);
                if (s.getId().equalsIgnoreCase(newServer.getId())) {
                    serverList.set(i, newServer);
                    System.out.println("Updated server: " + newServer.getName());
                    DBUtil.writeLogToDB("app", "Updated server: " + newServer.getName());
                    found = true;
                    break;
                }
            }
            if (!found) {
                newServer.setId(serverList.size() + 1 + "_" + newServer.getName());
                newServer.setRunning(false);
                newServer.setAutoStart(true);
                serverList.add(newServer);
                System.out.println("Added new server: " + newServer.getName());
                DBUtil.writeLogToDB("app", "Added new server: " + newServer.getName());
            }
            saveToFile();
            System.out.println("Saved serverList.json");
            DBUtil.writeLogToDB("app", "Saved serverList.json");
            return new ArrayList<>(serverList);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save server"+ newServer.getName());
            DBUtil.writeLogToDB("app", "Failed to save server"+ newServer.getName());
            return new ArrayList<>(serverList);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Server> deleteServer(String id) {
        lock.writeLock().lock();
        try {
            //1. 查找server
            Server server = serverList.stream()
                    .filter(s -> s.getId().equalsIgnoreCase(id))
                    .findFirst()
                    .orElse(null);
            if (server == null) {
                return serverList;
            }

            // 2. 先停止任务
            ServerTaskManager.getInstance().stopMonitoring(server);

            // 3. 从列表移除
            serverList.removeIf(s -> s.getId().equalsIgnoreCase(id));
            System.out.println("Deleted server: " + server.getName());
            DBUtil.writeLogToDB("app", "Deleted server: " + server.getName());

            // 4. 持久化（此时 status 已无关）
            saveToFile();
            System.out.println("Saved serverList.json");
            DBUtil.writeLogToDB("app", "Saved serverList.json");

            // 5. 删除数据库 如果是mysql, 因为以后重名可以能写入冲突
            DBUtil.deleteSchema(server.getName());

            return new ArrayList<>(serverList);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to delete server"+ id);
            DBUtil.writeLogToDB("app", "Failed to delete server"+ id);
            return new ArrayList<>(serverList);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to delete server"+ id);
            DBUtil.writeLogToDB("app", "Failed to delete server"+ id);
            return new ArrayList<>(serverList);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 启动
    @Override
    public List<Server> startServer(String id) {
        lock.writeLock().lock();
        try {
            for (Server s : serverList) {
                if (s.getId().equalsIgnoreCase(id)) {
                    // 启动监控任务
                    ServerTaskManager.getInstance().startMonitoring(s);
                    // 更新状态
                    s.setRunning(true);
                    // 注意：不保存 status 到文件！只更新内存
                    break;
                }
            }
            return serverList;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 停止
    @Override
    public List<Server> stopServer(String id) {
        lock.writeLock().lock();
        try {
            for (Server s : serverList) {
                if (s.getId().equalsIgnoreCase(id)) {
                    ServerTaskManager.getInstance().stopMonitoring(s);
                    s.setRunning(false);
                    // 注意：不保存 status 到文件！只更新内存
                    break;
                }
            }
            return serverList;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 判断服务名称是否存在, 存在返回1，不存在返回0
     *
     * @param name
     * @return
     */
    @Override
    public int isServerNameExist(String name) {
        if (name == null) {
            return 1;
        }

        boolean exists = serverList.stream()
                .anyMatch(server -> name.equalsIgnoreCase(server.getName()));

        return exists ? 1 : 0;
    }
}
