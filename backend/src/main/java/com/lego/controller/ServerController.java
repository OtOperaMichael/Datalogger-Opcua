package com.lego.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.lego.common.Result;
import com.lego.common.ResultCodeEnum;
import com.lego.pojo.Server;
import com.lego.service.Impl.InstanceServiceImpl;
import com.lego.service.Impl.ServerServiceImpl;
import com.lego.service.InstanceService;
import com.lego.service.ServerService;
import com.lego.util.WebUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: ServerController
 * Package: lego.controller
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:58
 * @Version 1.0
 */
@WebServlet("/server/*")
public class ServerController extends BaseController {

    private final ServerService serverService = ServerServiceImpl.getInstance();
    private final InstanceService instanceService = new InstanceServiceImpl();

    protected void getAllServers(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 调用service saveTemplate 到内存并保存到文件中
        List<Server> serverList = serverService.getAllServers();
        //成功 便返回 整个templateList，并相应给客户端
        Result result = null;
        Map data = new HashMap();
        data.put("serverList", serverList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }

    protected void saveServer(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取请求参数
        Server server = WebUtil.readJson(req, Server.class);
        Result result = null;

        // 判断名称是否已存在
        if (serverService.isServerNameExist(server.getName()) == 1) {
            result = Result.build(null, ResultCodeEnum.SERVER_NAME_EXIST);
            WebUtil.writeJson(resp, result);
            return;
        }

        //调用service saveServer 到内存并保存到文件中
        List<Server> serverList = serverService.saveServer(server);

        //成功 便返回 整个templateList，并相应给客户端
        Map data = new HashMap();
        data.put("serverList", serverList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }

    protected void deleteServer(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取请求参数
        String id = req.getParameter("id");
        Result result = null;

        //调用service deleteServer 到内存并保存到文件中
        List<Server> serverList = serverService.deleteServer(id);

        //成功 便返回 删除后的serverList，并相应给客户端
        Map data = new HashMap();
        data.put("serverList", serverList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);

    }

    protected void startServer(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取请求参数
        String id = req.getParameter("id");
        Result result = null;

        //调用service deleteServer 到内存并保存到文件中
        List<Server> serverList = serverService.startServer(id);

        //成功 便返回 删除后的serverList，并相应给客户端
        Map data = new HashMap();
        data.put("serverList", serverList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }

    protected void stopServer(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取请求参数
        String id = req.getParameter("id");
        Result result = null;

        //调用service deleteServer 到内存并保存到文件中
        List<Server> serverList = serverService.stopServer(id);

        //成功 便返回 删除后的serverList，并相应给客户端
        Map data = new HashMap();
        data.put("serverList", serverList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }


}
