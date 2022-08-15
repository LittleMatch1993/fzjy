package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.dto.InstitutionalFrameworkDTO;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.domain.vo.InstitutionalFrameworkExcelVO;

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
     * 通过结果同步数据
     */
    void  syncDa();

    /**
     * 导出
     * @param ids
     * @return
     */
    List<InstitutionalFrameworkExcelVO> export(List<String> ids);

    Org selectByOrgancode(String orgCode);

    List<Org> selectChildOrgByPathnamecode(String pathnamecode);

    List<Org> selectByName(String name,String orgCode);
}
