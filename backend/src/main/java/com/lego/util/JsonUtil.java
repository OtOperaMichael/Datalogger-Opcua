package com.lego.util;

/**
 * ClassName: JsonUtil
 * Package: lego.util
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 18:32
 * @Version 1.0
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON 工具类：提供线程安全的 ObjectMapper 单例
 */
public final class JsonUtil {

    // 使用 static final 保证全局唯一、线程安全（ObjectMapper 是线程安全的，一旦配置完成）
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 静态初始化块：配置 ObjectMapper
    static {
        // 忽略未知字段（防止 JSON 多字段导致反序列化失败）
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 序列化时忽略 null 值（可选，按需开启）
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 自动识别日期时间类型（如 LocalDateTime, Instant 等）
        MAPPER.registerModule(new JavaTimeModule());

        // 禁用时间戳格式（使用 ISO 8601 字符串，如 "2026-02-10T18:30:00"）
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 允许对象为空（反序列化时如果 JSON 为 null，不报错）
        MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    /**
     * 获取配置好的 ObjectMapper 实例
     * @return 全局共享的 ObjectMapper（线程安全）
     */
    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    /**
     * 将对象转为 JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * 将 JSON 字符串转为指定类型的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }
}
