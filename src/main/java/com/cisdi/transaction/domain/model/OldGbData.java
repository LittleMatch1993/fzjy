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
 * @Description:
 * @Date: 2022/8/21 16:59
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)//链式编程，可以连续调用set方法
@TableName("old_basic_info") //这个表需要手动创建，可以直接复制 69654103_gb_basic_info表
public class OldGbData {

        /**
         * id
         */
        @TableId(value = "id",type = IdType.ASSIGN_UUID)
        private String id;

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

}
