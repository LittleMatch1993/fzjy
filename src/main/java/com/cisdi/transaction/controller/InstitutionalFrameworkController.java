package com.cisdi.transaction.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.ExportExcelUtils;
import com.cisdi.transaction.config.utils.MinIoUtil;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.domain.dto.BusinessTransactionDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.InstitutionalFrameworkDTO;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.domain.vo.*;
import com.cisdi.transaction.service.OrgService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 9:56
 */
@Slf4j
@RestController
@RequestMapping("/institutional/framework")
@Api(tags ="组织机构管理")
@Validated
public class InstitutionalFrameworkController {

    @Autowired
    private OrgService orgService;
    @Autowired
    private MinIoUtil minIoUtil;

    @Value("${minio.bucketName}")
    private String bucketName;


    @ApiOperation("更新组织信息")
    @PostMapping("/syncOrg")
    public ResultMsgUtil<Object> syncOrg() {
        try {
            //orgService.syncDa(null);
        }catch (Exception e){
            e.printStackTrace();
            return ResultMsgUtil.failure("数据同步失败，请稍后再试");
        }

        return ResultMsgUtil.success();
    }

    @ApiOperation("新增组织机构信息")
    @PostMapping("/saveInfo")
    public ResultMsgUtil<Object> saveInfo(@RequestBody @Valid InstitutionalFrameworkDTO dto) {
        int count = orgService.countByAncodeAndPathNamecode(dto.getAsgorgancode(),dto.getAsgpathnamecode());
        if(count>0){
            return ResultMsgUtil.failure("组织不能重复添加");
        }
        orgService.saveInfo(dto);
        return ResultMsgUtil.success();
    }

    @ApiOperation("编辑组织机构信息")
    @PostMapping("/editOrgInfo")
    public ResultMsgUtil<Object> editOrgInfo(@RequestBody  InstitutionalFrameworkDTO dto) {
         String id = dto.getId();
         if(StrUtil.isEmpty(id)){
             return ResultMsgUtil.failure("数据不存在");
         }
         orgService.editOrgInfo(dto);
        return ResultMsgUtil.success();
    }

    @ApiOperation("导出功能")
    @PostMapping("/institutionalFrameworkExport")
    public ResultMsgUtil<Object> institutionalFrameworkExport(@RequestBody @Valid CadreFamilyExportDto dto,
                                                           HttpServletResponse response) {
        String url = null;
        try {
            String fileName = new String("组织机构信息".getBytes(), StandardCharsets.UTF_8);
            List<InstitutionalFrameworkExcelVO> list=orgService.export(dto);
            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, InstitutionalFrameworkExcelVO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
        } catch (Exception e) {
            log.error("文件处理异常", e);
        }
        return ResultMsgUtil.success(url);
    }

    @ApiOperation("根据组织名获取单位信息")
    @GetMapping("/selectByName")
    public ResultMsgUtil<List> selectGbInfoByName(@ApiParam(value = "组织名") @RequestParam(value = "keyword" ,required = false) String keyword,
                                                  @RequestParam(value = "orgCode" ,required = false) String orgCode) {
        List<Org> list = orgService.selectUnitByName(keyword,orgCode);
        List<OrgDictVo> voList = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(list)){
            list.stream().forEach(e->{
                OrgDictVo vo = new OrgDictVo();
                vo.setId(e.getAsgorganname());
                vo.setName(e.getAsgorganname());
                voList.add(vo);
            });
        }
        return ResultMsgUtil.success(voList);
    }

    @ApiOperation("根据组织名和编码")
    @PostMapping("/selectCodeAndNameByName")
    public ResultMsgUtil<List> selectCodeAndNameByName(@RequestBody SearchVO search) {
        List<OrgDictVo> voList = new ArrayList<>();
        List<String> keywordList = search.getKeyword();
        String orgCode = search.getOrgCode();
        List<Org> list = orgService.selectByName(search);
        if(CollectionUtil.isNotEmpty(list)){
            list.stream().forEach(e->{
                OrgDictVo vo = new OrgDictVo();
                vo.setId(e.getAsgorganname());
                vo.setName(e.getAsgorgancode()+"-"+e.getAsgorganname());
                voList.add(vo);
            });
        }
        return ResultMsgUtil.success(voList);
    }

    @ApiOperation("根据编码查询组织机构树")
    @PostMapping("/selectOrgByOrgCode")
    public ResultMsgUtil<List> selectOrgByOrgCode(@RequestBody OrgConditionVO searchVO) {
        List<OrgVo> orgVos= orgService.selectOrgByOrgCode(searchVO);
        return ResultMsgUtil.success(orgVos);
    }

    @ApiOperation("根据编码查询组织链路码")
    @PostMapping("/selectAsgpathnamecodeByOrgCode")
    public ResultMsgUtil<String> selectAsgpathnamecodeByOrgCode(@RequestBody OrgConditionVO searchVO) {
        String asgpathnamecode= orgService.selectAsgpathnamecodeByOrgCode(searchVO);
        return ResultMsgUtil.success(asgpathnamecode);
    }
}
