package com.cisdi.transaction.domain.model;

import lombok.Data;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/26 18:58
 */
@Data
public class AuthUser {

    private String userName; //账号

    private String personName; //姓名

    private String unit; //所属单位

}
