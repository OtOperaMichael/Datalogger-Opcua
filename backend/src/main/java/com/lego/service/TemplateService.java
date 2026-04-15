package com.lego.service;

import com.lego.pojo.template.Template;

import java.util.List;

/**
 * ClassName: TemplateService
 * Package: lego.service
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:48
 * @Version 1.0
 */
public interface TemplateService {

    public List<Template> saveTemplate(Template newTemplate);
    public List<Template> deleteTemplate(String id);
    public List<Template> getAllTemplates();
    int isTemplateNameExist(String name);
}
