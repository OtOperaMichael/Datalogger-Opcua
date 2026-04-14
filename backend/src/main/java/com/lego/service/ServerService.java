package com.lego.service;

import com.lego.pojo.Server;

import java.util.List;

/**
 * ClassName: ServerService
 * Package: lego.service
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:49
 * @Version 1.0
 */
public interface ServerService {

    List<Server> getAllServers();
    List<Server> saveServer(Server server);
    List<Server> deleteServer(String id);
    List<Server> startServer(String id);
    List<Server> stopServer(String id);
    int isServerNameExist(String name);

}
