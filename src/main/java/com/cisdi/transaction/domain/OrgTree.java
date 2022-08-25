package com.cisdi.transaction.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/25 16:39
 */
@Data
public class OrgTree implements Serializable {
    private String id;

    private String name;

    private List<OrgTree> childSelect;


}
