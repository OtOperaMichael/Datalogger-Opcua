package com.lego.controller;

import com.lego.pojo.SystemConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.lego.common.Result;
import com.lego.common.ResultCodeEnum;
import com.lego.pojo.DBConfig;
import com.lego.service.CommonService;
import com.lego.service.Impl.CommonServiceImpl;
import com.lego.util.WebUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * ClassName: CommonController
 * Package: com.lego.controller
 * Description:
 *
 * @Author michael.zhu
 * @Create 2/25/2026 9:58 AM
 * @Version 1.0
 */
@WebServlet("/common/*")
public class CommonController extends BaseController {

    private final CommonService commonService = new CommonServiceImpl();

    /**
     * 前端启动时获取配置状态，检测数据库和influxdb配置，用于启动时判断是否需要引导用户进行配置
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void getConfigStatus(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //调用 service获取配置状态
        boolean status = commonService.isDatabaseConfigReady() && commonService.isSystemConfigReady();
        //返回结果
        Result result = Result.build(null, status ? ResultCodeEnum.SUCCESS : ResultCodeEnum.CONFIG_NOTEXIST);
        WebUtil.writeJson(resp, result);
    }

    /**
     * 重置配置
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void resetConfig(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //调用 service获取配置状态
        boolean status = commonService.reInitializeConfig();
        //返回结果
        Result result = Result.build(null, status ? ResultCodeEnum.SUCCESS : ResultCodeEnum.FAILURE);
        WebUtil.writeJson(resp, result);

    }

    protected void saveDatabaseConfig(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //获取参数
        DBConfig config = WebUtil.readJson(req, DBConfig.class);
        Result result = null;

        if (config.getUsername() == null || config.getPassword() == null || config.getDriverClassName() == null || config.getUrl() == null) {
            result = Result.build(null, ResultCodeEnum.FAILURE);
        } else {
            boolean isSaved = commonService.saveDatabaseConfig(config);
            if (!isSaved) {
                result = Result.build(null, ResultCodeEnum.FAILURE);
            } else {
                result = Result.ok(null);
            }
        }

        //初始化配置
        commonService.initDatabase();

        //返回结果
        WebUtil.writeJson(resp, result);

    }


    protected void saveSystemConfig(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //获取参数
        SystemConfig config = WebUtil.readJson(req, SystemConfig.class);
        Result result = null;

        if (config.getAdminPassword() == null || config.getMaxServerCount() == null || config.getMaxTableFieldCount() == null || config.getMaxTableCount() == null) {
            result = Result.build(null, ResultCodeEnum.FAILURE);
        } else {
            boolean isSaved = commonService.saveSystemConfig(config);
            if (!isSaved) {
                result = Result.build(null, ResultCodeEnum.FAILURE);
            } else {
                result = Result.ok(null);
            }
        }

        //初始化配置
        commonService.initSystemConfig();

        //返回结果
        WebUtil.writeJson(resp, result);

    }

    protected void getSystemConfig(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Result result = null;
        //调用 service获取配置
        SystemConfig config = commonService.getSystemConfig();

        //成功 便返回 config，并相应给客户端
        Map data = new HashMap();
        data.put("systemConfig", config);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }

    /**
     * 测试连接
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void testDatabaseConnection(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DBConfig config = WebUtil.readJson(req, DBConfig.class);

        boolean isConnOk = commonService.testDatabase(config);
        Result result = isConnOk ? Result.ok(null) : Result.build(null, ResultCodeEnum.FAILURE);
        WebUtil.writeJson(resp, result);
    }


}
