package com.jetcahe.support.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ListCacheInvalidate {

    String area() default "default";

    String name();

    String key() default "$$undefined$$";

    String condition() default "$$undefined$$";

    boolean multi() default false;
}
