package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author yuw
 * @version 1.0 企业交易信息
 * @date 2022/8/3 11:09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("69654103_enterprise_deal_info")
public class EnterpriseDealInfo {
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
     * 禁业职务类型
     */
    private String banPostType;
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
     * 唯一标识码
     */
    private String uniqueCode;
    /**
     * 供应商名称
     */
    private String supplier;
    /**
     * 统一社会信用代码
     */
    private String code;
    /**
     * 采购单位名称
     */
    private String purchaseName;
    /**
     * 直管单位名称
     */
    private String straightPipeName;
    /**
     * 采购业务编码
     */
    private String businessCode;
    /**
     * 采购业务名称
     */
    private String businessName;
    /**
     * 信息类型
     */
    private String infoType;
    /**
     * 供应商报价
     */
    private String supplierPrice;
    /**
     * 是否为中标供应商
     */
    private String bidder;
    /**
     * 采购合同编码
     */
    private String contractCode;
    /**
     * 采购合同名称
     */
    private String contractName;
    /**
     * 采购合同金额
     */
    private String contractPrice;
    /**
     * 采购合同签订日期
     */
    private String contractTime;
    /**
     * 信息提示
     */
    private String infoTips;
    /**
     * 创建人
     */
    private String creatorId;
    /**
     * 修改人
     */
    private String updaterId;

    /**
     * 禁止交易采购单位代码
     */
    private String banPurchaseCode;
    /**
     * 禁止交易采购单位名称
     */
    private String banPurchaseName;
}
