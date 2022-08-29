package com.cisdi.transaction.config.excel;

/**
 * @author tgl
 * @version 1.0
 * @date 2022/8/16 10:32
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Excel身份证校验注解</p>
 *
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface IdCardValid {
    String message() default "身份证号码不正确";
}
