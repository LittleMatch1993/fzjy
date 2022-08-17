package com.cisdi.transaction;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/10 13:35
 */
public class Test {
    public static void main(String[] args) {

        /*String company = null;;
        String department = null;

        System.out.println((StrUtil.isEmpty(company)||StrUtil.isEmpty(department)) || !(company.contains("中国五矿集团有限公司")&&department.contains("专职董(监)事办公室")));
*/

       /* List<String> list1 = new ArrayList<String>();
        list1.add("1");
        list1.add("2");
        list1.add("3");

        List<String> list2 = new ArrayList<String>();
        list2.add("2");


        System.out.println("======================");
        // list1 差集
        list1 = list1.stream().filter(e -> {
            return !list2.contains(e);
        }).collect(Collectors.toList());
        System.out.println(list1);*/
        String name = "王 五";
        String name1 = "王    五";
        String name2 = " 王 五 ";
        System.out.println(name.replace(" ",""));
        System.out.println(name1.replace(" ",""));
        System.out.println(name2.replace(" ",""));
    }
}
