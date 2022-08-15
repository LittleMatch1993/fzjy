package com.cisdi.transaction.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.ExportExcelUtils;
import com.cisdi.transaction.config.utils.MinIoUtil;
import com.cisdi.transaction.domain.dto.CadreDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.vo.CadreExcelVO;
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
            List<CadreExcelVO> list=gbBasicInfoService.export(dto.getIds());
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
