package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author yuw
 * @version 1.0 配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况
 * @date 2022/8/3 11:39
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("69654103_mechanism_info")
public class MechanismInfo {
    /**
     * id
     */
    @TableId(value = "id",type = IdType.ASSIGN_UUID)
    private String id;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 修改时间
     */
    private Date updateTime;
    /**
     * 干部姓名
     */
    private String gbName;
    /**
     * 身份证号
     */
    private String cardId;
    /**
     * 工作单位
     */
    private String company;
    /**
     * 现任职务
     */
    private String post;
    /**
     * 干部类型
     */
    private String postType;
    /**
     * 职务层次
     */
    private String gradation;
    /**
     * 标签
     */
    private String label;
    /**
     * 职级
     */
    private String gbRank;
    /**
     * 人员类型
     */
    private String userType;
    /**
     * 政治面貌
     */
    private String face;
    /**
     * 在职状态
     */
    private String jobStatus;
    /**
     * 姓名
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED )
    private String name;
    /**
     * 称谓
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String title;
    /**
     * 执业资格名称
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String qualificationName;
    /**
     * 执业证号
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String qualificationCode;
    /**
     * 开办或所在机构名称
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String organizationName;
    /**
     * 统一社会信用代码
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String code;
    /**
     * 经营范围
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String operatScope;
    /**
     * 成立日期
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String establishTime;
    /**
     * 注册地（国家）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String registerCountry;
    /**
     * 注册地（省份）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String registerProvince;
    /**
     * 注册地（市）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String city;
    /**
     * 经营（服务）地
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String operatAddr;
    /**
     * 机构类型
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String organizationType;
    /**
     * 注册资本（金）或资金数额（出资额）（人民币万元）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String registerCapital;
    /**
     * 经营状态
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String operatState;
    /**
     * 是否为机构股东（合伙人、所有人等）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String shareholder;
    /**
     * 个人认缴出资额或个人出资额（人民币万元）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String personalCapital;
    /**
     * 个人认缴出资比例或个人出资比例（%）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String personalRatio;
    /**
     * 入股（合伙）时间
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String joinTime;
    /**
     * 是否在该机构中从业
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String practice;
    /**
     * 所担任的职务名称
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String postName;
    /**
     * 入职时间
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String inductionTime;
    /**
     * 是否与报告人所在单位（系统）直接发生过经济关系
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String isRelation;
    /**
     * 备注
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String remarks;
    /**
     * 填报类型
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String tbType;
    /**
     * 年度
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String year;
    /**
     * 状态
     */
    private String state;
    /**
     * 有无此类情况
     */
    private String isSituation;
    /**
     * 创建人
     */
    private String creatorId;
    /**
     * 修改人
     */
    private String updaterId;


    /**
     * 创建人姓名
     */
    private  String createName;

    /**
     * 创建人账号
     */
    private  String createAccount;

    /**
     * 组织名称
     */
    private  String orgName;

    /**
     * 组织代码
     */
    private  String orgCode;


    /**
     * 提示
     */
    private  String tips;

    /**
     * 家属证件类型
     */
    private String familyCardType;

    /**
     * 家属证件号
     */
    private String familyCardId;


}
