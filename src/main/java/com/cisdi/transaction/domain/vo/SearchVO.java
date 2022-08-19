package com.cisdi.transaction.domain.vo;

import lombok.Data;

import java.util.List;

/**搜搜实体
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/19 9:40
 */
@Data
public class SearchVO {

    private List<String> keywords;

    private String orgCode;
}
