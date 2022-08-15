package com.cisdi.transaction.domain.model;

import lombok.Data;

/**干部部门信息
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/4 17:29
 */
@Data
public class GbOrgInfo {

    /**
     * 干部姓名
     */
    private String id;

    /**
     * 干部姓名
     */
    private String name;

    /**
     * 干部身份证id
     */
    private String cardId;

    /**
     * 单位
     */
    private String unit;

    /**
     * 单位编码
     */
    private String unitCode;

    /**
     * 部门
     */
    private String deparment;

    /**
     * 部门编码
     */
    private String deparmentCode;


    /**
     * 职务
     */
    private String post;


    /**
     * 职务类型
     */
    private String postType;

    private String orgCode;

}
