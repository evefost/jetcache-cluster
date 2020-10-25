/**
 * Created on  13-09-04
 */
package jetcache.samples.annotation;


import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MultiTask {

    String parentName() default "";

    String name();

    String threadPoolName() default "";

    /**
     * 任务数
     * @return
     */
    int subTaskCount() default 0;
}
