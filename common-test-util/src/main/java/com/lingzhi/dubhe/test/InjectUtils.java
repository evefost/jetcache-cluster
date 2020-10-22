package com.lingzhi.dubhe.test;

import org.springframework.aop.framework.AopProxyUtils;

import java.lang.reflect.Field;

/**
 * @author xieyang
 */
public abstract class InjectUtils {

    public static void injectFields(Object target, Object... fieldValues) throws IllegalAccessException {

        Object singletonTarget = AopProxyUtils.getSingletonTarget(target);
        if(singletonTarget == null){
            singletonTarget = target;
        }
        Class<?> aClass = AopProxyUtils.ultimateTargetClass(target);
        Field[] declaredFields = aClass.getDeclaredFields();
        for (Field field : declaredFields) {
            for(Object fieldValue:fieldValues){
                if(field.getType().isAssignableFrom(fieldValue.getClass())){
                    field.setAccessible(true);
                    field.set(singletonTarget, fieldValue);
                    break;
                }
            }
        }

    }

}
