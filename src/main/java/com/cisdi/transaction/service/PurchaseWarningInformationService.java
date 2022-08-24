package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.dto.BusinessTransactionDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.YjxxObjectDTO;
import com.cisdi.transaction.domain.model.EnterpriseDealInfo;
import com.cisdi.transaction.domain.model.PurchaseWarningInformationInfo;
import com.cisdi.transaction.domain.vo.BusinessTransactionExcelVO;

import java.util.List;

/**
 * @author tgl
 * @version 1.0
 * @date 2022/8/3 16:59
 */
public interface PurchaseWarningInformationService extends IService<PurchaseWarningInformationInfo> {

    void saveInfo(YjxxObjectDTO yjxxObjectDTO);
}
