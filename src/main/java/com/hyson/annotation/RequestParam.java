package com.hyson.annotation;

import java.lang.annotation.*;

/**
 * @author wyh
 * @version 1.0
 * @time 2020/10/14 3:03 下午
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

    String value();

}
