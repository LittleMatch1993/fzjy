package com.cisdi.transaction.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.cisdi.transaction.config.excel.DateStringValid;
import com.cisdi.transaction.config.excel.ExcelValid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author yuw
 * @version 1.0  家属投资私募股权投资基金或者担任高级职务的情况
 * @date 2022/8/4 16:22
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EquityFundsDTO {

    @ExcelProperty(value = "干部姓名")
    @ExcelValid(message = "干部姓名为空")
    private String gbName;

    @ExcelProperty(value = "身份证号")
    @ExcelValid(message = "身份证号为空")
    private String cardId;

    @ExcelProperty(value = "工作单位")
//    @ExcelValid(message = "工作单位为空")
    private String company;

    @ExcelProperty(value = "现任职务")
//    @ExcelValid(message = "现任职务为空")
    private String post;

    @ExcelProperty(value = "职务层次")
//    @ExcelValid(message = "职务层次为空")
    private String gradation;

    @ExcelProperty(value = "标签")
    private String label;

    @ExcelProperty(value = "职级")
//    @ExcelValid(message = "职级为空")
    private String gbRank;

    @ExcelProperty(value = "人员类别")
//    @ExcelValid(message = "人员类别为空")
    private String userType;

    @ExcelProperty(value = "政治面貌")
//    @ExcelValid(message = "政治面貌为空")
    private String face;

    @ExcelProperty(value = "在职状态")
//    @ExcelValid(message = "在职状态为空")
    private String jobStatus;

    @ExcelProperty(value = "姓名")
    @ExcelValid(message = "姓名为空")
    private String name;

    @ExcelProperty(value = "称谓")
    @ExcelValid(message = "称谓为空")
    private String title;

    @ExcelProperty(value = "投资的私募股权投资基金产品名称")
    @ExcelValid(message = "投资的私募股权投资基金产品名称为空")
    private String privateequityName;

    @ExcelProperty(value = "编码")
    @ExcelValid(message = "编码为空")
    private String code;

    @ExcelProperty(value = "基金总实缴金额（人民币万元）")
    @ExcelValid(message = "基金总实缴金额为空")
    private String money;

    @ExcelProperty(value = "个人实缴金额（人民币万元）")
    @ExcelValid(message = "个人实缴金额为空")
    private String personalMoney;

    @ExcelProperty(value = "基金投向")
    @ExcelValid(message = "基金投向为空")
    private String investDirection;

    @ExcelProperty(value = "基金合同签署日")
    @ExcelValid(message = "基金合同签署日为空")
    @DateStringValid(message = "基金合同签署日格式不正确")
    private String contractTime;

    @ExcelProperty(value = "基金合同约定的到期日")
    @ExcelValid(message = "基金合同约定的到期日为空")
    @DateStringValid(message = "基金合同约定的到期日格式不正确")
    private String contractExpireTime;

    @ExcelProperty(value = "私募股权投资基金管理人名称")
    @ExcelValid(message = "私募股权投资基金管理人名称为空")
    private String manager;

    @ExcelProperty(value = "登记编号")
    @ExcelValid(message = "登记编号为空")
    private String registrationNumber;

    @ExcelProperty(value = "是否为该基金管理人的实际控制人")
    @ExcelValid(message = "是否为该基金管理人的实际控制人为空")
    private String controller;

    @ExcelProperty(value = "是否为该基金管理人的股东（合伙人）")
    @ExcelValid(message = "是否为该基金管理人的股东（合伙人）为空")
    private String shareholder;

    @ExcelProperty(value = "认缴金额（人民币万元）")
//    @ExcelValid(message = "认缴金额为空")
    private String subscriptionMoney;

    @ExcelProperty(value = "认缴比例（%）")
//    @ExcelValid(message = "认缴比例为空")
    private String subscriptionRatio;

    @ExcelProperty(value = "认缴时间")
//    @ExcelValid(message = "认缴时间为空")
    @DateStringValid(message = "认缴时间格式不正确")
    private String subscriptionTime;

    @ExcelProperty(value = "是否担任该基金管理人高级职务")
    @ExcelValid(message = "是否担任该基金管理人高级职务为空")
    private String practice;

    @ExcelProperty(value = "所担任的高级职务名称")
//    @ExcelValid(message = "所担任的高级职务名称为空")
    private String postName;

    @ExcelProperty(value = "担任高级职务的开始时间")
//    @ExcelValid(message = "担任高级职务的开始时间为空")
    @DateStringValid(message = "担任高级职务的开始时间时间格式不正确")
    private String inductionStartTime;

    @ExcelProperty(value = "担任高级职务的结束时间")
//    @ExcelValid(message = "担任高级职务的结束时间为空")
    @DateStringValid(message = "担任高级职务的结束时间格式不正确")
    private String inductionEndTime;

    @ExcelProperty(value = "基金管理人的经营范围")
//    @ExcelValid(message = "基金管理人的经营范围为空")
    private String managerOperatScope;

    @ExcelProperty(value = "是否与报告人所在单位（系统）直接发生过经济关系")
    @ExcelValid(message = "是否与报告人所在单位（系统）直接发生过经济关系为空")
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
