package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.OrgTree;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.InstitutionalFrameworkDTO;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.domain.vo.InstitutionalFrameworkExcelVO;
import com.cisdi.transaction.domain.vo.OrgConditionVO;
import com.cisdi.transaction.domain.vo.OrgVo;
import com.cisdi.transaction.domain.vo.SearchVO;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/1 23:53
 */
public interface OrgService  extends IService<Org> {
    /**
     * 组织机构新增信息
     * @param dto
     */
    void saveInfo(InstitutionalFrameworkDTO dto);

    /**
     * 根据增量同步日期删除
     * @param asgDate
     * @return
     */
    boolean  removeByAsgDate(String asgDate);
    //public Org selectById(String id);

    /**
     * 根据组织编码和编码链查询总数
     * @param anCode
     * @param pathNamecode
     * @return
     */
    int countByAncodeAndPathNamecode(String anCode,String pathNamecode);

    /**
     * 编辑组织信息
     * @param dto
     */
    void editOrgInfo(InstitutionalFrameworkDTO dto);

    /**
     * 通过接口同步数据
     * @param date 增量数据的日期。为空查询所有
     */
    void  syncDa(String date);

    /**
     * 导出
     * @param ids
     * @return
     */
    List<InstitutionalFrameworkExcelVO> export(List<String> ids);

    Org selectByOrgancode(String orgCode);

    /**
     * 根据orgcode批量查询
     * @param orgCodes
     * @return
     */
    List<Org> selectByOrgancode(List<String> orgCodes);

    /**
     * 获取多个组织的最高等级
     * @param orgs
     * @return
     */
    String getHighestLevel(List<Org> orgs);

    List<Org> selectChildOrgByPathnamecode(String pathnamecode);

    List<Org> selectByName(SearchVO search);

    List<Org> selectUnitByName(String name,String orgCode);

    Org getOrgByUnitCodeAndDepartmentName(String unitCode,String departmentName);

    List<String> getCardIdsByAsgpathnamecode(String asgpathnamecode);

    String getOrgNamesByCodePath(String codePath);

    List<InstitutionalFrameworkExcelVO> export(CadreFamilyExportDto dto);

    List<OrgVo> selectOrgByOrgCode(OrgConditionVO searchVO);

    String selectAsgpathnamecodeByOrgCode(OrgConditionVO searchVO);

    /**
     * 按权限获取组织结构树
     * @param orgCode
     * @return
     */
    List<OrgTree> selectOrgTree(String orgCode);

    List<Org> selectByAsgpathnamecode(String asgpathnamecode);

    List<Org> selectByOrgancodes(List<String> orgCodeList);

    String selectAsgpathnamecodeByMoreOrgCode(OrgConditionVO searchVO);

    List<String> getOrgNameListByCodePath(String codePath);
}
