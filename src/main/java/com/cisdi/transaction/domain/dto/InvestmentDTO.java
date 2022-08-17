package com.cisdi.transaction.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.cisdi.transaction.config.excel.ExcelValid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;


/**
 * @author yuw
 * @version 1.0 家属投资企业或者担任高级职务导入模型
 * @date 2022/8/4 16:11
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InvestmentDTO {
    @ExcelProperty(value = "干部姓名")
    @ColumnWidth(15)
    @ExcelValid(message = "干部姓名为空")
    private String gbName;

    @ExcelProperty(value = "身份证号")
    @ColumnWidth(8)
    @ExcelValid(message = "身份证号为空")
    private String cardId;

    @ExcelProperty(value = "工作单位")
    @ColumnWidth(8)
    @ExcelValid(message = "工作单位为空")
    private String company;

    @ExcelProperty(value = "现任职务")
    @ColumnWidth(8)
    @ExcelValid(message = "现任职务为空")
    private String post;

    @ExcelProperty(value = "职务层次")
    @ExcelValid(message = "职务层次为空")
    private String gradation;

    @ExcelProperty(value = "标签")
    private String label;

    @ExcelProperty(value = "职级")
    @ExcelValid(message = "职级为空")
    private String gbRank;

    @ExcelProperty(value = "人员类别")
    @ExcelValid(message = "人员类别为空")
    private String userType;

    @ExcelProperty(value = "政治面貌")
    @ExcelValid(message = "政治面貌为空")
    private String face;

    @ExcelProperty(value = "在职状态")
    @ExcelValid(message = "在职状态为空")
    private String jobStatus;

    @ExcelProperty(value = "姓名")
    private String name;

    @ExcelProperty(value = "称谓")
    private String title;

    @ExcelProperty(value = "统一社会信用代码/注册号")
    private String code;

    @ExcelProperty(value = "企业或其他市场主体名称")
    private String enterpriseName;

    @ExcelProperty(value = "成立日期")
    private String establishTime;

    @ExcelProperty(value = "经营范围")
    private String operatScope;

    @ExcelProperty(value = "注册地（国家）")
    private String registerCountry;

    @ExcelProperty(value = "注册地（省）")
    private String registerProvince;

    @ExcelProperty(value = "注册地（市）")
    private String city;

    @ExcelProperty(value = "经营地")
    private String operatAddr;

    @ExcelProperty(value = "企业或其他市场主体类型")
    private String enterpriseType;

    @ExcelProperty(value = "注册资本（金）或资金数额（出资额）（人民币万元）")
    private String registerCapital;

    @ExcelProperty(value = "企业状态")
    private String enterpriseState;

    @ExcelProperty(value = "是否为股东（合伙人、所有人）")
    private String shareholder;

    @ExcelProperty(value = "个人认缴出资额或个人出资额（人民币万元）")
    private String personalCapital;

    @ExcelProperty(value = "个人认缴出资比例或个人出资比例（%）")
    private String personalRatio;

    @ExcelProperty(value = "投资时间")
    private String investTime;

    @ExcelProperty(value = "是否担任高级职务")
    private String seniorPosition;

    @ExcelProperty(value = "所担任的高级职务名称")
    private String seniorPositionName;

    @ExcelProperty(value = "担任高级职务的开始时间")
    private String seniorPositionStartTime;

    @ExcelProperty(value = "担任高级职务的结束时间")
    private String seniorPositionEndTime;

    @ExcelProperty(value = "该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系")
    private String isRelation;

    @ExcelProperty(value = "备注")
    private String remarks;

    @ExcelProperty(value = "填报类型")
    private String tbType;

    @ExcelProperty(value = "年度")
    private String year;

//    @ExcelProperty(value = "有无此类情况")
//    @ExcelValid(message = "有无此类情况为空")
    private String isSituation;

    private Integer columnNumber;

    private String message;
}
