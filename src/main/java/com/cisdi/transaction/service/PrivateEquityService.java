package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.InvestInfo;
import com.cisdi.transaction.domain.model.PrivateEquity;
import com.cisdi.transaction.domain.vo.ExportReturnVO;
import com.cisdi.transaction.domain.vo.KVVO;

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
     * 新增家属信息
     * @param info
     */
    void addFamilyInfo(PrivateEquity info);

    /**
     * 批量修改的tips不同相同的值
     * @param kvList
     * @return
     */
    int  updateTips(List<KVVO> kvList);

    /**
     * 批量修改的tips相同的值
     * @param kvList
     * @return
     */
    boolean  updateBathTips(List<KVVO> kvList);
    /**
     * 覆盖重复数据
     * @param id 以存在的数据id
     * @param dto 需要更新的数据
     */
    void overrideInvestInfo(String id , PrivateEquityDTO dto);


    PrivateEquity getRepeatInvestInfo(String name, String cardId, String code);

    public ResultMsgUtil<Object> submitPrivateEquity(SubmitDto subDto);

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
    @Deprecated
    void saveBatchInvestmentInfo(List<EquityFundsDTO> list);

    /**
     * 导出功能
     * @return
     */
    List<EquityFundsDTO> exportEquityFundsExcel(List<String> ids);

    /**
     *
     * @param list
     * @param baseDTO
     */
    /**
     * 批量导入新增
     * @param list
     */
    void saveBatchInvestmentInfo(List<EquityFundsDTO> list, BaseDTO baseDTO, ExportReturnVO exportReturnVO);

    /**
     * 导出功能
     * @return
     */
    List<EquityFundsDTO> exportEquityFundsExcel(CadreFamilyExportDto exportDto);
}
