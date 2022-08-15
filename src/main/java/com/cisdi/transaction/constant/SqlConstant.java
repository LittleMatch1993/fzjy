package com.cisdi.transaction.constant;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/3 17:17
 */
public class SqlConstant {
    private SqlConstant(){

    }

    /**
     * 截取第一个数据
     */
    public static final String ONE_SQL="\t limit 1";


    /**
     * 截取前5个数据
     */
    public static final String ONE_SQL_FIVE="\t limit 5";

    public static final String ONE_SQL_YB="\t limit 200";
}
