package com.lego.test;

import io.github.libplctag.Tag;

/**
 * ClassName: LibplctagTest
 * Package: lego.test
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/17 19:30
 * @Version 1.0
 */
public class LibplctagTest {
    public static void main(String[] args) {

        Tag tag = new Tag("protocol=ab-eip&gateway=10.146.47.13&path=1,0&plc=ControlLogix&elem_count=10&name=TestArray", 100);
        int rc = tag.getStatus();
        if (rc != Tag.PLCTAG_STATUS_OK) {
            System.err.println("Unable to create the tag, got error " + Tag.decodeError(rc) + "!");
            System.exit(1);
        }

        int a,b,c,d,e,f,g,h,j, k;
        while (true) {
            // 读取数据
            int status = tag.read(1000);
            if (status != Tag.PLCTAG_STATUS_OK) {
                System.err.println("❌ 读取失败: ");
            }


            a = tag.getInt16(0);
            b = tag.getInt16(2);
            c = tag.getInt16(4);
            d = tag.getInt16(6);
            e = tag.getInt16(8);
            f = tag.getInt16(10);
            g = tag.getInt16(12);
            h = tag.getInt16(14);
            j = tag.getInt16(16);
            k = tag.getInt16(18);

            System.out.println("a:"+a+";    "+"b:"+b+";    "+"c:"+c+";    "+"d:"+d+";    "+"e:"+e
                    +";    "+"f:"+f+";    "+"g:"+g+";    "+"h:"+h+";    "+"j:"+j+";    "+"k:"+k);


            try {
                Thread.sleep(1000);
            } catch (InterruptedException err) {
                throw new RuntimeException(err);
            }


        }
    }
}
