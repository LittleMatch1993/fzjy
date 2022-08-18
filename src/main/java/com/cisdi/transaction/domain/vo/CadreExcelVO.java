package com.cisdi.transaction.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.cisdi.transaction.config.excel.ExcelValid;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yuw
 * @version 1.0  导出干部信息
 * @date 2022/8/9 10:17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CadreExcelVO {
    @ExcelProperty(value = "干部姓名")
    private String name;

    @ExcelProperty(value = "身份证号")
    private String cardId;

    @ExcelProperty(value = "单位")
    private String unit;

    @ExcelProperty(value = "部门")
    private String department;

    @ExcelProperty(value = "职务")
    private String post;

    @ExcelProperty(value = "职务类型")
    private String postType;

    @ExcelProperty(value = "原职务类型")
    private String oldPostType;

    @ExcelProperty(value = "分配类型")
    private String allotType;



}
