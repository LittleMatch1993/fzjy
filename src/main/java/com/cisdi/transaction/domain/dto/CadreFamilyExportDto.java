package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/9 8:45
 */
@Data
@ApiModel(description = "干部家属导出条件")
public class CadreFamilyExportDto extends BaseDTO{

//    @ApiModelProperty(value = "id集合")
//    @NotEmpty(message = "导出不能为空")
//    private List<String> ids;

    @ApiModelProperty(value = "名字")
    private String name;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "职务类型")
    private String post;

}
