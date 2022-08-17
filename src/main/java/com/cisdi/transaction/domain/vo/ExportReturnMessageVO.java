package com.cisdi.transaction.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/15 16:48
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportReturnMessageVO {
    private Integer column;
    private String message;
}
