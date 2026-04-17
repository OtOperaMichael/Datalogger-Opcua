package com.lego.util;

import com.lego.pojo.Server;
import com.lego.pojo.template.Template;
import com.lego.service.Impl.TemplateServiceImpl;

/**
 * ClassName: TemplateUtil
 * Package: lego.util
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/23 16:53
 * @Version 1.0
 */
public class TemplateUtil {
    /**
     * 解析模板
     * 如果模板不存在，记录日志并返回 null，不会中断应用
     * 
     * @param server 服务器配置
     * @return 模板对象，未找到则返回 null
     */
    public static Template resolveTemplate(Server server) {
        String templateName = server.getTemplateName();
        
        // 检查模板名称是否为空
        if (templateName == null || templateName.isEmpty()) {
            LogUtil.logWarning(true, "app", "Server {} has no template configured", server.getName());
            return null;
        }

        // 查找模板
        Template template = TemplateServiceImpl.getInstance()
                .getAllTemplates()
                .stream()
                .filter(t -> templateName.equals(t.getName()))
                .findFirst()
                .orElse(null);

        // 如果未找到，记录日志
        if (template == null) {
            LogUtil.logWarning(true, "app", "Template not found: {} for server: {}", templateName, server.getName());
        }

        return template;
    }
}
