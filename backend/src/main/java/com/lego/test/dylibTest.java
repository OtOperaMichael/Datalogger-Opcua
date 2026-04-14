package com.lego.test;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * ClassName: dylibTest
 * Package: lego.test
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/17 19:16
 * @Version 1.0
 */
public class dylibTest {

    // 定义 libplctag C API 的 JNA 接口
    public interface Libplctag extends Library {
        Libplctag INSTANCE = Native.load("plctag", Libplctag.class);

        // 创建标签
        int plc_tag_create(String tagString, int timeout);

        // 获取标签状态
        int plc_tag_status(int tagId);

        // 读取标签
        int plc_tag_read(int tagId, int timeout);

        // 写入标签
        int plc_tag_write(int tagId, int timeout);

        // 获取 INT16 值
        short plc_tag_get_int16(int tagId, int offset);

        // 设置 INT16 值
        int plc_tag_set_int16(int tagId, int offset, short value);

        // 获取 UINT32（用于 DINT/REAL）
        int plc_tag_get_uint32(int tagId, int offset);
        int plc_tag_set_uint32(int tagId, int offset, int value);

        // 获取浮点数
        float plc_tag_get_float32(int tagId, int offset);
        int plc_tag_set_float32(int tagId, int offset, float value);

        // 销毁标签
        void plc_tag_destroy(int tagId);

        // 设置调试级别 (0-5)
        void plc_tag_set_debug_level(int level);
    }

    public static void main(String[] args) {

        // 🔴 启用 JNA 调试输出
        System.setProperty("jna.debug_load", "true");
        System.setProperty("jna.debug_load.jna", "true");

        // === 配置你的 PLC 参数 ===
        String gateway = "192.168.3.200";
        String path = "1,0";          // Slot,CPU
        String plcType = "ControlLogix";
        String tagName = "TestArray"; // 你的数组标签名
        int elemCount = 10;           // 数组长度

        // 构造与 tag_rw2 完全相同的连接字符串
        String tagString = String.format(
                "protocol=ab_eip&gateway=%s&path=%s&plc=%s&elem_count=%d&name=%s",
                gateway, path, plcType, elemCount, tagName
        );

        System.out.println("Connecting with: " + tagString);

        // === 设置调试（可选）===
        // Libplctag.INSTANCE.plc_tag_set_debug_level(4); // 0=none, 4=detail

        // === 加载原生库 ===
        // 方法 1: 如果 libplctag.dylib 在 java.library.path 中
        // 方法 2: 显式指定路径（推荐）
        try {
            // 替换为你的实际路径！
            String libPath = "/Users/michael/tmp2/libplctag_folder/external_zips/libplctag_2.6.12_macos_aarch64";
            System.setProperty("jna.library.path", libPath);

            Libplctag lib = Libplctag.INSTANCE;

            // === 创建标签 ===
            int tagId = lib.plc_tag_create(tagString, 1000); // 15秒超时
            if (tagId < 0) {
                System.err.println("❌ Failed to create tag. Error code: " + tagId);
                return;
            }

            System.out.println("✅ Tag created, ID: " + tagId);

            // === 等待标签就绪 ===
            int status = lib.plc_tag_status(tagId);
            while (status == 1) { // PLCTAG_STATUS_PENDING
                try { Thread.sleep(10); } catch (InterruptedException e) { }
                status = lib.plc_tag_status(tagId);
            }

            if (status != 0) { // not PLCTAG_STATUS_OK
                System.err.println("❌ Tag error after creation: " + status);
                lib.plc_tag_destroy(tagId);
                return;
            }

            // === 读取数据 ===
            System.out.println("📡 Reading tag...");
            int readStatus = lib.plc_tag_read(tagId, 5000);
            if (readStatus == 0) {
                // 读取 TestArray[0] (INT 类型)
                short value0 = lib.plc_tag_get_int16(tagId, 0);
                short value1 = lib.plc_tag_get_int16(tagId, 2); // offset=2 bytes
                System.out.println("✅ TestArray[0] = " + value0);
                System.out.println("✅ TestArray[1] = " + value1);
            } else {
                System.err.println("❌ Read failed with error: " + readStatus);
            }

            // === 可选：写入数据 ===
            /*
            System.out.println("✏️ Writing to TestArray[0]...");
            int writeStatus = lib.plc_tag_write(tagId, 5000);
            if (writeStatus == 0) {
                System.out.println("✅ Write successful!");
            } else {
                System.err.println("❌ Write failed: " + writeStatus);
            }
            */

            // === 清理资源 ===
            lib.plc_tag_destroy(tagId);
            System.out.println("🧹 Tag destroyed.");

        } catch (Exception e) {
            System.err.println("💥 Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}