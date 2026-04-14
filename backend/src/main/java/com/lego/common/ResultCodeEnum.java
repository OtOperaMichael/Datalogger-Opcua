package com.lego.common;

/**
 * ClassName: ResultCode
 * Package: LEGO.common
 * Description:
 * Author Michael Zhu
 * Create 2026/1/8 16:44
 * Version 1.0
 */


public enum ResultCodeEnum {

    SUCCESS(200, "success"),
    FAILURE(501, "failure"),
    TEMPLATE_NAME_EXIST(502, "templateNameExist"),
    SERVER_NAME_EXIST(503, "serverNameExist"),
    NOTLOGIN(504, "notLogin"),
    USERNAME_USED(505, "userNameUsed"),
    CONFIG_NOTEXIST(506, "configNotExist");

    private Integer code;
    private String message;

    private ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
