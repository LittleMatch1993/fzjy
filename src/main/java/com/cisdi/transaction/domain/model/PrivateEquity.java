package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 13:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)//链式编程，可以连续调用set方法
@TableName("69654103_privateequity")
public class PrivateEquity {
    /**
     * id
     */
    @TableId(value = "id",type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 租户id
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
    private String  cardId;

//    /**
//     * 干部类型
//     */
//    private String postType;

    /**
     * 工作单位
     */
    private String  company;

    /**
     * 现任职务
     */
    private String  post;

    /**
     * 职务层次
     */
    private String  gradation;

    /**
     * 标签
     */
    private String  label;

    /**
     * 职级
     */
    private String  gbRank;

    /**
     * 人员类别
     */
    private String  userType;

    /**
     * 政治面貌
     */
    private String  face;

    /**
     * 在职状态
     */
    private String  jobStatus;

    /**
     * 姓名
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED )
    private String  name;

    /**
     * 称谓
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  title;

    /**
     * 投资的私募股权投资基金产品名称
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  privateequityName;

    /**
     * 编码
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  code;

    /**
     * 基金总实缴金额（人民币万元）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  money;

    /**
     * 个人实缴金额（人民币万元）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  personalMoney;

    /**
     * 基金投向
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  investDirection;

    /**
     * 基金合同签署日
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  contractTime;

    /**
     * 基金合同约定的到期日
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  contractExpireTime;

    /**
     * 私募股权投资基金管理人名称
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  manager;

    /**
     * 登记编号
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  registrationNumber;

    /**
     * 是否为该基金管理人的实际控制人
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  controller;

    /**
     * 是否为该基金管理人的股东（合伙人）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  shareholder;

    /**
     * 认缴金额（人民币万元）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  subscriptionMoney;

    /**
     * 认缴比例（%）
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  subscriptionRatio;

    /**
     * 认缴时间
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  subscriptionTime;

    /**
     * 是否担任该基金管理人高级职
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  practice;

    /**
     * 所担任的高级职务名称
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  postName;

    /**
     * 担任高级职务的开始时间
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  inductionStartTime;

    /**
     * 担任高级职务的结束时间
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  inductionEndTime;

    /**
     *基金管理人的经营范围
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  managerOperatScope;

    /**
     * 是否与报告人所在单位（系统）直接发生过经济关系
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  isRelation;

    /**
     * 备注
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  remarks;

    /**
     * 填报类型
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  tbType;

    /**
     * 年度
     */
    @TableField(updateStrategy =FieldStrategy.IGNORED )
    private String  year;

    /**
     * 状态
     */
    private String  state;

    /**
     * 创建人
     */
    private String  creatorId;

    /**
     * 修改人
     */
    private String  updaterId;

    /**
     * 有无此类情况
     */
    private String  isSituation;

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
