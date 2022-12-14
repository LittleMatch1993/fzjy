package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.InvestInfo;
import com.cisdi.transaction.domain.model.MechanismInfo;
import com.cisdi.transaction.domain.vo.ExportReturnMessageVO;
import com.cisdi.transaction.domain.vo.ExportReturnVO;
import com.cisdi.transaction.domain.vo.KVVO;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:16
 */
public interface MechanismInfoService extends IService<MechanismInfo> {
    boolean updateState(List<String> ids, String state);

    int  countByNameAndCardIdAndCode(String name,String cardId,String code);

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
    void overrideInvestInfo(String id , MechanismInfoDTO dto);


    MechanismInfo getRepeatInvestInfo(String name, String cardId, String code);

    ResultMsgUtil<Object> submitMechanismInfo(SubmitDto submitDto);

    /**
     * 新增家属信息
     * @param info
     */
    void addFamilyInfo(MechanismInfo info);

    /**
     * 新增
     * @param dto
     */
    void saveMechanismInfo(MechanismInfoDTO dto);

    /**
     * 编辑
     * @param dto
     */
    void editMechanismInfo(MechanismInfoDTO dto);

    /**
     * 批量导入新增
     * @param list
     */
    void saveBatchInvestInfo(List<CommunityServiceDTO> list);

    /**
     * 导出功能
     * @return
     */
    List<CommunityServiceDTO> exportCommunityServiceExcel(List<String> ids);

    /**
     * 批量导入新增
     * @param list
     */
    void saveBatchInvestInfo(List<CommunityServiceDTO> list, BaseDTO baseDTO, ExportReturnVO exportReturnVO);

    /**
     * 导出功能
     * @return
     */
    List<CommunityServiceDTO> exportCommunityServiceExcel(CadreFamilyExportDto exportDto);

    List<KVVO> getCreateInfoForMechanism(String orgCode);
}
