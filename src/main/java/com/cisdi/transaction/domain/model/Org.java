package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @Author: cxh
 * @Description:  组织机构
 * @Date: 2022/8/1 17:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)//链式编程，可以连续调用set方法
@TableName("69654103_org")
public class Org {
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
     * 创建人
     */
    private String creatorId;

    /**
     * 修改人
     */
    private String updaterId;

    /**
     * id
     */
   // private String asgId;


    /**
     * 根节点ID 页面不显示
     */
   // private String asgObjId;

    /**
     * 层级
     */
    private String asglevel;


    /**
     * 层级码
     */
    private String asgpathcode;

    /**
     * 组织名称链
     */
    private String asgpathname;

    /**
     * 组织编码链
     */
    private String asgpathnamecode;

    /**
     * 组织编码
     */
    private String asgorgancode;

    /**
     * 组织名称
     */
    private String asgorganname;

    /**
     * 同步日期
     */
    private String asgDate;

    /**
     * 是否分管
     */
    private String asgleadfg;

    /**
     * 是否领导班子
     */
    private String asglead;

    /**
     * 组织类型
     */
    private String asgtype;

    /**
     * 组织名称链
     */
    private String asgpathnameall;


    /**
     * 同步时间
     */
    //private Date asgCrtdate;

    /**
     * 组织简称
     */
    private String asgorgannameshort;

    /**
     * 负责人
     */
    private String asgqyfzr;

    /**
     * 经营类型
     */
    private String asgzzzlxbm;

    /**
     * 经营状态
     */
    private String asgjzyzt;


}
