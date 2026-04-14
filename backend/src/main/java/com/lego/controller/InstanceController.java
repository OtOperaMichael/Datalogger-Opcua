package com.lego.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.lego.common.Result;
import com.lego.common.ResultCodeEnum;
import com.lego.pojo.QueryLoad;
import com.lego.pojo.Server;
import com.lego.pojo.Table;
import com.lego.pojo.Template;
import com.lego.service.Impl.InstanceServiceImpl;
import com.lego.service.Impl.ServerServiceImpl;
import com.lego.service.InstanceService;
import com.lego.service.ServerService;
import com.lego.util.TemplateUtil;
import com.lego.util.WebUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: InstanceController
 * Package: lego.controller
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:58
 * @Version 1.0
 */
@WebServlet("/instance/*")
public class InstanceController extends BaseController {
    private final InstanceService instanceService = new InstanceServiceImpl();
    private final ServerService serverService = ServerServiceImpl.getInstance();
    protected void query(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取参数
        QueryLoad queryLoad = WebUtil.readJson(req, QueryLoad.class);
        // 参数校验
        if (queryLoad.getServerName() == null || queryLoad.getServerName().trim().isEmpty()) {
            WebUtil.writeJson(resp, Result.build(null, ResultCodeEnum.FAILURE));
            return;
        }
        if (queryLoad.getTableName() == null || queryLoad.getTableName().trim().isEmpty()) {
            WebUtil.writeJson(resp, Result.build(null, ResultCodeEnum.FAILURE));
            return;
        }
        if (queryLoad.getStartTime() == null || queryLoad.getEndTime() == null) {
            WebUtil.writeJson(resp, Result.build(null, ResultCodeEnum.FAILURE));
            return;
        }

        // 解析时间
        // 确保使用系统默认时区（中国时区）
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(queryLoad.getStartTime(), formatter);
        LocalDateTime endTime = LocalDateTime.parse(queryLoad.getEndTime(), formatter);
        queryLoad.setStartTime(String.valueOf(startTime));
        queryLoad.setEndTime(String.valueOf(endTime));

        // 查找服务器
        Server server = serverService.getAllServers().stream()
                .filter(s -> queryLoad.getServerName().equals(s.getName()))
                .findFirst()
                .orElse(null);

        if (server == null) {
            WebUtil.writeJson(resp, Result.build(null, ResultCodeEnum.FAILURE));
            return;
        }

        // 获取模板
        Template template = TemplateUtil.resolveTemplate(server);
        if (template == null) {
            WebUtil.writeJson(resp, Result.build(null, ResultCodeEnum.FAILURE));
            return;
        }

        // 查找表信息
        Table table = template.getTableList().stream()
                .filter(t -> queryLoad.getTableName().equals(t.getName()))
                .findFirst()
                .orElse(null);

        if (table == null) {
            WebUtil.writeJson(resp, Result.build(null, ResultCodeEnum.FAILURE));
            return;
        }
        //调用 service查询
        List<Map<String, Object>> results =  instanceService.query(queryLoad);
        //返回结果
        // 构造响应
        Map<String, Object> data = new HashMap<>();
        data.put("results", results);
        data.put("count", results.size());

        WebUtil.writeJson(resp, Result.ok(data));

    }

    protected void queryLogs(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取参数
        String lineId = req.getParameter("lineId");
        Result result = null;
        // 调用service queryLogs
        List<String> logList = instanceService.queryLogs(lineId, 7);

        //成功 便返回 删除后的serverList，并相应给客户端
        Map data = new HashMap();
        data.put("logList", logList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }

}
