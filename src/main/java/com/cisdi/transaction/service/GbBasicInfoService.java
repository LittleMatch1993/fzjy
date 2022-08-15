package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.dto.CadreDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.GbOrgInfo;
import com.cisdi.transaction.domain.vo.CadreExcelVO;

import java.util.List;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/3 14:35
 */
public interface GbBasicInfoService extends IService<GbBasicInfo> {
    List<GbBasicInfo> selectByName(String name,String orgCode);

    List<GbBasicInfo> selectGbDictVoByName(String name,String orgCode);

    /**
     * 新增干部信息
     * @param dto
     */
    void saveInfo(CadreDTO dto);

    List<GbBasicInfo> selectBatchByCardIds(List<String> cardIds);

    /**
     * 同步数据
     */
    void syncData();

    /**
     * 导出
     * @param dto
     */
    List<CadreExcelVO> export(List<String> ids);

    /**
     * 根据身份证id查询干部的基本信息带组织信息
     * @param ids 身份证id
     * @return
     */
    List<GbOrgInfo> selectGbOrgInfoByCardIds(List<String> ids);
    List<GbOrgInfo> selectByOrgCode(String orgCode);

    List<GbOrgInfo> selectByOrgCodeAndGbName(String orgCode,String name);
}
