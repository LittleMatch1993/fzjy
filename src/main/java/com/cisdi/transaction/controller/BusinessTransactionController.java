package com.cisdi.transaction.controller;

import com.alibaba.fastjson.JSONObject;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.ExportExcelUtils;
import com.cisdi.transaction.config.utils.MinIoUtil;
import com.cisdi.transaction.constant.WarnCodeConstant;
import com.cisdi.transaction.domain.dto.BusinessTransactionDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.YjxxDTO;
import com.cisdi.transaction.domain.vo.BusinessTransactionExcelVO;
import com.cisdi.transaction.service.EnterpriseDealInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 9:55
 */
@Slf4j
@RestController
@RequestMapping("/business/transaction")
@Api(tags ="企业交易信息")
@Validated
public class BusinessTransactionController {
    @Autowired
    private EnterpriseDealInfoService enterpriseDealInfoService;
    @Autowired
    private MinIoUtil minIoUtil;

    @Value("${minio.bucketName}")
    private String bucketName;


    @ApiOperation("新增企业交易信息")
    @PostMapping("/saveInfo")
    public ResultMsgUtil<Object> saveInfo(@RequestBody List<YjxxDTO> yjxxDTOS)   {
        if (CollectionUtils.isEmpty(yjxxDTOS)){
            return ResultMsgUtil.failure("预警信息不能为空");
        }
        List<String> codes = Arrays.asList(WarnCodeConstant.JYJY01, WarnCodeConstant.JYJY06, WarnCodeConstant.JYJY07, WarnCodeConstant.JYJY08);
        List<YjxxDTO> yjxxDTOList = yjxxDTOS.stream().filter(yjxxDTO -> codes.contains(yjxxDTO.getYjlxid())).collect(Collectors.toList());
        List<BusinessTransactionDTO> dtos = yjxxDTOList.stream().map(e -> {
            String outputField = e.getOutputField();
            JSONObject yjxxObject = JSONObject.parseObject(outputField);
            BusinessTransactionDTO dto = new BusinessTransactionDTO();
            dto.setInfoType("禁止交易");
            dto.setUniqueCode(yjxxObject.getString("唯一标识码"));
            dto.setCode(yjxxObject.getString("社会统一信用代码"));
            dto.setPurchaseName(yjxxObject.getString("采购单位名称"));
            dto.setSupplier(yjxxObject.getString("禁止交易供应商名称"));
            dto.setBusinessCode(yjxxObject.getString("参与的采购业务编码"));
            dto.setBusinessName(yjxxObject.getString("参与的采购业务名称"));
            if (WarnCodeConstant.JYJY07.equals(e.getYjlxid())||WarnCodeConstant.JYJY08.equals(e.getYjlxid())){
                dto.setStraightPipeName(yjxxObject.getString("直管单位名称"));
                dto.setContractPrice(yjxxObject.getString("合同价格"));
                try {
                    dto.setContractTime(StringUtils.isBlank(yjxxObject.getString("签订日期")) ? null : new SimpleDateFormat("yyyy-MM-dd").parse(yjxxObject.getString("签订日期")));
                } catch (ParseException e1) {
                    log.error("日期格式错误",yjxxObject.getString("签订日期"));
                }
            }
            return dto;
        }).collect(Collectors.toList());
        enterpriseDealInfoService.saveList(dtos);
        return ResultMsgUtil.success();
    }

    @ApiOperation("导出功能")
    @PostMapping("/businessTransactionExport")
    public ResultMsgUtil<Object> businessTransactionExport(@RequestBody @Valid CadreFamilyExportDto dto,
                                             HttpServletResponse response) {
        String url = null;
        try {
            String fileName = new String("企业交易信息".getBytes(), StandardCharsets.UTF_8);
            List<BusinessTransactionExcelVO> list=enterpriseDealInfoService.export(dto.getIds());
            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, BusinessTransactionExcelVO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
        } catch (Exception e) {
            log.error("文件处理异常", e);
        }
        return ResultMsgUtil.success(url);
    }
}
