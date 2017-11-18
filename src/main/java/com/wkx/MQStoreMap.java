package com.wkx;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MQStoreMap {

    private static Map<String,Set<Method>> methodMap=new ConcurrentHashMap<>();

    private static Map<Method,Object> methodObjectMap=new ConcurrentHashMap<>();

    public static Map<String,Set<Method>> getMethodMap(){
        return methodMap;
    }

    public static Map<Method,Object> getMethodObjectMap(){
        return methodObjectMap;
    }

}
