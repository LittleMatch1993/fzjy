package com.cisdi.transaction.config.utils;


import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: tgl
 * @Description:
 * @Date: 2022/8/8 11:20
 */
public class NumberUtils {

    public static boolean isNumeric(String str){

        Pattern pattern = Pattern.compile("[0-9]*\\.?[0-9]+");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isAllNumeric(String ...numberString){
        if (Objects.isNull(numberString)||numberString.length==0){
            return false;
        }
        for (String string : numberString) {
            if (StringUtils.isBlank(string)||!isNumeric(string)){
                return false;
            }
        }
        return true;
    }

    public static boolean isAllNumeric(List<String> stringList){
        if (CollectionUtils.isEmpty(stringList)){
            return false;
        }
        for (String string : stringList) {
            if (StringUtils.isBlank(string)||!isNumeric(string)){
                return false;
            }
        }
        return true;
    }
}
