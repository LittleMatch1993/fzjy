package com.cisdi.transaction.domain.exportparam;

import lombok.Data;

/**投资私募股权投资基金或者担任高级职务 导出参数
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/17 18:21
 */
@Data
public class PrivateequityExportParam {

    private String gbName; //干部姓名

    private String company; //单位

    private String state; //单位

    private String orgCode; //组织编码
}
