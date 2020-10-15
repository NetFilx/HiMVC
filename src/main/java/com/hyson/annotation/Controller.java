package com.hyson.annotation;

import java.lang.annotation.*;

/**
 * @author wyh
 * @version 1.0
 * @time 2020/10/14 3:01 下午
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {

    String value() default "";

}
