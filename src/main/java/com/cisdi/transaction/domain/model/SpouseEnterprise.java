package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author tgl
 * @version 1.0 配偶子女关联企业
 * @date 2022/9/1 11:09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("69654103_spouse_enterprise")
public class SpouseEnterprise {
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
     * 创建人
     */
    private String creatorId;
    /**
     * 修改人
     */
    private String updaterId;

    /**
     * 配偶子女id
     */
    private String spouseId;

    /**
     * 13-1、13-2、13-3的主键
     */
    private String enterpriseId;

    /**
     * 关联类型：1、13-1,2、13-2、3、13-3
     */
    private String type;
}
