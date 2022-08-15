package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/9 13:37
 */
@Data
@ApiModel(description = "数据提交")
public class SubmitDto extends  BaseDTO {
    @ApiModelProperty(value = "id集合")
    @NotBlank(message = "提交的数据不能为空")
    private List<String> ids;
    private String orgCode;
}
