package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 10:13
 */
@Data
@ApiModel(description = "企业交易")
public class BusinessTransactionDTO extends BaseDTO {

//    @ApiModelProperty(value = "身份证号")
//    @NotBlank(message = "身份证号不能为空")
//    private String cardId;
//
//    @ApiModelProperty(value = "姓名")
//    @NotBlank(message = "姓名不能为空")
//    private String name;
//
//    @ApiModelProperty(value = "单位")
//    @NotBlank(message = "单位不能为空")
//    private String company;
//
//    @ApiModelProperty(value = "职务")
//    @NotBlank(message = "职务不能为空")
//    private String post;
//
//    @ApiModelProperty(value = "职务类型")
//    @NotBlank(message = "职务类型不能为空")
//    private String postType;
//
//    @ApiModelProperty(value = "禁业职务类型")
//    @NotBlank(message = "禁业职务类型不能为空")
//    private String banPostType;
//
//    @ApiModelProperty(value = "家人姓名")
//    @NotBlank(message = "家人姓名不能为空")
//    private String familyName;
//
//    @ApiModelProperty(value = "家人关系")
//    @NotBlank(message = "家人关系不能为空")
//    private String relation;

//    @ApiModelProperty(value = "经商类型")
//    @NotBlank(message = "经商类型不能为空")
//    private String engageType;
//
//    @ApiModelProperty(value = "经商类型详细描述")
//    @NotBlank(message = "经商类型详细描述不能为空")
//    private String engageInfo;
//
//    @ApiModelProperty(value = "经营范围")
//    @NotBlank(message = "经营范围不能为空")
//    private String operatScope;

    @ApiModelProperty(value = "唯一标识码")
    @NotBlank(message = "唯一标识码不能为空")
    private String uniqueCode;

    @ApiModelProperty(value = "供应商名称")
    @NotBlank(message = "供应商名称不能为空")
    private String supplier;

    @ApiModelProperty(value = "统一社会信用代码")
    @NotBlank(message = "统一社会信用代码不能为空")
    private String code;

    @ApiModelProperty(value = "采购单位名称")
    @NotBlank(message = "采购单位名称不能为空")
    private String purchaseName;

    @ApiModelProperty(value = "直管单位名称")
    @NotBlank(message = "直管单位名称不能为空")
    private String straightPipeName;

    @ApiModelProperty(value = "采购业务编码")
    private String businessCode;

    @ApiModelProperty(value = "采购业务名称")
    private String businessName;

    @ApiModelProperty(value = "信息类型")
    private String infoType;

    @ApiModelProperty(value = "供应商报价")
    private String supplierPrice;

    @ApiModelProperty(value = "是否为中标供应商")
    private String bidder;

    @ApiModelProperty(value = "采购合同编码")
    private String contractCode;

    @ApiModelProperty(value = "采购合同名称")
    private String contractName;

    @ApiModelProperty(value = "采购合同金额")
    private String contractPrice;

    @ApiModelProperty(value = "采购合同签订日期")
    private Date contractTime;
//
    @ApiModelProperty(value = "信息提示")
//    @NotBlank(message = "信息提示不能为空")
    private String infoTips;
}
