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
    private List<ExportReturnMessageVO> failMessage=new ArrayList<>();
}
