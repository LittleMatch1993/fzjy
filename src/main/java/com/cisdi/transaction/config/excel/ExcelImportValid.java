package com.cisdi.transaction.config.excel;

import cn.hutool.core.util.IdcardUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.cisdi.transaction.config.utils.CreditCodeUtil;
import com.cisdi.transaction.config.utils.IdCardUtil;
import com.cisdi.transaction.constant.SystemConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;

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
        StringBuilder errorMessage=new StringBuilder();
        StringBuilder currentColumnErrorMessage;
        for (Field field : fields) {
            currentColumnErrorMessage=new StringBuilder();
            //设置可访问
            field.setAccessible(true);
            //属性的值
            Object fieldValue = null;
            try {
                fieldValue = field.get(object);
            } catch (IllegalAccessException e) {
                throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", "导入参数检查失败");
            }
            boolean excelProperty = field.isAnnotationPresent(ExcelProperty.class);
            if (!excelProperty){
                continue;
            }
            String columnValue = field.getAnnotation(ExcelProperty.class).value()[0];
            //是否包含必填校验注解
            boolean isExcelValid = field.isAnnotationPresent(ExcelValid.class);
            if (isExcelValid && (Objects.isNull(fieldValue))) {
//                throw new ExceptionCustom("NULL", field.getAnnotation(ExcelValid.class).message());
                currentColumnErrorMessage.append("、").append(field.getAnnotation(ExcelValid.class).message());
            }

            //统一社会信用代码/注册号校验注解
            boolean unifiedCreditCodeValid = field.isAnnotationPresent(UnifiedCreditCodeValid.class);
            if (unifiedCreditCodeValid&&Objects.nonNull(fieldValue)&&fieldValue instanceof String&&StringUtils.isNotBlank((String)fieldValue)&& !CreditCodeUtil.validateUnifiedCreditCode((String)fieldValue)){
//                throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", field.getAnnotation(UnifiedCreditCodeValid.class).message());
                currentColumnErrorMessage.append("、").append(field.getAnnotation(UnifiedCreditCodeValid.class).message());
            }

            boolean dateStringValid = field.isAnnotationPresent(DateStringValid.class);
            //日期字符串校验
            if (dateStringValid &&Objects.nonNull(fieldValue)&&fieldValue instanceof String&& StringUtils.isNotBlank((String)fieldValue)) {
                if (!GenericValidator.isDate((String)fieldValue, "yyyy-MM-dd", true)){
//                    throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", field.getAnnotation(DateStringValid.class).message());
                    currentColumnErrorMessage.append("、").append(field.getAnnotation(DateStringValid.class).message());
                }
            }

            boolean timeStringValid = field.isAnnotationPresent(TimeStringValid.class);
            //时间字符串校验
            if (timeStringValid &&Objects.nonNull(fieldValue)&&fieldValue instanceof String&& StringUtils.isNotBlank((String)fieldValue)) {
                if (!GenericValidator.isDate((String)fieldValue, "yyyy-MM-dd HH:mm:ss", true)){
//                    throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", field.getAnnotation(TimeStringValid.class).message());
                    currentColumnErrorMessage.append("、").append(field.getAnnotation(TimeStringValid.class).message());
                }
            }

            //身份证校验
            boolean idCardValid = field.isAnnotationPresent(IdCardValid.class);
            //时间字符串校验
            if (idCardValid &&Objects.nonNull(fieldValue)) {
                if (!IdCardUtil.isIdcard(""+fieldValue)){
//                    throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", field.getAnnotation(IdCardValid.class).message());
                    currentColumnErrorMessage.append("、").append(field.getAnnotation(IdCardValid.class).message());
                }
            }
//            //如果两个注解都有，则不能为"无"
//            if (isExcelValid&&dateStringValid&&SystemConstant.NO.equals(fieldValue)){
//                throw new ExceptionCustom("NULL", field.getAnnotation(ExcelValid.class).message());
//            }
            //有无此类情况校验
            if (field.isAnnotationPresent(IsSituationValid.class)&&fieldValue instanceof String&& !Arrays.asList("有此类情况","无此类情况").contains((String)fieldValue)){
//                throw new ExceptionCustom("IMPORT_PARAM_CHECK_FAIL", field.getAnnotation(IsSituationValid.class).message());
                currentColumnErrorMessage.append("、").append(field.getAnnotation(IsSituationValid.class).message());
            }
            if (currentColumnErrorMessage.length()>0){
                errorMessage.append(columnValue).append(":").append(currentColumnErrorMessage.substring(1)).append(";");
            }
        }
        if (errorMessage.length()>0){
            throw new ExceptionCustom("NULL", errorMessage.toString());
        }
    }
}
