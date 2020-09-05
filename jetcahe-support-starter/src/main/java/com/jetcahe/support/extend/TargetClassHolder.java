package com.jetcahe.support.extend;


public class TargetClassHolder {

    private static ThreadLocal<Class> cacheThreadLocal = new ThreadLocal<Class>() {
        @Override
        protected Class initialValue() {
            return null;
        }
    };

    public static void set(Class cls){
        cacheThreadLocal.set(cls);
    }

    public static void remove(){
        cacheThreadLocal.remove();
    }

    public static Class get(){
      return   cacheThreadLocal.get();
    }
}
