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
 * <p>Excel导入日期校验注解</p>
 *
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DateStringValid {
    String message() default "日期正确格式为yyyy-MM-dd";
}
