package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.dto.BusinessTransactionDTO;
import com.cisdi.transaction.domain.model.EnterpriseDealInfo;
import com.cisdi.transaction.domain.vo.BusinessTransactionExcelVO;

import java.util.List;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/3 16:59
 */
public interface EnterpriseDealInfoService extends IService<EnterpriseDealInfo> {
    /**
     * 新增信息
     * @param dto
     */
    void saveInfo(BusinessTransactionDTO dto);

    /**
     * 导出功能
     * @param ids
     * @return
     */
    List<BusinessTransactionExcelVO> export(List<String> ids);

    /**
     * 新增信息
     * @param dtos
     */
    void saveList(List<BusinessTransactionDTO> dtos);
}
