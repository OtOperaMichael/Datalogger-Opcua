package com.lego.service;

import com.lego.pojo.DBConfig;
import com.lego.pojo.SystemConfig;

import java.io.IOException;

/**
 * ClassName: TemplateService
 * Package: lego.service
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:48
 * @Version 1.0
 */
public interface CommonService {
    boolean isDatabaseConfigReady();
    boolean saveDatabaseConfig(DBConfig config) throws IOException;
    void initDatabase();
    boolean reInitializeConfig();
    boolean testDatabase(DBConfig config);
    boolean saveSystemConfig(SystemConfig config) throws IOException;
    boolean isSystemConfigReady();

    void initSystemConfig();

    SystemConfig getSystemConfig();
}
