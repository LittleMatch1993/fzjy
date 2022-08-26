package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.dto.CadreDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.model.AuthUser;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.GbBasicInfoThree;
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

    List<GbBasicInfo> selectGbInfoByNameAndUnitAndPost(String name,String unit,String post,String cardId);

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
     * 获取原数据
     */
    public List<GbBasicInfoThree> selectOldGbBasicInfo();


    /**
     * 导出
     * @param dto
     */
    List<CadreExcelVO> export(CadreFamilyExportDto dto);

    /**
     * 根据身份证id查询干部的基本信息带组织信息
     * @param ids 身份证id
     * @return
     */
    List<GbOrgInfo> selectGbOrgInfoByCardIds(List<String> ids);
    List<GbOrgInfo> selectByOrgCode(String orgCode);

    /**
     * 获取指定干部的组织部门信息
     * @param orgCode
     * @param cardIds 干部身份证id
     * @return
     */
    List<GbOrgInfo> selectByOrgCodeAndCardIds(String orgCode,List<String> cardIds);

    List<GbOrgInfo> selectByOrgCodeAndGbName(String orgCode,String name);

    List<String> selectNoAuthCardIds(String orgCode);

    /**
     * 获取当前系统可登录账户信息
     * @return
     */
    public List<AuthUser> selectAuthUser();
}
