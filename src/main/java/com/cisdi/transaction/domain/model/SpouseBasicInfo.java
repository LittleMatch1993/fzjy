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
 * @version 1.0  配偶，子女及其配偶基本信息
 * @date 2022/8/3 14:02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("69654103_spouse_basic_info")
public class SpouseBasicInfo {

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
    private String cadreName;
    /**
     * 干部身份证号
     */
    private String cadreCardId;
    /**
     * 姓名
     */
    private String name;
    /**
     * 称谓
     */
    private String title;
    /**
     * 证件名字
     */
    private String cardName;
    /**
     * 证件号码
     */
    private String cardId;
    /**
     * 创建人
     */
    private String creatorId;
    /**
     * 修改人
     */
    private String updaterId;

    /**
     * 管理数据id
     */
    private String refId;
}
