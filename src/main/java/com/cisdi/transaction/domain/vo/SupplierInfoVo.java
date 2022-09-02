package com.cisdi.transaction.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**企业的名字 注册号及 社会同意信用代码
 * @Author: cxh
 * @Description:
 * @Date: 2022/9/1 14:52
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInfoVo {
    private String company; //企业名称

    private String creditNo; //社会统一信用代码

    private String regNo; //注册号
}
