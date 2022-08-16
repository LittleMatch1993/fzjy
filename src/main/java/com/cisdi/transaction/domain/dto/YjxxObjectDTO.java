package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.List;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 10:13
 */
@Data
@ToString
@ApiModel(description = "预警信息接收实体")
public class YjxxObjectDTO extends BaseDTO {

    private List<YjxxDTO> yjxxDTOS;

}
