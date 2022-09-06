package com.cisdi.transaction.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author yuw
 * @version 1.0  禁止交易导出
 * @date 2022/8/9 11:23
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProhibitTransactionExcelVO {

    @ExcelProperty(value = "身份证号")
    private String cardId;

    @ExcelProperty(value = "姓名")
    private String name;

    @ExcelProperty(value = "单位")
    private String company;

    @ExcelProperty(value = "职务")
    private String post;

    @ExcelProperty(value = "职务类型")
    private String postType;

    @ExcelProperty(value = "禁止职务类型")
    private String banPostType;

    @ExcelProperty(value = "家人姓名")
    private String familyName;

    @ExcelProperty(value = "家人关系")
    private String relation;

    @ExcelProperty(value = "企业状态/经营状态")
    private String enterpriseState;

    @ExcelProperty(value = "经商类型")
    private String engageType;

    @ExcelProperty(value = "经商类型详细描述")
    private String engageInfo;

    @ExcelProperty(value = "经营范围")
    private String operatScope;

    @ExcelProperty(value = "数据校验提示")
    private String checkTips;

    @ExcelProperty(value = "供应商名称")
    private String supplier;

    @ExcelProperty(value = "统一社会信用代码")
    private String code;

    @ExcelProperty(value = "禁止交易采购单位代码")
    private String banPurchaseCode;

    @ExcelProperty(value = "禁止交易采购单位名称")
    private String banPurchaseName;

    @ExcelProperty(value = "是否继承关系")
    private String isExtends;

    @ExcelProperty(value = "管理单位名称")
    private String manageCompany;

    @ExcelProperty(value = "管理单位代码")
    private String manageCompanyCode;

    @ExcelProperty(value = "创建人")
    private String creator;

    @ExcelProperty(value = "创建人账号")
    private String creatorAccount;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "状态")
    private String state;

}
