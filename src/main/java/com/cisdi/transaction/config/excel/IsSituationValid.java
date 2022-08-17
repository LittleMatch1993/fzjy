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
 * <p>Excel导入有无此类状况校验注解</p>
 *
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface IsSituationValid {
    String message() default "有无此类情况错误";
}
