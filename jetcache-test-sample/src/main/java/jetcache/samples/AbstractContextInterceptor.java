package jetcache.samples;


import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * context 配置拦截器
 * <p>
 *
 * @author 谢洋
 * @version 1.0.0
 * @date 2020/1/3
 */
public abstract class AbstractContextInterceptor<C extends Context<String>> implements ContextInterceptor<C> {
    /**
     * 上下文元素key
     */
    private static volatile List<String> contextElementKeys;

    /**
     * 默认租户id
     */
    private static String defaultTenantId;

    /**
     * 默认storeId
     */
    private static String defaultStoreId;

    public static void setDefaultTenantId(String defaultTenantId) {
        AbstractContextInterceptor.defaultTenantId = defaultTenantId;
    }

    public static void setDefaultStoreId(String defaultStoreId) {
        AbstractContextInterceptor.defaultStoreId = defaultStoreId;
    }

    /**
     * 如果有匹配,设置默认的，主要给天虹或其它用独立部发布
     *
     * @param context
     */
    protected void setDefaultTenant(Context<String> context) {
        if (defaultTenantId != null) {
            context.put("tenant-id", defaultTenantId);
            context.put("store-id", defaultStoreId);
        }
    }

    /**
     * 设置context
     *
     * @param context
     */
    @Override
    public void setContext(C context) {
        //设置默认租户
        setDefaultTenant(context);
        if (context.in()) {
            //设置日志扩展属性
        }
        if (contextElementKeys != null) {
            contextElementKeys.forEach((key) -> {
                set(context, key);
            });
        }

    }

    /**
     * 获取默认的 context
     *
     * @return
     */
    public static DefaultContext getDefaultContext() {
        DefaultContext defaultContext = new DefaultContext(false);
        if (contextElementKeys != null) {
            contextElementKeys.forEach((key) -> {
               // defaultContext.put(key, (String) ServerContextHolder.getData(key));
            });
        }
        return defaultContext;
    }

    /**
     * 1. 入站从context 数据放到ServerContextHolder
     * 2. 出站数据从ServerContextHolder 放到context里
     *
     * @param context
     * @param key
     */
    private void set(C context, String key) {
        if (context.in()) {
            //ServerContextHolder.setData(key, context.get(key));
        } else {
            //context.put(key, (String) ServerContextHolder.getData(key));
        }
    }

    /**
     * 清除context
     *
     * @param context
     */
    @Override
    public void clearContext(C context) {
        if (contextElementKeys != null) {
            contextElementKeys.forEach((key) -> {
                context.remove(key);
            });
        }
    }

    public static void setContextElementKeys(List<String> newValues) {
        List<String> finalKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(newValues)) {
            for (String key : newValues) {
                finalKeys.add(key);
            }
        }
        contextElementKeys = Collections.unmodifiableList(finalKeys);
    }

    public static List<String>  getContextElementKeys() {
       return Collections.unmodifiableList(contextElementKeys);
    }
}
