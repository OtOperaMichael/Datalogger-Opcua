package com.lego.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * ClassName: Instance
 * Package: lego.pojo
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:27
 * @Version 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Server {

    private String id;
    private String name;
    private String hostIP;
    private String templateName;

    // running / stopped
    private boolean running = false;
    // 是否开机自启（可选）
    private boolean autoStart = false;

}

