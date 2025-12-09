package org.ssssssss.magicboot.model;

import java.util.HashMap;
import java.util.Map;

public class UserCacheMap {

    private static Map<String, Map<String, Object>> map = new HashMap<>();

    public static void put(String key, Map<String, Object> value){
        map.put(key, value);
    }

    public static void remove(String key){
        map.remove(key);
    }

    public static Map<String, Object> get(String key){
        return map.get(key);
    }

}
