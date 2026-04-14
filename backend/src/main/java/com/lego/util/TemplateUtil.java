package com.lego.util;

import com.lego.pojo.Server;
import com.lego.pojo.Template;
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
     * @param server
     * @return
     */
    public static Template resolveTemplate(Server server) {
        String templateName = server.getTemplateName();
        if (templateName == null) {
            return null;
        }

        return TemplateServiceImpl.getInstance()
                .getAllTemplates()
                .stream()
                .filter(t -> templateName.equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));
    }
}
