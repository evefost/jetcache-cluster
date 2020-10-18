package jetcache.samples;

/**
 * context 配置拦截器
 * <p>
 *
 * @author 谢洋
 * @version 1.0.0
 * @date 2020/1/3
 */
public interface ContextInterceptor<C extends Context> {

    /**
     * 设置context
     *
     * @param context
     */
    void setContext(C context);


    /**
     * 清除context
     *
     * @param context
     */
    void clearContext(C context);

}
