package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/5 9:21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("69654103_ban_deal_info")
public class PurchaseBanDealInfo {

    /**
     * id
     */
    private String id;
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
     * 创建时间
     */
    private Date createTime;
}
