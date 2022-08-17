package com.cisdi.transaction.domain.exportparam;

import lombok.Data;

/**禁止交易信息导出参数
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/17 18:22
 */
@Data
public class BanDealExportParam {

    private String name; //姓名

    private String unit;//单位

    private String postType; //职务类型

    private String orgCode; //组织编码
}
