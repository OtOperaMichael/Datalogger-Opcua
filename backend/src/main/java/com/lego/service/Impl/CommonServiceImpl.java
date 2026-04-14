package com.lego.service.Impl;

import com.lego.pojo.DBConfig;
import com.lego.pojo.SystemConfig;
import com.lego.service.CommonService;
import com.lego.util.FIlepathUtil;
import com.lego.util.DBUtil;
import com.lego.util.SystemConfigUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ClassName: CommonServiceImpl
 * Package: lego.service.Impl
 * Description:
 *
 * @Author michael.zhu
 * @Create 2/25/2026 10:04 AM
 * @Version 1.0
 */
public class CommonServiceImpl implements CommonService {

    private static final String SYSTEM_CONFIG_PATH = "conf/system.properties";

    @Override
    public boolean isDatabaseConfigReady() {
        return DBUtil.isConfigReady();
    }

    @Override
    public boolean saveDatabaseConfig(DBConfig config) throws IOException {
        return DBUtil.saveConfigFile(config);
    }

    @Override
    public void initDatabase() {
        DBUtil.init();
    }

    @Override
    public boolean testDatabase(DBConfig config) {
        return DBUtil.testConnection(config);
    }

    @Override
    public boolean reInitializeConfig() {
        boolean resultOfDatabase = DBUtil.deleteConfigFile();
        boolean resultOfSystemConfig = SystemConfigUtil.deleteSystemConfig();
        return resultOfDatabase && resultOfSystemConfig;
    }

    @Override
    public boolean saveSystemConfig(SystemConfig config) throws IOException {
        return SystemConfigUtil.getInstance().saveSystemConfig(config);
    }


    @Override
    public boolean isSystemConfigReady() {
        return SystemConfigUtil.getInstance().isSystemConfigReady();
    }

    @Override
    public void initSystemConfig() {
        SystemConfigUtil.getInstance();
    }

    @Override
    public SystemConfig getSystemConfig() {
        return SystemConfigUtil.getInstance().getSystemConfig();
    }


}
