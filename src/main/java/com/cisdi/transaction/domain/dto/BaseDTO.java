package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/9 9:59
 */
@Data
@ApiModel(description = "平台传过来的参数")
public class BaseDTO {
    /**
     * 租户ID
     */
    @ApiModelProperty(value = "租户ID")
    private String serviceLesseeId;

    /**
     * 登陆人姓名
     */
    @ApiModelProperty(value = "登陆人姓名")
    private String serviceUserName;

    /**
     * 登陆人id
     */
    @ApiModelProperty(value = "登陆人id")
    private String serviceUserId;

    /**
     * 登陆人账号
     */
    @ApiModelProperty(value = "登陆人账号")
    private String serviceUserAccount;

    /**
     * 登录人所在单位
     */
    @ApiModelProperty(value = "登陆人所在单位")
    private String serviceLesseeName;


}
