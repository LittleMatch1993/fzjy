package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author yuw
 * @version 1.0   禁止交易信息表
 * @date 2022/8/3 10:44
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("69654103_ban_deal_info")
public class BanDealInfo {
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
     * 身份证号
     */
    private String cardId;
    /**
     * 姓名
     */
    private String name;
    /**
     * 单位
     */
    private String company;
    /**
     * 职务
     */
    private String post;
    /**
     * 职务类型
     */
    private String postType;
    /**
     * 家人姓名
     */
    private String familyName;
    /**
     * 家人关系
     */
    private String relation;
    /**
     * 经商类型
     */
    private String engageType;
    /**
     * 经商类型详细描述
     */
    private String engageInfo;
    /**
     * 经营范围
     */
    private String operatScope;
    /**
     * 数据校验提示
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED )
    private String checkTips;
    /**
     * 供应商名称
     */
    private String supplier;
    /**
     * 统一社会信用代码
     */
    private String code;
    /**
     * 禁止交易采购单位代码
     */
    private String banPurchaseCode;
    /**
     * 禁止交易采购单位名称
     */
    private String banPurchaseName;
    /**
     * 是否继承关系
     */
    private String isExtends;
    /**
     * 管理单位名称
     */
    private String manageCompany;
    /**
     * 管理单位代码
     */
    private String manageCompanyCode;
    /**
     * 创建人
     */
    private String creator;
    /**
     * 创建人账号
     */
    private String creatorAccount;
    /**
     * 状态
     */
    private String state;
    /**
     * 禁止职务类型
     */
    private String banPostType;
    /**
     * 创建人
     */
    private String creatorId;
    /**
     * 修改人
     */
    private String updaterId;

    /**
     * 关联数据id
     */
    private String refId;
}
