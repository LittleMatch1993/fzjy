package com.cisdi.transaction.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.ExportExcelUtils;
import com.cisdi.transaction.config.utils.MinIoUtil;
import com.cisdi.transaction.domain.dto.CadreDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.InstitutionalFrameworkDTO;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.vo.CadreExcelVO;
import com.cisdi.transaction.domain.vo.KVVO;
import com.cisdi.transaction.service.GbBasicInfoService;
import com.cisdi.transaction.service.SpouseBasicInfoService;
import com.cisdi.transaction.util.ThreadLocalUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 9:50
 */
@Slf4j
@RestController
@RequestMapping("/cadre/manager")
@Api(tags = "干部管理")
@Validated
public class CadreController{
    @Autowired
    private GbBasicInfoService gbBasicInfoService;

    @Autowired
    private SpouseBasicInfoService spouseBasicInfoService;
    @Autowired
    private MinIoUtil minIoUtil;

    @Value("${minio.bucketName}")
    private String bucketName;


    @ApiOperation("更新干部信息")
    @PostMapping("/syncGbInfo")
    public ResultMsgUtil<Object> syncGbInfo() {
        try {
            gbBasicInfoService.syncData();
        }catch (Exception e){
            ResultMsgUtil.failure("数据同步失败，请稍后再试");
        }
        return ResultMsgUtil.success();
    }
    /**
     * 根据干部姓名获取信息
     *
     * @param id 干部id
     * @return
     */
    @ApiOperation("根据干部id获取信息")
    @GetMapping("/selectGbInfoById")
    public ResultMsgUtil<Object> selectGbInfoById(@ApiParam(value = "干部id") @RequestParam(value = "id" ,required = true) String id,
                                                  @RequestParam(value = "orgCode" ,required = false) String orgCode) {
        System.out.println("orgCode------------"+orgCode);
        if(StrUtil.isEmpty(id)){
            return ResultMsgUtil.failure("干部id为空");
        }
        GbBasicInfo gbBasicInfoServiceById = gbBasicInfoService.getById(id);
        return ResultMsgUtil.success(gbBasicInfoServiceById);
    }


    /**
     * 根据干部姓名获取信息
     *
     * @param name 干部姓名
     * @return
     */
    @ApiOperation("根据干部姓名获取信息")
    @GetMapping("/selectGbInfoByName")
    public ResultMsgUtil<List> selectGbInfoByName(@ApiParam(value = "干部姓名") @RequestParam(value = "name" ,required = false) String name,
                                                  @RequestParam(value = "orgCode" ,required = false) String orgCode) {
        System.out.println("orgCode------------"+orgCode);
        List<GbBasicInfo> list = gbBasicInfoService.selectByName(name,orgCode);
        return ResultMsgUtil.success(list);
    }

    @ApiOperation("根据干部姓名，单位 职务 获取信息")
    @GetMapping("/selectGbInfoByNameAndUnitAndPost")
    public ResultMsgUtil<Object> selectGbInfoByNameAndUnitAndPost(@ApiParam(value = "干部姓名") @RequestParam(value = "name" ,required = false) String name,
                                                  @RequestParam(value = "unit" ,required = false) String unit,
                                                                @RequestParam(value = "post" ,required = false) String post,
                                                                  @RequestParam(value = "cardId" ,required = false) String cardId) {
        List<GbBasicInfo> gbBasicInfos = gbBasicInfoService.selectGbInfoByNameAndUnitAndPost(name, unit, post,cardId);
        if(CollectionUtil.isNotEmpty(gbBasicInfos)){
             GbBasicInfo gbBasicInfo = gbBasicInfos.get(0);
             return   ResultMsgUtil.success(gbBasicInfo);
        }
        return ResultMsgUtil.success(null);
    }


    /**
     * 根据干部姓名获取 干部字典类型数据，如：  id_name   name
     *
     * @param name 干部姓名
     * @return
     */
    @ApiOperation("根据干部姓名获取 干部字典类型数据")
    @GetMapping("/selectGbDivtVoByName")
    public ResultMsgUtil<List> selectGbDivtVoByName(@ApiParam(value = "干部姓名") @RequestParam(value = "name" ,required = false) String name,
                                                  @RequestParam(value = "orgCode" ,required = false) String orgCode) {
         System.out.println("orgCode------------"+orgCode);
         List<GbBasicInfo> list = gbBasicInfoService.selectGbDictVoByName(name,orgCode);
         List<KVVO> kvvoList = new ArrayList<>();
         if(CollectionUtil.isNotEmpty(list)){
             list.stream().forEach(e->{
                 KVVO kv = new KVVO();

                 String unit = e.getUnit();
                 if(StrUtil.isNotEmpty(unit)){
                     String id = e.getId();
                     String gbname = e.getName();
                     kv.setId(id+"_"+gbname);
                     kv.setName(gbname+"-"+unit);
                     kvvoList.add(kv);
                 }

             });
         }
        return ResultMsgUtil.success(kvvoList);
    }


    /**
     * 根据干部姓名获取家属信息
     *
     * @param cardId 干部身份证Id
     * @return
     */
    @ApiOperation("根据干部姓名获取家属信息")
    @GetMapping("/selectGbFamilyInfoByCardId")
    public ResultMsgUtil selectGbFamilyInfoByName(@ApiParam(value = "身份证Id") @RequestParam String cardId,
                                                  @ApiParam(value = "条数", required = false, defaultValue = "1") @RequestParam Integer pageSize,
                                                  @ApiParam(value = "页码", required = false, defaultValue = "10") @RequestParam Integer pageIndex) {
        Map<String, Object> map = spouseBasicInfoService.selectGbFamilyInfoByCardId(cardId, pageSize.intValue(), pageIndex.intValue());
        return ResultMsgUtil.success(map);
    }


    @ApiOperation("新增干部信息")
    @PostMapping("/saveInfo")
    public ResultMsgUtil<Object> saveInfo(@RequestBody @Valid CadreDTO dto) {
        gbBasicInfoService.saveInfo(dto);
        return ResultMsgUtil.success();
    }

    @ApiOperation("验证干部信息")
    @GetMapping ("/validGbInfo")
    public ResultMsgUtil<Object> saveInfo(String id, String cardId, HttpServletRequest httpServletRequest) {
        boolean b = false;
        if(StrUtil.isEmpty(id)){//新增验证
            List<String> ids = new ArrayList<>();
            ids.add(cardId);
            List<GbBasicInfo> list = gbBasicInfoService.selectBatchByCardIds(ids);
            if(CollectionUtil.isEmpty(list)){ //没有找到数据
                b = true;
            }
        }else{ //编辑验证
            List<String> ids = new ArrayList<>();
            ids.add(cardId);
            List<GbBasicInfo> list = gbBasicInfoService.selectBatchByCardIds(ids);
            long count = list.stream().filter(e->!e.getId().equals(id)).count();
            if(count==0){
                b = true;
            }
        }
        String tenantId = httpServletRequest.getHeader("tenantId");
        String ids = httpServletRequest.getHeader("strTenantIds");
        System.out.println("执行base.."+ids+"----"+tenantId);
         String orgCode = ThreadLocalUtils.get("orgCode");
        System.out.println("orgCode="+orgCode);
        // List<String> list = Arrays.asList(ids.split(","));
        if(b){
            return ResultMsgUtil.success(b);

        }else{
            return ResultMsgUtil.success("身份证号码重复",b);
        }
    }


    @ApiOperation("导出功能")
    @PostMapping("/cadreExport")
    public ResultMsgUtil<Object> cadreExport(@RequestBody @Valid CadreFamilyExportDto dto,
                                             HttpServletResponse response) {
        String url = null;
        try {
            String fileName = new String("干部信息".getBytes(), StandardCharsets.UTF_8);
            List<CadreExcelVO> list=gbBasicInfoService.export(dto);
            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, CadreExcelVO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
            return ResultMsgUtil.failure("导出Excel编码异常");
        } catch (Exception e) {
            log.error("文件处理异常", e);
            return ResultMsgUtil.failure("文件处理异常");
        }
        return ResultMsgUtil.success(url);
    }
}
