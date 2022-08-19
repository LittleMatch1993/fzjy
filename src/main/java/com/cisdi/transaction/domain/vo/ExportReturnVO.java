package com.cisdi.transaction.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: tgl
 * @Description:
 * @Date: 2022/8/15 16:48
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ExportReturnVO {
    @ExcelProperty(value = "成功数量")
    private int successNumber=0;
    @ExcelProperty(value = "失败数量")
    private int failNumber=0;
    @ExcelProperty(value = "返回内容")
    private String message;

    private List<ExportReturnMessageVO> failMessage=new ArrayList<>();

    public void addMessage(){
        if (CollectionUtils.isEmpty(failMessage)){
            this.message="无失败内容";
        }else {
            this.message=failMessage.toString();
        }
    }
}