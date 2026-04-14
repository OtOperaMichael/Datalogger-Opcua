package com.lego.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lego.pojo.Template;
import com.lego.service.TemplateService;
import com.lego.util.DBUtil;
import com.lego.util.FIlepathUtil;
import com.lego.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ClassName: TemplateImpl
 * Package: lego.service.Impl
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:57
 * @Version 1.0
 */
public class TemplateServiceImpl implements TemplateService {

    // 双重检查锁单例
    private static volatile TemplateServiceImpl instance;

    // 内存中的模板列表（线程安全）
    private final List<Template> templateList = new CopyOnWriteArrayList<>();

    // JSON 文件路径
    private final String JSON_FILE_PATH;

    // 读写锁（比 synchronized 更高效）
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // 私有构造函数
    private TemplateServiceImpl() {
        JSON_FILE_PATH = FIlepathUtil.getTemplateListPath();
        loadFromFile();
    }

    // 获取单例实例
    public static TemplateServiceImpl getInstance() {
        if (instance == null) {
            synchronized (TemplateServiceImpl.class) {
                if (instance == null) {
                    instance = new TemplateServiceImpl();
                }
            }
        }
        return instance;
    }

    // 从文件加载
    private void loadFromFile() {
        File file = new File(JSON_FILE_PATH);
        if (!file.exists()) {
            System.out.println("templateList.json not found. Starting with empty template list.");
            DBUtil.writeLogToDB("app", "templateList.json not found. Starting with empty template list.");
            return;
        }

        try {
            ObjectMapper mapper = JsonUtil.getObjectMapper();
            List<Template> loaded = mapper.readValue(file, new TypeReference<List<Template>>() {
            });
            templateList.clear();
            templateList.addAll(loaded);
            System.out.println("Loaded " + templateList.size() + " templates from " + JSON_FILE_PATH);
            DBUtil.writeLogToDB("app", "Loaded " + templateList.size() + " templates from " + JSON_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Failed to load templateList.json: " + e.getMessage());
            DBUtil.writeLogToDB("app", "Failed to load templateList.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 保存到文件
    private void saveToFile() throws IOException {
        File file = new File(JSON_FILE_PATH);
        file.getParentFile().mkdirs(); // 自动创建目录

        ObjectMapper mapper = JsonUtil.getObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, templateList);
        System.out.println("Saved " + templateList.size() + " templates to " + JSON_FILE_PATH);
        DBUtil.writeLogToDB("app", "Template saved: " + templateList.size() + " templates to " + JSON_FILE_PATH);
    }

    // ----------------- Service Methods -----------------

    @Override
    public List<Template> getAllTemplates() {
        lock.readLock().lock();
        try {
            // 返回副本，防止外部修改内部状态
            return new ArrayList<>(templateList);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Template> saveTemplate(Template newTemplate) {
        if (newTemplate == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        lock.writeLock().lock();
        try {
            // 查找是否已存在（按 id 更新，否则新增）
            boolean found = false;
            for (int i = 0; i < templateList.size(); i++) {
                Template t = templateList.get(i);
                if (t.getId().equalsIgnoreCase(newTemplate.getId())) {
                    templateList.set(i, newTemplate);
                    System.out.println("Updated template: " + newTemplate.getId());
                    DBUtil.writeLogToDB("app", "Template updated: " + newTemplate.getId());
                    found = true;
                    break;
                }
            }
            if (!found) {
                newTemplate.setId(templateList.size() + 1 + "_" + newTemplate.getName());
                templateList.add(newTemplate);
                System.out.println("Added new template: " + newTemplate.getId());
                DBUtil.writeLogToDB("app", "New template added: " + newTemplate.getId());
            }

            saveToFile();
            System.out.println("Saved templateList.json");
            DBUtil.writeLogToDB("app", "Template saved: templateList.json");
            return new ArrayList<>(templateList);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save template" + e.getMessage());
            DBUtil.writeLogToDB("app", "Failed to save template: " + e.getMessage());
            return new ArrayList<>(templateList);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public List<Template> deleteTemplate(String id) {

        lock.writeLock().lock();
        try {
            templateList.removeIf(t -> (t.getId().equalsIgnoreCase(id)));
            System.out.println("Deleted template: " + id);
            DBUtil.writeLogToDB("app", "Template deleted: " + id);
            saveToFile();
            return new ArrayList<>(templateList);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to delete template" + e.getMessage());
            DBUtil.writeLogToDB("app", "Failed to delete template: " + e.getMessage());
            return new ArrayList<>(templateList);
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
    public int isTemplateNameExist(String name) {
        if (name == null) {
            return 1;
        }

        boolean exists = templateList.stream()
                .anyMatch(template -> name.equalsIgnoreCase(template.getName()));

        return exists ? 1 : 0;
    }
}