package com.cisdi.transaction.domain.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/4 12:44
 */
@Data
@ApiModel(description = "禁止交易信息")
public class BanDealInfoDTO extends BaseDTO{

        @ApiModelProperty(value = "id")
        private String id;

        @ApiModelProperty(value = "身份证号")
        @NotBlank(message = "身份证号不能为空")
        private String cardId;

        @ApiModelProperty(value = "姓名")
        @NotBlank(message = "姓名不能为空")
        private String name;

        @ApiModelProperty(value = "单位")
        @NotBlank(message = "单位不能为空")
        private String company;

        @ApiModelProperty(value = "职务")
        @NotBlank(message = "职务不能为空")
        private String post;

        @ApiModelProperty(value = "职务类型")
        @NotBlank(message = "职务类型不能为空")
        private String postType;

        @ApiModelProperty(value = "家人姓名")
        @NotBlank(message = "家人姓名不能为空")
        private String familyName;

        @ApiModelProperty(value = "家人关系")
        @NotBlank(message = "家人关系不能为空")
        private String relation;

        @ApiModelProperty(value = "经商类型")
        @NotBlank(message = "经商类型不能为空")
        private String engageType;

        @ApiModelProperty(value = "经商类型详细描述")
        @NotBlank(message = "经商类型详细描述不能为空")
        private String engageInfo;

        @ApiModelProperty(value = "经营范围")
        @NotBlank(message = "经营范围不能为空")
        private String operatScope;

        @ApiModelProperty(value = "数据校验提示")
        private String checkTips;

        @ApiModelProperty(value = "供应商名称")
        @NotBlank(message = "供应商名称不能为空")
        private String supplier;

        @ApiModelProperty(value = "统一社会信用代码")
        @NotBlank(message = "统一社会信用代码不能为空")
        private String code;

        @ApiModelProperty(value = "禁止交易采购单位代码")
        @NotBlank(message = "禁止交易采购单位代码不能为空")
        private String banPurchaseCode;

        @ApiModelProperty(value = "禁止交易采购单位名称")
        @NotBlank(message = "禁止交易采购单位名称不能为空")
        private String banPurchaseName;

        @ApiModelProperty(value = "是否继承关系")
        @NotBlank(message = "是否继承关系不能为空")
        private String isExtends;

        @ApiModelProperty(value = "管理单位名称")
        @NotBlank(message = "管理单位名称不能为空")
        private String manageCompany;

        @ApiModelProperty(value = "管理单位代码")
        @NotBlank(message = "管理单位代码不能为空")
        private String manageCompanyCode;

        @ApiModelProperty(value = "创建人")
        private String creator;

        @ApiModelProperty(value = "创建人账号")
        private String creatorAccount;

        @ApiModelProperty(value = "禁止职务类型")
        @NotBlank(message = "禁止职务类型不能为空")
        private String banPostType;

        @ApiModelProperty(value = "企业状态")
        private String enterpriseState;


}
