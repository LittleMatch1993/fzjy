package com.cisdi.transaction.controller;

import com.alibaba.fastjson.JSONObject;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.ExportExcelUtils;
import com.cisdi.transaction.config.utils.MinIoUtil;
import com.cisdi.transaction.constant.WarnCodeConstant;
import com.cisdi.transaction.domain.dto.BusinessTransactionDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.YjxxDTO;
import com.cisdi.transaction.domain.dto.YjxxObjectDTO;
import com.cisdi.transaction.domain.vo.BusinessTransactionExcelVO;
import com.cisdi.transaction.enums.WarnTypeEnum;
import com.cisdi.transaction.service.EnterpriseDealInfoService;
import com.cisdi.transaction.service.PurchaseWarningInformationService;
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
import java.util.*;
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
    private PurchaseWarningInformationService purchaseWarningInformationService;
    @Autowired
    private MinIoUtil minIoUtil;

    @Value("${minio.bucketName}")
    private String bucketName;


    @ApiOperation("新增企业交易信息")
    @PostMapping("/saveInfo")
    public ResultMsgUtil<Object> saveInfo(@RequestBody YjxxObjectDTO yjxxObjectDTO)   {

        List<YjxxDTO> yjxxDTOS = yjxxObjectDTO.getYjxxDTOS();
        if (CollectionUtils.isEmpty(yjxxDTOS)){
            return ResultMsgUtil.failure("预警信息不能为空");
        }
        purchaseWarningInformationService.saveInfo(yjxxObjectDTO);

        return ResultMsgUtil.success();
    }

    @ApiOperation("导出功能")
    @PostMapping("/businessTransactionExport")
    public ResultMsgUtil<Object> businessTransactionExport(@RequestBody @Valid CadreFamilyExportDto dto,
                                             HttpServletResponse response) {
        String url = null;
       try {
            String fileName = new String("企业交易信息".getBytes(), StandardCharsets.UTF_8);
            List<BusinessTransactionExcelVO> list=enterpriseDealInfoService.export(dto);
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
