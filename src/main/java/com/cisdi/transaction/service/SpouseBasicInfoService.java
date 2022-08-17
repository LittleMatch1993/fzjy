package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.dto.CadreFamiliesDTO;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.SpouseBasicInfo;
import com.cisdi.transaction.domain.vo.CadreFamiliesExcelVO;

import java.util.List;
import java.util.Map;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:56
 */
public interface SpouseBasicInfoService extends IService<SpouseBasicInfo> {

    /**
     * 新增配偶，子女及其配偶基本信息
     * @param info
     */
    public  boolean insertSpouseBasicInfo(SpouseBasicInfo info);

    /**
     * 新增干部家属信息
     * @param dto
     */
    void saveInfo(CadreFamiliesDTO dto);

    /**
     * 根据干部身份证号，家属姓名，称谓查询重复数
     * @param cadreCardId
     * @param name
     * @param title
     * @return
     */
    int selectCount(String cadreCardId, String name, String title, List<SpouseBasicInfo> sbInfo);

    /**
     * 数据库中取查询家属信息是否重复
     * @param cadreCardId 干部身份证
     * @param name 家属信息
     * @param title 称谓
     * @return
     */
    long selectCount(String cadreCardId, String name, String title);

    List<SpouseBasicInfo> selectAll();

    /**
     * 根据干部查询家属信息
     * @param cardId
     * @param pageSize
     * @param pageIndex
     * @return
     */
    Map<String,Object>selectGbFamilyInfoByCardId(String cardId, int pageSize, int pageIndex);

    /**
     * 导出功能
     * @param ids
     * @return
     */
    List<CadreFamiliesExcelVO> export(List<String> ids);
}
