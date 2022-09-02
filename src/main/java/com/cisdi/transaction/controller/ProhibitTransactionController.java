package com.cisdi.transaction.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cisdi.transaction.config.base.ResultCode;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.ExportExcelUtils;
import com.cisdi.transaction.config.utils.MinIoUtil;
import com.cisdi.transaction.domain.dto.BanDealInfoDTO;
import com.cisdi.transaction.domain.dto.SubmitDto;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.model.BanDealInfo;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.vo.InstitutionalFrameworkExcelVO;
import com.cisdi.transaction.domain.vo.ProhibitTransactionExcelVO;
import com.cisdi.transaction.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/1 15:49
 */
@Slf4j
@RestController
@RequestMapping("/prohibit/transaction")
@Api(tags = "禁止交易信息管理")
@Validated
public class ProhibitTransactionController{

    @Autowired
    private BanDealInfoService banDealInfoService;
    @Autowired
    private MinIoUtil minIoUtil;

    @Value("${minio.bucketName}")
    private String bucketName;


    @ApiOperation("提交禁止交易信息")
    @PostMapping("/submitBanDealInfo")
    public ResultMsgUtil<Object> submitBanDealInfo(
            @ApiParam(value = "id集合", required = true)
            @RequestBody SubmitDto ids) {
        ResultMsgUtil<Object> result = null;
        try {
            result = banDealInfoService.submitBanDealInfo(ids.getIds());
        }catch (Exception e){
            log.error("提交禁止交易信息异常",e);
            return ResultMsgUtil.failure("推送禁止交易信息失败");
        }
        return result;
    }

    @ApiOperation("新增禁止交易信息")
    @PostMapping("/saveBanDealInfo")
    public ResultMsgUtil<Object> saveBanDealInfo(@RequestBody @Valid BanDealInfoDTO infoDto) {
        System.out.println("新增禁止交易。。。");
        System.out.println("1"+infoDto.getServiceUserAccount());
        System.out.println("3"+infoDto.getServiceUserId());
        int count = banDealInfoService.countByCardIdAndNameAndCode(infoDto.getCardId(),infoDto.getFamilyName(),infoDto.getCode());
        if (count >0) {
            return ResultMsgUtil.failure("数据重复");
        }
        banDealInfoService.insertBanDealInfo(infoDto);
        return ResultMsgUtil.success();
    }

    @ApiOperation("删除禁止交易信息")
    @PostMapping("/delete")
    public ResultMsgUtil<Object> deleteBanDealInfo(
            @ApiParam(value = "id集合", required = true)
            @RequestBody SubmitDto ids) {
        banDealInfoService.deleteBanDealInfo(ids.getIds());
        return ResultMsgUtil.success();
    }

    @ApiOperation("编辑交易信息")
    @PostMapping("/editBanDealInfo")
    public ResultMsgUtil<Object> editBanDealInfo(@RequestBody @Valid BanDealInfoDTO infoDto) {
        if(StrUtil.isEmpty(infoDto.getId())){
            return ResultMsgUtil.failure("数据不存在");
        }
        System.out.println("开始禁止交易编辑数据");
        banDealInfoService.editBanDealInfo(infoDto);
        return ResultMsgUtil.success();
    }

    @ApiOperation("导出功能")
    @PostMapping("/prohibitTransactionExport")
    public ResultMsgUtil<Object> prohibitTransactionExport(@RequestBody @Valid CadreFamilyExportDto dto,
                                                              HttpServletResponse response) {
        String url = null;
        try {
            String fileName = new String("禁止交易信息".getBytes(), StandardCharsets.UTF_8);
            List<ProhibitTransactionExcelVO> list=banDealInfoService.export(dto);

            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, ProhibitTransactionExcelVO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
        } catch (Exception e) {
            log.error("文件处理异常", e);
        }
        return ResultMsgUtil.success(url);
    }

}
