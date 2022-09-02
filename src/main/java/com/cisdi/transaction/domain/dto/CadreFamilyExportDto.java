package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/9 8:45
 */
@Data
@ApiModel(description = "干部家属导出条件")
public class CadreFamilyExportDto extends BaseDTO{

    @ApiModelProperty(value = "id集合")
//    @NotEmpty(message = "导出不能为空")
    private List<String> ids;

    @ApiModelProperty(value = "干部信息名字")
    private String name;

    @ApiModelProperty(value = "干部信息单位")
    private String unit;

    @ApiModelProperty(value = "干部信息职务类型")
    private List<String> post_type;

    @ApiModelProperty(value = "家人信息干部姓名")
    private String cadre_name;

    @ApiModelProperty(value = "投资企业或者担任高级职务的情况干部姓名")
    private String gb_name;

    @ApiModelProperty(value = "投资企业或者担任高级职务的情况工作单位")
    private String company;

    @ApiModelProperty(value = "投资企业或者担任高级职务的情况状态")
    private String state;

    @ApiModelProperty(value = "组织名称")
    private String asgorganname;

    @ApiModelProperty(value = "组织编码")
    private String asgorgancode;

    @ApiModelProperty(value = "是否分管")
    private String asgleadfg;

    @ApiModelProperty(value = "是否领导班子")
    private String asglead;

    @ApiModelProperty(value = "排序列")
    private String columnName;

    @ApiModelProperty(value = "是否升序")
    private Boolean isAsc;

    @ApiModelProperty(value = "创建账户")
    private List<String> create_account;

    @ApiModelProperty(value = "管理单位")
    private List<String> manage_company_code;

    public Boolean getIsAsc(){
        return Objects.isNull(this.isAsc)?false:this.isAsc;
    }





}
