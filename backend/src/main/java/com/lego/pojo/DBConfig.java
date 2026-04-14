package com.lego.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName: MysqlConfig
 * Package: lego.pojo
 * Description:
 *
 * @Author michael.zhu
 * @Create 2/25/2026 3:20 PM
 * @Version 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DBConfig {

    private String username;
    private String password;
    private String driverClassName;
    private String url;
}
