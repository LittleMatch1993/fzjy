package com.cisdi.transaction.domain.vo;

import lombok.Data;

/** 根据干部姓名，单位 职务 获取信息
 * @Author: cxh
 * @Description: selectGbInfoByNameAndUnitAndPost接口参数
 * @Date: 2022/8/31 16:02
 */
@Data
public class GbInfoByNameAndUnitAndPost {
    private String name;

    private String unit;

    private String post;

    private String cardId;
}
