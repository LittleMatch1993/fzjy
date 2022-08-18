package com.cisdi.transaction.config.excel;

import com.cisdi.transaction.constant.SystemConstant;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author yuw
 * @version 1.0   <p>Excel导入字段校验</p>
 * @date 2022/8/6 10:33
 */
public class ExcelImportValid {
    /**
     * Excel导入字段校验
     *
     * @param object 校验的JavaBean 其属性须有自定义注解
     */
    public static void valid(Object object) throws ExceptionCustom{
        Field[] fields = object.getClass().getDeclaredFields();
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        for (Field field : fields) {
            //设置可访问
            field.setAccessible(true);
            //属性的值
            Object fieldValue = null;
            try {
                fieldValue = field.get(object);
            } catch (IllegalAccessException e) {
                throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", "导入参数检查失败");
            }
            //是否包含必填校验注解
            boolean isExcelValid = field.isAnnotationPresent(ExcelValid.class);
            if (isExcelValid && (Objects.isNull(fieldValue))) {
                throw new ExceptionCustom("NULL", field.getAnnotation(ExcelValid.class).message());
            }

            boolean dateStringValid = field.isAnnotationPresent(DateStringValid.class);
            //日期字符串校验
            if (dateStringValid &&Objects.nonNull(fieldValue)&&fieldValue instanceof String&& StringUtils.isNotBlank((String)fieldValue)) {
                try {
                    simpleDateFormat.parse((String)fieldValue);
                } catch (ParseException e) {
                    throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", field.getAnnotation(DateStringValid.class).message());
                }
            }
//            //如果两个注解都有，则不能为"无"
//            if (isExcelValid&&dateStringValid&&SystemConstant.NO.equals(fieldValue)){
//                throw new ExceptionCustom("NULL", field.getAnnotation(ExcelValid.class).message());
//            }
            //有无此类情况校验
            if (field.isAnnotationPresent(IsSituationValid.class)&&fieldValue instanceof String&& !Arrays.asList("有此类情况","无此类情况").contains((String)fieldValue)){
                throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", field.getAnnotation(IsSituationValid.class).message());
            }

        }
    }
}
