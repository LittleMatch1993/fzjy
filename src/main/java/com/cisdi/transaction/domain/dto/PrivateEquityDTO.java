package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/4 13:33
 */
@Data
@ApiModel(description = "配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况")
public class PrivateEquityDTO extends BaseDTO{

    @ApiModelProperty(value = "id")
    private String id;
    /**
     * 租户id
     */
    @ApiModelProperty(value = "租户ID")
    private String tenantId;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @ApiModelProperty(value = " 修改时间")
    private Date updateTime;


    /**
     * 干部姓名
     */
    @ApiModelProperty(value = "干部姓名")
    @NotBlank(message = "干部姓名不能为空")
    private String gbName;


    /**
     * 身份证号
     */
    @ApiModelProperty(value = "身份证号")
    @NotBlank(message = "身份证号不能为空")
    private String  cardId;

    /**
     * 工作单位
     */
    @ApiModelProperty(value = "工作单位")
    private String  company;

    /**
     * 现任职务
     */
    @ApiModelProperty(value = "现任职务")
    private String  post;

    /**
     * 干部类职务类型
     */
    @ApiModelProperty(value = "干部职务类型")
    private String postType;

    /**
     * 职务层次
     */
    @ApiModelProperty(value = "职务层次")
    private String  gradation;

    /**
     * 标签
     */
    @ApiModelProperty(value = "标签")
    private String  label;

    /**
     * 职级
     */
    @ApiModelProperty(value = "职级")
    private String  gbRank;

    /**
     * 人员类别
     */
    @ApiModelProperty(value = "人员类别")
    private String  userType;

    /**
     * 政治面貌
     */
    @ApiModelProperty(value = "政治面貌")
    private String  face;

    /**
     * 在职状态
     */
    @ApiModelProperty(value = "在职状态")
    private String  jobStatus;

    /**
     * 姓名
     */
    @ApiModelProperty(value = "姓名")
    private String  name;

    /**
     * 称谓
     */
    @ApiModelProperty(value = "称谓")
    private String  title;

    /**
     * 投资的私募股权投资基金产品名称
     */
    @ApiModelProperty(value = "投资的私募股权投资基金产品名称")
    private String  privateequityName;

    /**
     * 编码
     */
    @ApiModelProperty(value = "编码")
    private String  code;

    /**
     * 基金总实缴金额（人民币万元）
     */
    @ApiModelProperty(value = "基金总实缴金额（人民币万元）")
    private String  money;

    /**
     * 个人实缴金额（人民币万元）
     */
    @ApiModelProperty(value = "个人实缴金额（人民币万元）")
    private String  personalMoney;

    /**
     * 基金投向
     */
    @ApiModelProperty(value = "基金投向")
    private String  investDirection;

    /**
     * 基金合同签署日
     */
    @ApiModelProperty(value = "基金合同签署日")
    private String  contractTime;

    /**
     * 基金合同约定的到期日
     */
    @ApiModelProperty(value = "基金合同约定的到期日")
    private String  contractExpireTime;

    /**
     * 私募股权投资基金管理人名称
     */
    @ApiModelProperty(value = "私募股权投资基金管理人名称")
    private String  manager;

    /**
     * 登记编号
     */
    @ApiModelProperty(value = "登记编号")
    private String  registrationNumber;

    /**
     * 是否为该基金管理人的实际控制人
     */
    @ApiModelProperty(value = "是否为该基金管理人的实际控制人")
    private String  controller;

    /**
     * 是否为该基金管理人的股东（合伙人）
     */
    @ApiModelProperty(value = "是否为该基金管理人的股东（合伙人）")
    private String  shareholder;

    /**
     * 认缴金额（人民币万元）
     */
    @ApiModelProperty(value = "认缴金额（人民币万元）")
    private String  subscriptionMoney;

    /**
     * 认缴比例（%）
     */
    @ApiModelProperty(value = "认缴比例（%）")
    private String  subscriptionRatio;

    /**
     * 认缴时间
     */
    @ApiModelProperty(value = "认缴时间")
    private String  subscriptionTime;

    /**
     * 是否担任该基金管理人高级职
     */
    @ApiModelProperty(value = "是否担任该基金管理人高级职")
    private String  practice;

    /**
     * 所担任的高级职务名称
     */
    @ApiModelProperty(value = "所担任的高级职务名称")
    private String  postName;

    /**
     * 担任高级职务的开始时间
     */
    @ApiModelProperty(value = "担任高级职务的开始时间")
    private String  inductionStartTime;

    /**
     * 担任高级职务的结束时间
     */
    @ApiModelProperty(value = "担任高级职务的结束时间")
    private String  inductionEndTime;

    /**
     *基金管理人的经营范围
     */
    @ApiModelProperty(value = "基金管理人的经营范围")
    private String  managerOperatScope;

    /**
     * 是否与报告人所在单位（系统）直接发生过经济关系
     */
    @ApiModelProperty(value = "是否与报告人所在单位（系统）直接发生过经济关系")
    private String  isRelation;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String  remarks;

    /**
     * 填报类型
     */
    @ApiModelProperty(value = "填报类型")
    private String  tbType;

    /**
     * 年度
     */
    @ApiModelProperty(value = "年度")
    private String  year;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private String  state;

    /**
     *  创建人
     */
    @ApiModelProperty(value = "创建人")
    private String  creatorId;

    /**
     * 修改人
     */
    @ApiModelProperty(value = " 修改人")
    private String  updaterId;

    /**
     * 有无此类情况
     */
    @ApiModelProperty(value = "有无此类情况")
    @NotBlank(message = "有无此类情况不能为空")
    private String  isSituation;

    @ApiModelProperty(value = "家属证件类型")
    private String familyCardType;

    @ApiModelProperty(value = "家属证件号")
    private String familyCardId;

    @ApiModelProperty(value = "是否投资私募股权投资基金")
    private String isInvest;

}
