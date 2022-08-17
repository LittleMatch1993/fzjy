package com.cisdi.transaction.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: tgl
 * @Description:
 * @Date: 2022/8/15 16:48
 */
@Data
public class ExportReturnVO {
    private int successNumber=0;
    private int failNumber=0;

    private List<ExportReturnMessageVO> failMessage=new ArrayList<>();
}
