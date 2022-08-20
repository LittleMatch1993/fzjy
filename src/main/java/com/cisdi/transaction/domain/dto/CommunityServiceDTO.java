package com.cisdi.transaction.domain.dto;

//import cn.afterturn.easypoi.excel.annotation.Excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.cisdi.transaction.config.excel.DateStringValid;
import com.cisdi.transaction.config.excel.ExcelValid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author yuw
 * @version 1.0  开办有偿社会中介和法律服务机构或者从业的情况
 * @date 2022/8/4 16:23
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CommunityServiceDTO {
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

    @ExcelProperty(value = "家人姓名")
    @ExcelValid(message = "家人姓名为空")
    private String name;

    @ExcelProperty(value = "称谓")
    @ExcelValid(message = "称谓为空")
    private String title;

    @ExcelProperty(value = "执业资格名称")
    @ExcelValid(message = "执业资格名称为空")
    private String qualificationName;

    @ExcelProperty(value = "执业证号")
    @ExcelValid(message = "执业证号为空")
    private String qualificationCode;

    @ExcelProperty(value = "开办或所在机构名称")
    @ExcelValid(message = "开办或所在机构名称为空")
    private String organizationName;

    @ExcelProperty(value = "统一社会信用代码/注册号")
    @ExcelValid(message = "统一社会信用代码/注册号为空")
    private String code;

    @ExcelProperty(value = "经营（服务）范围")
    @ExcelValid(message = "经营（服务）范围为空")
    private String operatScope;

    @ExcelProperty(value = "成立日期")
    @DateStringValid(message = "成立日期格式不正确")
    private String establishTime;

    @ExcelProperty(value = "注册地（国家）")
    @ExcelValid(message = "注册地（国家）为空")
    private String registerCountry;

    @ExcelProperty(value = "注册地（省）")
    @ExcelValid(message = "注册地（省）为空")
    private String registerProvince;

    @ExcelProperty(value = "注册地（市）")
    private String city;

    @ExcelProperty(value = "经营（服务）地")
    private String operatAddr;

    @ExcelProperty(value = "机构类型")
    @ExcelValid(message = "机构类型为空")
    private String organizationType;

    @ExcelProperty(value = "注册资本（金）或资金数额（出资额）（人民币万元）")
    @ExcelValid(message = "注册资本（金）或资金数额（出资额）（人民币万元）为空")
    private String registerCapital;

    @ExcelProperty(value = "经营状态")
    @ExcelValid(message = "经营状态为空")
    private String operatState;

    @ExcelProperty(value = "是否为机构股东（合伙人、所有人等）")
    @ExcelValid(message = "是否为机构股东（合伙人、所有人等）为空")
    private String shareholder;

    @ExcelProperty(value = "个人认缴出资额或个人出资额（人民币万元）")
    private String personalCapital;

    @ExcelProperty(value = "个人认缴出资比例或个人出资比例（%）")
    private String personalRatio;

    @ExcelProperty(value = "入股（合伙）时间")
    @DateStringValid(message = "入股（合伙）时间格式不正确")
    private String joinTime;

    @ExcelProperty(value = "是否在该机构中从业")
    @ExcelValid(message = "是否在该机构中从业为空")
    private String practice;

    @ExcelProperty(value = "所担任的职务名称")
    private String postName;

    @ExcelProperty(value = "入职时间")
    @DateStringValid(message = "入职时间格式不正确")
    private String inductionTime;

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
