package com.cisdi.transaction.config.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * @Author: tgl
 * @Description: 日期比较工具
 * @Date: 2022/8/18 13:46
 */
public class CalendarUtil {
    /**
     * 是否大于系统时间
     * @param dateString
     * @return
     */
    public static boolean greaterThanNow(String dateString){
        Calendar calendar = GregorianCalendar.getInstance();
        try {
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(dateString));
        }catch (ParseException e){
            throw new RuntimeException("日期格式错误");
        }
        if (calendar.compareTo(GregorianCalendar.getInstance())>0){
            return true;
        }else {
            return false;
        }
    }

    public static boolean compare(String oneDateString,String twoDateString){

        try {
            Calendar calendarOne = GregorianCalendar.getInstance();
            calendarOne.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(oneDateString));

            Calendar calendarTwo = GregorianCalendar.getInstance();
            calendarTwo.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(twoDateString));

            if (calendarOne.compareTo(calendarTwo)>0){
                return  true;
            }else {
                return false;
            }
        }catch (ParseException e){
            throw new RuntimeException("日期格式错误");
        }
    }
}
