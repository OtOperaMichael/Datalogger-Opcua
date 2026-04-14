package com.lego.test;

import com.lego.util.FIlepathUtil;

/**
 * ClassName: PathUtilTest
 * Package: lego.test
 * Description:
 *
 * @Author michael.zhu
 * @Create 2/25/2026 3:10 PM
 * @Version 1.0
 */
public class PathUtilTest {

    public static void main(String[] args) {
        System.out.println(FIlepathUtil.getDatabaseConfigPath());
        System.out.println(FIlepathUtil.getSystemConfigPath());
        System.out.println(FIlepathUtil.getTemplateListPath());
        System.out.println(FIlepathUtil.getServerListPath());
        System.out.println(System.getProperty("user.home"));
    }
}
