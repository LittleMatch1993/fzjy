package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/4 13:34
 */
@Data
@ApiModel(description = " 配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况")
public class MechanismInfoDTO extends BaseDTO{

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
    private String gbName;
    /**
     * 身份证号
     */
    @ApiModelProperty(value = "身份证号")
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
     * 干部类职务类型
     */
    @ApiModelProperty(value = "干部职务类型")
    private String postType;
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
     * 执业资格名称
     */
    @ApiModelProperty(value = "执业资格名称")
    private String qualificationName;
    /**
     * 执业证号
     */
    @ApiModelProperty(value = "执业证号")
    private String qualificationCode;
    /**
     * 开办或所在机构名称
     */
    @ApiModelProperty(value = "开办或所在机构名称")
    private String organizationName;
    /**
     * 统一社会信用代码
     */
    @ApiModelProperty(value = "统一社会信用代码")
    private String code;
    /**
     * 经营范围
     */
    @ApiModelProperty(value = "经营范围")
    private String operatScope;
    /**
     * 成立日期
     */
    @ApiModelProperty(value = "成立日期")
    private String establishTime;

    @ApiModelProperty(value = "注册地（国家）")
    private String registerCountry;

    @ApiModelProperty(value = "注册地（省）")
    private String registerProvince;

    @ApiModelProperty(value = "注册地（市）")
    private String city;
    /**
     * 经营（服务）地
     */
    @ApiModelProperty(value = "经营（服务）地")
    private String operatAddr;
    /**
     * 机构类型
     */
    @ApiModelProperty(value = " 机构类型")
    private String organizationType;
    /**
     * 注册资本（金）或资金数额（出资额）（人民币万元）
     */
    @ApiModelProperty(value = "注册资本（金）或资金数额（出资额）（人民币万元）")
    private String registerCapital;
    /**
     * 经营状态
     */
    @ApiModelProperty(value = "经营状态")
    private String operatState;
    /**
     * 是否为机构股东（合伙人、所有人等）
     */
    @ApiModelProperty(value = "是否为机构股东（合伙人、所有人等）")
    private String shareholder;
    /**
     * 个人认缴出资额或个人出资额（人民币万元）
     */
    @ApiModelProperty(value = "个人认缴出资额或个人出资额（人民币万元）")
    private String personalCapital;
    /**
     * 个人认缴出资比例或个人出资比例（%）
     */
    @ApiModelProperty(value = "个人认缴出资比例或个人出资比例（%）")
    private String personalRatio;
    /**
     * 入股（合伙）时间
     */
    @ApiModelProperty(value = "入股（合伙）时间")
    private String joinTime;
    /**
     * 是否在该机构中从业
     */
    @ApiModelProperty(value = "是否在该机构中从业")
    private String practice;
    /**
     * 所担任的职务名称
     */
    @ApiModelProperty(value = "所担任的职务名称")
    private String postName;
    /**
     * 入职时间
     */
    @ApiModelProperty(value = "入职时间")
    private String inductionTime;
    /**
     * 是否与报告人所在单位（系统）直接发生过经济关系
     */
    @ApiModelProperty(value = "是否与报告人所在单位（系统）直接发生过经济关系")
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
