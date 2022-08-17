package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: cxh
 * @Description:  干部基本信息
 * @Date: 2022/8/1 15:42
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)//链式编程，可以连续调用set方法
@TableName("69654103_gb_basic_info")
public class GbBasicInfo {

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
     * 姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String cardId;

    /**
     * 单位
     */
    private String unit;

    /**
     * 部门
     */
    private String department;

    /**
     * 职务
     */
    private String post;

    /**
     * 职务类型
     */
    private String postType;
    /**
     * 分配类型
     */
    private String allotType;
    /**
     * 创建人
     */
    private String creatorId;

    /**
     * 修改人
     */
    private String updaterId;


    /**
     * 组织编码
     */
    private  String orgCode;

    /**
     * 原职务类型
     */
    private  String oldPostType;




}
