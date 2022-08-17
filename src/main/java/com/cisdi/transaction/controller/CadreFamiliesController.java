package com.cisdi.transaction.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.cisdi.transaction.config.base.ResultCode;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.excel.listener.ImportCommunityServiceExcelListener;
import com.cisdi.transaction.config.excel.listener.ImportEquityFundsExcelListener;
import com.cisdi.transaction.config.excel.listener.ImportInvestmentExcelListener;
import com.cisdi.transaction.config.utils.ExportExcelUtils;
import com.cisdi.transaction.config.utils.MinIoUtil;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.InvestInfo;
import com.cisdi.transaction.domain.model.MechanismInfo;
import com.cisdi.transaction.domain.model.PrivateEquity;
import com.cisdi.transaction.domain.vo.*;
import com.cisdi.transaction.service.*;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.bag.SynchronizedSortedBag;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.FileItemFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 9:54
 */
@Slf4j
@RestController
@RequestMapping("/cadre/family")
@Api(tags = "干部家属管理")
@Validated
public class CadreFamiliesController {

    @Autowired
    private InvestInfoService investInfoService;

    @Autowired
    private MechanismInfoService mechanismInfoService;

    @Autowired
    private PrivateEquityService privateEquityService;
    @Autowired
    private SpouseBasicInfoService spouseBasicInfoService;
    @Autowired
    private GlobalCityInfoService globalCityInfoService;

    @Autowired
    private BanDealInfoService banDealInfoService;

    @Autowired
    private MinIoUtil minIoUtil;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 投资企业或担任高级职务情况 提交生成 禁止交易信息
     * @param ids
     * @return
     */
    @ApiOperation("投资企业或担任高级职务情况 提交生成 禁止交易信息")
    @PostMapping("/submitInvestInfo")
    @ResponseBody
    public ResultMsgUtil<String> submitInvestInfo(@ApiParam(value = "id集合", required = true)
                                                 @RequestBody SubmitDto ids) {
        //提交数据至禁止交易表
        ResultMsgUtil<String> result = null;
        try {
            result = investInfoService.submitInvestInfo(ids);
        }catch (Exception e){
            e.printStackTrace();
            return ResultMsgUtil.failure("提交失败,程序处理数据异常");
        }
        return result;
    }

    /**
     * 新增投资企业或担任高级职务情况
     *
     * @param infoDto
     * @return
     */
    @ApiOperation("新增投资企业或担任高级职务情况 ")
    @PostMapping("/saveInvestInfo")
    public ResultMsgUtil<String> saveInvestInfo(@RequestBody @Valid InvestInfoDTO infoDto) {

        System.out.println("Account="+infoDto.getServiceUserAccount());
        System.out.println("LesseeId="+infoDto.getServiceLesseeId());
        //int count = investInfoService.countByNameAndCardIdAndCode(infoDto.getName(), infoDto.getCardId(), infoDto.getCode());
        InvestInfo one = investInfoService.getRepeatInvestInfo(infoDto.getName(), infoDto.getCardId(), infoDto.getCode());
        if (Objects.nonNull(one)) {
            String id = one.getId();
            investInfoService.overrideInvestInfo(id,infoDto);
            //如果状态是有效的 还需要删除禁止交易信息表中数据
            if(SystemConstant.VALID_STATE.equals(one.getState())){
                List<String> ids = new ArrayList<>();
                ids.add(id);
                banDealInfoService.deleteBanDealInfoByRefId(ids);
            }
            return ResultMsgUtil.success("数据重复,已覆盖");
        }
        investInfoService.saveInvestInfo(infoDto);
        return ResultMsgUtil.success();
    }

    /**
     * 编辑投资企业或担任高级职务情况
     *
     * @param infoDto
     * @return
     */
    @ApiOperation("编辑投资企业或担任高级职务情况 ")
    @PostMapping("/editInvestInfo")
    public ResultMsgUtil<String> editInvestInfo(@RequestBody @Valid InvestInfoDTO infoDto) {

        String id = infoDto.getId();
        if (StrUtil.isEmpty(id)) {
            return ResultMsgUtil.failure("数据不存在");
        }
        investInfoService.editInvestInfo(infoDto);
        return ResultMsgUtil.success();
    }

    /**
     * 配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况 提交生成 禁止交易信息
     *
     * @param ids
     * @return
     */
    @ApiOperation("配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况 提交生成 禁止交易信息")
    @PostMapping("/submitPrivateEquity")
    public ResultMsgUtil<String> submitPrivateEquity(@ApiParam(value = "id集合", required = true)
                                                     @RequestBody SubmitDto ids) {
        ResultMsgUtil<String> result = null;
        try {
            //向禁止交易表中提交数据
            result = privateEquityService.submitPrivateEquity(ids);
        }catch (Exception e){
            return ResultMsgUtil.success("提交失败,程序处理数据异常");
        }

        return result;
    }

    /**
     * 新增 配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况
     *
     * @param dto
     * @return
     */
    @ApiOperation("新增 配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况")
    @PostMapping("/savePrivateEquity")
    public ResultMsgUtil<String> savePrivateEquity(@RequestBody @Valid PrivateEquityDTO dto) {
        System.out.println("私募新增");
        System.out.println(dto.toString());
        //int count = privateEquityService.countByNameAndCardIdAndCode(dto.getName(), dto.getCardId(), dto.getCode());
        PrivateEquity one =  privateEquityService.getRepeatInvestInfo(dto.getName(), dto.getCardId(), dto.getRegistrationNumber());
        if (Objects.nonNull(one)) {
            String id = one.getId();
            privateEquityService.overrideInvestInfo(id,dto);
            //如果状态是有效的 还需要删除禁止交易信息表中数据
            if(SystemConstant.VALID_STATE.equals(one.getState())){
                List<String> ids = new ArrayList<>();
                ids.add(id);
                banDealInfoService.deleteBanDealInfoByRefId(ids);
            }
            return ResultMsgUtil.failure("数据重复,已覆盖");
        }
        privateEquityService.savePrivateEquity(dto);
        return ResultMsgUtil.success();
    }

    /**
     * 编辑 配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况
     *
     * @param dto
     * @return
     */
    @ApiOperation("编辑 配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况")
    @PostMapping("/editPrivateEquity")
    public ResultMsgUtil<String> editPrivateEquity(@RequestBody @Valid PrivateEquityDTO dto) {
        String id = dto.getId();
        if (StrUtil.isEmpty(id)) {
            return ResultMsgUtil.failure("数据不存在");
        }
        privateEquityService.editPrivateEquity(dto);
        return ResultMsgUtil.success();
    }

    /**
     * 配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况 提交生成 禁止交易信息
     *
     * @param ids
     * @return
     */
    @ApiOperation("配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况 提交生成 禁止交易信息")
    @PostMapping("/submitMechanismInfo")
    public ResultMsgUtil<String> submitMechanismInfo(@ApiParam(value = "id集合", required = true)
                                                     @RequestBody SubmitDto ids) {
        ResultMsgUtil<String> result = null;
        try {
            result = mechanismInfoService.submitMechanismInfo(ids);
        }catch (Exception e){
            return ResultMsgUtil.failure("提交失败,程序处理数据异常");
        }

        //提交数据至禁止交易表
        return result;
    }

    /**
     * 新增配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况
     *
     * @param dto
     * @return
     */
    @ApiOperation("新增配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况")
    @PostMapping("/saveMechanismInfo")//TODO 校验国家/省份/市
    public ResultMsgUtil<String> saveMechanismInfo(@RequestBody @Valid MechanismInfoDTO dto) {

        //int count = mechanismInfoService.countByNameAndCardIdAndCode(dto.getGbName(), dto.getCardId(), dto.getCode());
        MechanismInfo one = mechanismInfoService.getRepeatInvestInfo(dto.getName(), dto.getCardId(), dto.getCode());
        if (Objects.nonNull(one)) {
            String id = one.getId();
            mechanismInfoService.overrideInvestInfo(id,dto);
            //如果状态是有效的 还需要删除禁止交易信息表中数据
            if(SystemConstant.VALID_STATE.equals(one.getState())){
                List<String> ids = new ArrayList<>();
                ids.add(id);
                banDealInfoService.deleteBanDealInfoByRefId(ids);
            }
            return ResultMsgUtil.failure("数据重复,已覆盖");
        }
        mechanismInfoService.saveMechanismInfo(dto);
        return ResultMsgUtil.success();
    }

    /**
     * 修改配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况
     *
     * @param dto
     * @return
     */
    @ApiOperation("修改配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况")
    @PostMapping("/editMechanismInfo")
    public ResultMsgUtil<String> editMechanismInfo(@RequestBody @Valid MechanismInfoDTO dto) {
        String id = dto.getId();
        if (StrUtil.isEmpty(id)) {
            return ResultMsgUtil.failure("数据不存在");
        }
        mechanismInfoService.editMechanismInfo(dto);
        return ResultMsgUtil.success();
    }

    @ApiOperation("新增干部家属信息")
    @PostMapping("/saveInfo")
    public ResultMsgUtil<Object> saveInfo(@RequestBody @Valid CadreFamiliesDTO dto) {
        spouseBasicInfoService.saveInfo(dto);
        return ResultMsgUtil.success();
    }


    @ApiOperation("导出功能")
    @PostMapping("/cadreFamiliesExport")
    public ResultMsgUtil<Object> cadreFamiliesExport(@RequestBody @Valid CadreFamilyExportDto dto,
                                                     HttpServletResponse response) {
        String url = null;
        try {
            String fileName = new String("干部家属信息".getBytes(), StandardCharsets.UTF_8);
            List<CadreFamiliesExcelVO> list = spouseBasicInfoService.export(dto.getIds());
            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, CadreFamiliesExcelVO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
        } catch (Exception e) {
            log.error("文件处理异常", e);
        }
        return ResultMsgUtil.success(url);
    }

    @ApiOperation("家属投资企业或者担任高级职务导入功能")
    @PostMapping("/importInvestmentExcel")
    public ResultMsgUtil<Object> importInvestmentExcel(@RequestPart("file") MultipartFile file,BaseDTO baseDTO) {
        try {
            /**
             *  inputStream   文件流
             *  SubjectData.class   实体类
             *  new SubjectExcelListener(subjectService)   监听器(主要功能都在监听器里，比如读取添加等操作)
             *  SubjectExcelListener  没有交给Spring进行管理，所有不能在此方法使用    @Autowired 注入，所以使用  SubjectExcelListener(subjectService)   传入方式
             */
            ExportReturnVO exportReturnVO= new ExportReturnVO();
            EasyExcel.read(file.getInputStream(), InvestmentDTO.class, new ImportInvestmentExcelListener(investInfoService,baseDTO,exportReturnVO)).sheet().headRowNumber(3).doRead();
            return ResultMsgUtil.success(exportReturnVO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "导入失败"+e.getMessage());
        }
    }

    @ApiOperation("家属投资企业或者担任高级职务导出功能")
    @PostMapping("/exportInvestmentExcel")//TODO 目前导出功能是导出所有数据，需要条件直接加就是了
    public ResultMsgUtil<String> exportInvestmentExcel(@RequestBody @Valid CadreFamilyExportDto exportDto,
                                                       HttpServletResponse response) {
        System.out.println("导出......");
        String url = null;
        try {
            String fileName = new String("家属投资企业或者担任高级职务".getBytes(), StandardCharsets.UTF_8);
            List<InvestmentDTO> list = investInfoService.exportInvestmentExcel(exportDto.getIds());
            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, InvestmentDTO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
        } catch (Exception e) {
            log.error("文件处理异常", e);
        }
        return ResultMsgUtil.success(url);
    }


    @ApiOperation("开办有偿社会中介和法律服务机构或者从业的情况导入功能")
    @PostMapping(value = "/importCommunityServiceExcel")
    public ResultMsgUtil<Object> importCommunityServiceExcel(@RequestPart("file") MultipartFile file,BaseDTO baseDTO) {
        try {
            ExportReturnVO exportReturnVO= new ExportReturnVO();
            EasyExcel.read(file.getInputStream(), CommunityServiceDTO.class, new ImportCommunityServiceExcelListener(mechanismInfoService,baseDTO,exportReturnVO)).sheet().headRowNumber(3).doRead();
            return ResultMsgUtil.success(exportReturnVO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "导入失败"+e.getMessage());
        }
    }

    @ApiOperation("开办有偿社会中介和法律服务机构或者从业的情况导出功能")
    @PostMapping(value = "/exportCommunityServiceExcel")//TODO 目前导出功能是导出所有数据，需要条件直接加就是了
    public ResultMsgUtil<String> exportCommunityServiceExcel(@RequestBody @Valid CadreFamilyExportDto exportDto,
                                                             HttpServletResponse response) {
        System.out.println("导出....");
        String url = null;
        try {
            String fileName = new String("开办有偿社会中介和法律服务机构或者从业的情况".getBytes(), "UTF-8");
            List<CommunityServiceDTO> list = mechanismInfoService.exportCommunityServiceExcel(exportDto.getIds());
            // EasyExcel.write(response.getOutputStream(), CommunityServiceDTO.class).sheet("开办有偿社会中介和法律服务机构或者从业的情况").doWrite(list);
            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, CommunityServiceDTO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
        } catch (Exception e) {
            log.error("文件处理异常", e);
        }
        return ResultMsgUtil.success(url);
    }


    @ApiOperation("家属投资私募股权投资基金或者担任高级职务的情况导入功能")
    @PostMapping(value = "/importEquityFundsExcel")
    public ResultMsgUtil<Object> importEquityFundsExcel(@RequestPart("file") MultipartFile file,BaseDTO baseDTO) {
        try {
            ExportReturnVO exportReturnVO= new ExportReturnVO();
            EasyExcel.read(file.getInputStream(), EquityFundsDTO.class, new ImportEquityFundsExcelListener(privateEquityService,baseDTO,exportReturnVO)).sheet().headRowNumber(3).doRead();
            return ResultMsgUtil.success(exportReturnVO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "导入失败"+e.getMessage());
        }
    }

    @ApiOperation("家属投资私募股权投资基金或者担任高级职务的情况导出功能")
    @PostMapping(value = "/exportEquityFundsExcel")//TODO 目前导出功能是导出所有数据，需要条件直接加就是了
    public ResultMsgUtil<String> exportEquityFundsExcel(@RequestBody @Valid CadreFamilyExportDto exportDto,
                                                        HttpServletResponse response) {
        System.out.println("导出....");
        String url = null;
        try {
            String fileName = new String("家属投资私募股权投资基金或者担任高级职务的情况".getBytes(), "UTF-8");
            List<EquityFundsDTO> list = privateEquityService.exportEquityFundsExcel(exportDto.getIds());
            MultipartFile multipartFile = ExportExcelUtils.exportExcel(response, fileName, EquityFundsDTO.class, list);
            url = minIoUtil.downloadByMinio(multipartFile, bucketName, null);
            //EasyExcel.write(response.getOutputStream(), EquityFundsDTO.class).sheet("家属投资私募股权投资基金或者担任高级职务的情况").doWrite(list);
        } catch (UnsupportedEncodingException e) {
            log.error("导出Excel编码异常", e);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("文件处理异常", e);
        }
        return ResultMsgUtil.success(url);
    }
    @ApiOperation("国家/省/市下拉框")
    @GetMapping("/regionDropDownBox")
    public ResultMsgUtil<Object> regionDropDownBox() {
       List<RegionDropDownBoxVO>  list=globalCityInfoService.regionDropDownBox();
        return ResultMsgUtil.success(list);
    }

    @ApiOperation("国家下拉框")
    @GetMapping("/getCountryDropDownBox")
    public ResultMsgUtil<Object> getCountryDropDownBox() {
        List<RegionDropDownBoxVO>  list=globalCityInfoService.getCountryDropDownBox();
        return ResultMsgUtil.success(list);
    }

    @ApiOperation("省份/市下拉框")
    @GetMapping("/getProvinceDropDownBox")
    public ResultMsgUtil<Object> getProvinceDropDownBox(@ApiParam(value = "国家/省份id",required = true) @RequestParam String countryId) {
        List<RegionDropDownBoxVO>  list=globalCityInfoService.getProvinceDropDownBox(countryId);
        return ResultMsgUtil.success(list);
    }
    private void packaging(HttpServletRequest request, HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            fileName = java.net.URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        }
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
    }
}

