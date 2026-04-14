package com.lego.controller;

import com.lego.common.Result;
import com.lego.service.DiagnoseService;
import com.lego.service.Impl.DiagnoseServiceImpl;
import com.lego.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * ClassName: DiagnoseController
 * Package: com.lego.controller
 * Description:
 *
 * @Author michael.zhu
 * @Create 4/2/2026 11:49 AM
 * @Version 1.0
 */
@WebServlet("/diagnose/*")
public class DiagnoseController extends BaseController{

    private final DiagnoseService diagnoseService = new DiagnoseServiceImpl();

    /**
     * 获取线程状态
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void getThreadStatus(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //调用 service获取配置状态
        StringBuilder threadStatus = diagnoseService.getThreadStatus();
        //返回结果
        Result result = Result.ok(threadStatus);

        WebUtil.writeJson(resp, result);

    }
}




