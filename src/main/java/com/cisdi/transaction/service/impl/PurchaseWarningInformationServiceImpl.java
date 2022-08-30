package com.cisdi.transaction.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.AuthSqlUtil;
import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.constant.WarnCodeConstant;
import com.cisdi.transaction.domain.dto.BusinessTransactionDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.YjxxDTO;
import com.cisdi.transaction.domain.dto.YjxxObjectDTO;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.BusinessTransactionExcelVO;
import com.cisdi.transaction.enums.WarnTypeEnum;
import com.cisdi.transaction.mapper.master.EnterpriseDealInfoMapper;
import com.cisdi.transaction.mapper.master.PurchaseWarningInformationMapper;
import com.cisdi.transaction.service.BanDealInfoService;
import com.cisdi.transaction.service.EnterpriseDealInfoService;
import com.cisdi.transaction.service.PurchaseWarningInformationService;
import com.cisdi.transaction.service.SysDictBizService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotBlank;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tgl
 * @version 1.0
 * @date 2022/8/3 16:59
 */
@Service
@Slf4j
public class PurchaseWarningInformationServiceImpl extends ServiceImpl<PurchaseWarningInformationMapper, PurchaseWarningInformationInfo> implements PurchaseWarningInformationService {

    @Autowired
    private EnterpriseDealInfoService enterpriseDealInfoService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInfo(YjxxObjectDTO yjxxObjectDTO) {
        List<YjxxDTO> yjxxDTOS = yjxxObjectDTO.getYjxxDTOS();
        List<PurchaseWarningInformationInfo> purchaseWarningInformationInfos= Lists.newArrayList();
        List<String> codes = Arrays.asList(WarnCodeConstant.JYJY01, WarnCodeConstant.JYJY06, WarnCodeConstant.JYJY07, WarnCodeConstant.JYJY08);
        List<YjxxDTO> yjxxDTOList = yjxxDTOS.stream().filter(yjxxDTO -> {
            PurchaseWarningInformationInfo purchaseWarningInformationInfo=new PurchaseWarningInformationInfo();
            BeanUtils.copyProperties(yjxxDTO,purchaseWarningInformationInfo);
            purchaseWarningInformationInfo.setId(null);
            purchaseWarningInformationInfo.setWarnId(yjxxDTO.getId());
            purchaseWarningInformationInfo.setCreateTime(DateUtil.date());
            purchaseWarningInformationInfo.setUpdateTime(DateUtil.date());
            purchaseWarningInformationInfo.setProcessState("1");
            purchaseWarningInformationInfos.add(purchaseWarningInformationInfo);
            return codes.contains(yjxxDTO.getYjlxid());}).collect(Collectors.toList());
        if (!purchaseWarningInformationInfos.isEmpty()){
            this.saveBatch(purchaseWarningInformationInfos);
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<BusinessTransactionDTO> dtos = yjxxDTOList.stream().map(e -> {
            String outputField = e.getOutputField();
            JSONObject yjxxObject = JSONObject.parseObject(outputField);
            BusinessTransactionDTO dto = new BusinessTransactionDTO();
            dto.setUniqueCode(yjxxObject.getString("唯一标识码"));
            dto.setCode(yjxxObject.getString("社会统一信用代码"));
            dto.setPurchaseName(yjxxObject.getString("采购单位名称"));
            dto.setSupplier(yjxxObject.getString("禁止交易供应商名称"));
            dto.setBusinessCode(yjxxObject.getString("参与的采购业务编码"));
            dto.setBusinessName(yjxxObject.getString("参与的采购业务名称"));
            dto.setInfoTips(e.getYjnr());
            String yjlxid = e.getYjlxid();
            switch (WarnTypeEnum.valueOf(yjlxid)){
                case JYJY01:
                    dto.setInfoType(WarnTypeEnum.JYJY01.getName());
                    dto.setInfoTips(dto.getPurchaseName()+"开展的采购业务（业务编码"+dto.getBusinessCode()+"，业务名称"+dto.getBusinessName()+"）存在禁止交易范围内供应商（"+dto.getSupplier()+"）参与采购业务现象，系统已经实施予以禁止操作处理");
                    break;
                case JYJY06:
                    dto.setInfoType(WarnTypeEnum.JYJY06.getName());
                    dto.setInfoTips(dto.getPurchaseName()+"开展的采购业务（业务编码"+dto.getBusinessCode()+"，业务名称"+dto.getBusinessName()+"）存在禁止交易范围以外的供应商（"+dto.getSupplier()+"）参与采购业务现象");
                    break;
                case JYJY07:
                    dto.setStraightPipeName(yjxxObject.getString("直管单位名称"));
                    dto.setSupplierPrice(yjxxObject.getString("供应商报价"));
                    dto.setBidder(yjxxObject.getString("是否为中标供应商"));
                    dto.setInfoType(WarnTypeEnum.JYJY07.getName());
                    dto.setInfoTips(dto.getPurchaseName()+"开展的采购业务（业务编码"+dto.getBusinessCode()+"，业务名称"+dto.getBusinessName()+"）存在禁止交易范围以外供应商（"+dto.getSupplier()+"）参与采购业务现象，报价为"+dto.getSupplierPrice()+"元，"+("是".equals(dto.getBidder())?"是中标供应商":"不是中标供应商"));
                    break;
                case JYJY08:
                    dto.setInfoType(WarnTypeEnum.JYJY08.getName());
                    dto.setStraightPipeName(yjxxObject.getString("直管单位名称"));
                    dto.setContractPrice(yjxxObject.getString("合同价格"));
                    dto.setContractCode(yjxxObject.getString("采购合同编码"));
                    dto.setContractName(yjxxObject.getString("采购合同名称"));
                    try {
                        dto.setContractTime(StringUtils.isBlank(yjxxObject.getString("签订日期")) ? null : simpleDateFormat.parse(yjxxObject.getString("签订日期")));
                    } catch (ParseException e1) {
                        log.error("日期格式错误{}",yjxxObject.getString("签订日期"));
                    }
                    dto.setInfoTips(dto.getPurchaseName()+"开展的采购业务（业务编码"+dto.getBusinessCode()+"，业务名称"+dto.getBusinessName()+"）禁止交易范围以外供应商（"+dto.getSupplier()+"）参与采购业务现象，合同名称"+dto.getContractName()+",签订合同金额为"+dto.getContractPrice()+"元，签订日期"+ (Objects.isNull(dto.getContractTime())?"无":simpleDateFormat.format(dto.getContractTime())));
                    break;
            }
            return dto;
        }).collect(Collectors.toList());
        enterpriseDealInfoService.saveList(dtos);
    }
}
