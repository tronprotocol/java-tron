package org.tron.core.net2.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.springframework.util.StringUtils;

import java.util.Map;

public class JsonUtil {

    /**
     * json字符串转换为map对象
     *
     * @param jsonString
     * @return
     */
    @SuppressWarnings("unchecked")
    public static final Map<String, Object> json2Map(String jsonString) {
        if (jsonString == null || jsonString.equals("")) {
            return null;
        }
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(jsonString, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static final <T> Map<String, T> json2Map(String jsonString, Class<T> clazz) {
        return (Map<String, T>) json2Map(jsonString);
    }

    /**
     * json字符串转换为制定类型的对象
     *
     * @param jsonString
     * @param clazz
     * @return
     */
    public static final <T> T json2Obj(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString) || clazz == null) {
            return null;
        }
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(jsonString, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * json字符串转换为制定类型的对象
     *
     * @param jsonString
     * @param clazz
     * @return
     */
    public static final <T> T fromJson(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString) || clazz == null) {
            return null;
        }
        try {
            Gson gson = new Gson();
            return gson.fromJson(jsonString, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * obj转换为json格式的string
     *
     * @param obj
     * @return
     */
    public static final String obj2String(Object obj) {
        if (obj == null) {
            return null;
        }
        ObjectMapper om = new ObjectMapper();
        try {
            return om.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取不同层key的value，例如，获取2层key的value：{"key1":{"key2":"value"}}
     *
     * @param jsonString
     * @param keys
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static final Object getValueFromJson(String jsonString, String... keys) {
        Object object = json2Obj(jsonString, Map.class);

        for (int i = 0; i < keys.length; i++) {
            if ((object != null) && (object instanceof Map)) {
                object = ((Map) object).get(keys[i]);
            } else {
                return null;
            }
        }
        return object;
    }
}