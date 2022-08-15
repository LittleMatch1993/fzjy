package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.InvestInfo;
import com.cisdi.transaction.domain.model.PrivateEquity;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:07
 */
public interface PrivateEquityService extends IService<PrivateEquity> {
    boolean updateState(List<String> ids, String state);

    int  countByNameAndCardIdAndCode(String name,String cardId,String code);
    /**
     * 覆盖重复数据
     * @param id 以存在的数据id
     * @param dto 需要更新的数据
     */
    void overrideInvestInfo(String id , PrivateEquityDTO dto);


    PrivateEquity getRepeatInvestInfo(String name, String cardId, String code);

    public ResultMsgUtil<String> submitPrivateEquity(SubmitDto subDto);

    /**
     * 新增
     * @param dto
     */
    void savePrivateEquity(PrivateEquityDTO dto);

    /**
     * 修改
     * @param dto
     */
    void editPrivateEquity(PrivateEquityDTO dto);

    /**
     * 批量导入新增
     * @param list
     */
    void saveBatchInvestmentInfo(List<EquityFundsDTO> list);

    /**
     * 导出功能
     * @return
     */
    List<EquityFundsDTO> exportEquityFundsExcel(List<String> ids);
}
