package com.jetcahe.support.extend;

import com.alicp.jetcache.anno.aop.CachePointcut;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.support.ConfigMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class BatchCachePointcut extends CachePointcut {
    private ConfigMap cacheConfigMap;
    private String[] basePackages;

    public BatchCachePointcut(String[] basePackages) {
        super(basePackages);
    }

    @Override
    public boolean matches(Method method, Class targetClass) {
        boolean b = matchesImpl(method, targetClass);
        return b;
    }

    private boolean matchesImpl(Method method, Class targetClass) {
        if (!matchesThis(method.getDeclaringClass())) {
            return false;
        }
        if (exclude(targetClass.getName())) {
            return false;
        }
        String key = getKey(method, targetClass);
        CacheInvokeConfig cac = cacheConfigMap.getByMethodInfo(key);
        if (cac == CacheInvokeConfig.getNoCacheInvokeConfigInstance()) {
            return false;
        } else if (cac != null) {
            return true;
        } else {
            cac = new CacheInvokeConfig();
            ExtendCacheConfigUtil.parse(cac, method);
            String name = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            parseByTargetClass(cac, targetClass, name, paramTypes);

            if (!cac.isEnableCacheContext() && cac.getCachedAnnoConfig() == null &&
                    cac.getInvalidateAnnoConfigs() == null && cac.getUpdateAnnoConfig() == null) {
                cacheConfigMap.putByMethodInfo(key, CacheInvokeConfig.getNoCacheInvokeConfigInstance());
                return false;
            } else {
                cacheConfigMap.putByMethodInfo(key, cac);
                return true;
            }
        }
    }
    private void parseByTargetClass(CacheInvokeConfig cac, Class<?> clazz, String name, Class<?>[] paramTypes) {
        if (!clazz.isInterface() && clazz.getSuperclass() != null) {
            parseByTargetClass(cac, clazz.getSuperclass(), name, paramTypes);
        }
        Class<?>[] intfs = clazz.getInterfaces();
        for (Class<?> it : intfs) {
            parseByTargetClass(cac, it, name, paramTypes);
        }

        boolean matchThis = matchesThis(clazz);
        if (matchThis) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (methodMatch(name, method, paramTypes)) {
                    ExtendCacheConfigUtil.parse(cac, method);
                    break;
                }
            }
        }
    }
    private boolean exclude(String name) {
        if (name.startsWith("java")) {
            return true;
        }
        if (name.startsWith("org.springframework")) {
            return true;
        }
        if (name.indexOf("$$EnhancerBySpringCGLIB$$") >= 0) {
            return true;
        }
        if (name.indexOf("$$FastClassBySpringCGLIB$$") >= 0) {
            return true;
        }
        return false;
    }

    private boolean methodMatch(String name, Method method, Class<?>[] paramTypes) {
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        if (!name.equals(method.getName())) {
            return false;
        }
        Class<?>[] ps = method.getParameterTypes();
        if (ps.length != paramTypes.length) {
            return false;
        }
        for (int i = 0; i < ps.length; i++) {
            if (!ps[i].equals(paramTypes[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setCacheConfigMap(ConfigMap cacheConfigMap) {
        super.setCacheConfigMap(cacheConfigMap);
        this.cacheConfigMap = cacheConfigMap;
    }
}
