/**
 * Created on  13-09-04
 */
package jetcache.samples.annotation;


import java.lang.annotation.*;

/**
 * 标识多子任务执行入口，并行执行，同步返回
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MultiSyncTaskEntry {

    /**
     * 任务名称
     * @return
     */
    String name();

    /**
     * 任务执行的线程池
     * @return
     */
    String threadPoolName();

    /**
     * 子任务数
     * @return
     */
    int subTaskCount();
}
