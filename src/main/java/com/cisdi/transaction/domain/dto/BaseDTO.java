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
     * 登陆人姓名，接收到的是账号
     */
    @ApiModelProperty(value = "登陆人姓名")
    private String serviceUserName;

    /**
     * 登陆人姓名
     */
    @ApiModelProperty(value = "登陆人姓名")
    private String servicePersonName;

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

    /**
     * 登录人所组织code
     */
    @ApiModelProperty(value = "登陆人所在登录人所组织code")
    private String orgCode;
    /**
     * 登录人所在组织名称
     */
    @ApiModelProperty(value = "登录人所在组织名称")
    private String orgName;


}
