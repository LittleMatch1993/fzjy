package com.cisdi.transaction.config.excel;

/**
 * @author tgl
 * @version 1.0
 * @date 2022/8/31 10:32
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Excel统一社会信用代码/注册号校验注解</p>
 *
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface UnifiedCreditCodeValid {
    String message() default "统一社会信用代码/注册号不正确";
}
