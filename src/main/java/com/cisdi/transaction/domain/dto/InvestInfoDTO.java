package com.cisdi.transaction.domain.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/4 13:32
 */
@Data
@ApiModel(description = "投资企业或担任高级职务情况")
public class InvestInfoDTO extends BaseDTO {

    @ApiModelProperty(value = "id")
    private String id;
    /**
     * 租户ID
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
    @ApiModelProperty(value = "修改时间")
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
    @ApiModelProperty(value = " 身份证号")
    @NotBlank(message = "身份证号不能为空")
    private String cardId;
    /**
     * 工作单位
     */
    @ApiModelProperty(value = "工作单位")
    private String company;
    /**
     * 现任职务
     */
    @ApiModelProperty(value = "现任职务")
    private String post;
    /**
     * 职务层次
     */
    @ApiModelProperty(value = "职务层次")
    private String gradation;
    /**
     * 标签
     */
    @ApiModelProperty(value = "标签")
    private String label;
    /**
     * 职级
     */
    @ApiModelProperty(value = "职级")
    private String gbRank;
    /**
     * 人员类型
     */
    @ApiModelProperty(value = "人员类型")
    private String userType;
    /**
     * 政治面貌
     */
    @ApiModelProperty(value = "政治面貌")
    private String face;
    /**
     * 在职状态
     */
    @ApiModelProperty(value = "在职状态")
    private String jobStatus;
    /**
     * 姓名
     */
    @ApiModelProperty(value = "姓名")
    private String name;
    /**
     * 称谓
     */
    @ApiModelProperty(value = "称谓")
    private String title;
    /**
     * 统一社会信用代码
     */
    @ApiModelProperty(value = "统一社会信用代码")
    private String code;
    /**
     * 企业或其他市场主体名称
     */
    @ApiModelProperty(value = "企业或其他市场主体名称")
    private String enterpriseName;
    /**
     * 成立日期
     */
    @ApiModelProperty(value = "成立日期")
    private String establishTime;
    /**
     * 经营范围
     */
    @ApiModelProperty(value = "经营范围")
    private String operatScope;

    @ApiModelProperty(value = "注册地（国家）")
    private String registerCountry;

    @ApiModelProperty(value = "注册地（省份）")
    private String registerProvince;

    @ApiModelProperty(value = "注册地（市）")
    private String city;
//    /**
//     * 注册地点
//     */
//    @ApiModelProperty(value = "注册地（市）")
//    private String register;
//    /**
//     * 城市
//     */
//    @ApiModelProperty(value = "城市")
//    private String city;
    /**
     * 经营低
     */
    @ApiModelProperty(value = "经营地址")
    private String operatAddr;
    /**
     * 企业或其他市场主体类型
     */
    @ApiModelProperty(value = "企业或其他市场主体类型")
    private String enterpriseType;
    /**
     * 注册资本（金）或资金数额（出资额）（人民币万元）
     */
    @ApiModelProperty(value = "注册资本（金）或资金数额（出资额）（人民币万元）")
    private String registerCapital;
    /**
     * 企业状态
     */
    @ApiModelProperty(value = "企业状态")
    private String enterpriseState;
    /**
     * 是否为股东（合伙人、所有人）
     */
    @ApiModelProperty(value = "是否为股东（合伙人、所有人）")
    private String shareholder;
    /**
     * 个人认缴出资额或个人出资额（人民币万元
     */
    @ApiModelProperty(value = "个人认缴出资额或个人出资额（人民币万元")
    private String personalCapital;
    /**
     * 个人认缴出资比例或个人出资比例（%）
     */
    @ApiModelProperty(value = "个人认缴出资比例或个人出资比例（%）")
    private String personalRatio;
    /**
     * 投资时间
     */
    @ApiModelProperty(value = "投资时间")
    private String investTime;
    /**
     * 是否担任高级职务
     */
    @ApiModelProperty(value = "是否担任高级职务")
    private String seniorPosition;
    /**
     * 担任高级职务名称
     */
    @ApiModelProperty(value = "担任高级职务名称")
    private String seniorPositionName;
    /**
     * 担任高级职务的开始时间
     */
    @ApiModelProperty(value = "担任高级职务的开始时间")
    private String seniorPositionStartTime;
    /**
     * 担任高级职务的结束时间
     */
    @ApiModelProperty(value = "担任高级职务的结束时间")
    private String seniorPositionEndTime;
    /**
     * 该企业或其他市场主体是否与报告人所在单位
     */
    @ApiModelProperty(value = "该企业或其他市场主体是否与报告人所在单位")
    private String isRelation;
    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remarks;
    /**
     * 填报类型
     */
    @ApiModelProperty(value = "填报类型")
    private String tbType;
    /**
     * 年度
     */
    @ApiModelProperty(value = "年度")
    private String year;
    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private String state;
    /**
     * 有无此类情况
     */
    @ApiModelProperty(value = "有无此类情况")
    @NotBlank(message = "有无此类情况不能为空")
    private String isSituation;
    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String creatorId;
    /**
     * 修改人
     */
    @ApiModelProperty(value = "修改人")
    private String updaterId;

    @ApiModelProperty(value = "家属证件类型")
    private String familyCardType;

    @ApiModelProperty(value = "家属证件号")
    private String familyCardId;
}
