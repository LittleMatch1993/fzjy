package com.cisdi.transaction.domain.exportparam;

import lombok.Data;

/**投资企业或担任高级职务情况导出参数
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/17 18:20
 */
@Data
public class InvestExportParam {

    private String gbName; //干部姓名

    private String company; //单位

    private String state; //单位

    private String orgCode; //组织编码
}
