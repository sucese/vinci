package com.guoxiaoxing.vinci.util;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Convert data type between rn and java
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/9/7 下午2:07
 */
public final class DataTypeUtils {

    public static WritableMap toWritableMap(Map<String, ?> map) {
        WritableMap writableMap = Arguments.createMap();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            putIntoMap(writableMap, entry.getKey(), entry.getValue());
        }
        return writableMap;
    }

    public static WritableArray toWritableArray(Collection<?> map) {
        WritableArray writableArray = Arguments.createArray();
        for (Object obj : map) {
            putIntoArray(writableArray, obj);
        }
        return writableArray;
    }

    private static void putIntoMap(WritableMap writableMap, String key, Object value) {
        if (value instanceof String) {
            writableMap.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            writableMap.putBoolean(key, (Boolean) value);
        } else if (value instanceof Double) {
            writableMap.putDouble(key, (Double) value);
        } else if (value instanceof Integer) {
            writableMap.putInt(key, (Integer) value);
        } else if (value == null) {
            writableMap.putNull(key);
        } else if (value.getClass().isArray()) {
            WritableArray array = Arguments.createArray();
            for (int i = 0, size = Array.getLength(value); i < size; ++i) {
                putIntoArray(array, Array.get(value, i));
            }
            writableMap.putArray(key, array);
        } else if (value instanceof Collection) {
            writableMap.putArray(key, toWritableArray((Collection<?>) value));
        } else if (value instanceof Map) {
            //noinspection unchecked
            writableMap.putMap(key, toWritableMap((Map<String, ?>) value));
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static void putIntoArray(WritableArray writableArray, Object value) {
        if (value instanceof String) {
            writableArray.pushString((String) value);
        } else if (value instanceof Boolean) {
            writableArray.pushBoolean((Boolean) value);
        } else if (value instanceof Double) {
            writableArray.pushDouble((Double) value);
        } else if (value instanceof Integer) {
            writableArray.pushInt((Integer) value);
        } else if (value == null) {
            writableArray.pushNull();
        } else if (value.getClass().isArray()) {
            WritableArray array = Arguments.createArray();
            for (int i = 0, size = Array.getLength(value); i < size; ++i) {
                putIntoArray(array, Array.get(value, i));
            }
            writableArray.pushArray(array);
        } else if (value instanceof Collection) {
            writableArray.pushArray(toWritableArray((Collection<?>) value));
        } else if (value instanceof Map) {
            //noinspection unchecked
            writableArray.pushMap(toWritableMap((Map<String, ?>) value));
        } else {
            throw new IllegalArgumentException();
        }
    }
}
