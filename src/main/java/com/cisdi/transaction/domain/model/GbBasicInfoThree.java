package com.cisdi.transaction.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/7 23:13
 */
@Data
public class GbBasicInfoThree {

/*        *//**
         * id
         *//*
        @TableId(value = "id",type = IdType.ASSIGN_UUID)
        private String id;*/

       /* *//**
         * 租户id
         *//*
        private String tenantId;

        *//**
         * 创建时间
         *//*
        private Date createTime;

        *//**
         * 修改时间
         *//*
        private Date updateTime;
*/
        /**
         * 姓名
         */
        private String name;

        /**
         * 身份证号
         */
        private String cardId;

        /**
         * 部门
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
         * 原职务类型
         */
        private String oldPostType;
        /**
         * 创建人
         */
        private String creatorId;

        /**
         * 修改人
         */
        private String updaterId;

}
