package com.cisdi.transaction.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: tgl
 * @Description:
 * @Date: 2022/8/22 23:40
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgVo {
    String id;
    String name;
    Boolean haveChildren;
}
