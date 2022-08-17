package com.cisdi.transaction.domain.exportparam;

import lombok.Data;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/17 18:23
 */
@Data
public class OrgExportParam {

    private String asgorganname; //组织名称

    private String asgorgancode; //组织编码

    private String asgleadfg; //是否分管

    private String asglead; //是否领导班子

    private String orgCode; //组织编码
}
