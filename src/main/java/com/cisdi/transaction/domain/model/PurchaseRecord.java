package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**推送采购数据记录
 * @Author: cxh
 * @Description:
 * @Date: 2022/9/9 9:28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("69654103_purchase_record")
public class PurchaseRecord {

    /**
     * id
     */
    @TableId(value = "id",type = IdType.ASSIGN_UUID)
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

    private  String refId;

    private Integer delFlag;
}
