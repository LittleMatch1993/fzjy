package com.cisdi.transaction.domain.exportparam;

import lombok.Data;

/**开办有偿社会中介和法律服务机构 导出参数
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/17 18:21
 */
@Data
public class MechanismExportParam {

    private String gbName; //干部姓名

    private String company; //单位

    private String state; //单位

    private String orgCode; //组织编码
}
