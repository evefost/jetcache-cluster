/**
 * Created on  13-09-04
 */
package jetcache.samples.annotation;


import java.lang.annotation.*;

/**
 * 子任务标识
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MultiSyncSubTask {

    String parentTaskName();

}
