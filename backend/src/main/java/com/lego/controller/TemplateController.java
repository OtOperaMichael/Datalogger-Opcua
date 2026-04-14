package com.lego.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.lego.common.Result;
import com.lego.common.ResultCodeEnum;
import com.lego.pojo.Template;
import com.lego.service.Impl.TemplateServiceImpl;
import com.lego.service.TemplateService;
import com.lego.util.WebUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: TemplateController
 * Package: lego.controller
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:58
 * @Version 1.0
 */

@WebServlet("/template/*")
public class TemplateController extends BaseController {

    private final TemplateService templateService = TemplateServiceImpl.getInstance();

    protected void saveTemplate(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // 获取参数
        Template template = WebUtil.readJson(req, Template.class);
        String ifNew = req.getParameter("ifNew");
        Result result = null;

        // 如果是新建，判断名称是否已存在
        if ("true".equals(ifNew)) {
            if (templateService.isTemplateNameExist(template.getName()) == 1) {
                result = Result.build(null, ResultCodeEnum.TEMPLATE_NAME_EXIST);
                WebUtil.writeJson(resp, result);
                return;
            }
        }

        // 调用service saveTemplate 到内存并保存到文件中
        List<Template> templateList = templateService.saveTemplate(template);
        // 成功 便返回 整个templateList，并相应给客户端
        Map data = new HashMap();
        data.put("templateList", templateList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }

    protected void deleteTemplate(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // 获取参数
        String id = req.getParameter("id");
        Result result = null;

        // 调用service deleteTemplate
        List<Template> templateList = templateService.deleteTemplate(id);
        // 成功 便返回 整个templateList，并相应给客户端
        Map data = new HashMap();
        data.put("templateList", templateList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);

    }

    protected void getAllTemplates(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 调用service saveTemplate 到内存并保存到文件中
        List<Template> templateList = templateService.getAllTemplates();
        // 成功 便返回 整个templateList，并相应给客户端
        Result result = null;
        Map data = new HashMap();
        data.put("templateList", templateList);
        result = Result.ok(data);
        WebUtil.writeJson(resp, result);
    }
}
