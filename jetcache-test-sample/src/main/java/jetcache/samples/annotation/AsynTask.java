/**
 * Created on  13-09-04
 */
package jetcache.samples.annotation;


import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AsynTask {

    String parentName() default "";

    String name();

    String threadPoolName() default "";

    int subTasks();
}
