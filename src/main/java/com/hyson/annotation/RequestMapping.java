package com.hyson.annotation;

import java.lang.annotation.*;

/**
 * @author wyh
 * @version 1.0
 * @time 2020/10/14 3:02 下午
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {

    String value() default "";

}
