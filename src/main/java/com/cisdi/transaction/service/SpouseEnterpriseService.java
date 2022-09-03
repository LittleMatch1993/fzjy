package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.dto.CadreDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.CadreExcelVO;

import java.util.List;

/**
 * @author tgl
 * @version 1.0
 * @date 2022/9/1 14:35
 */
public interface SpouseEnterpriseService extends IService<SpouseEnterprise> {

    public List<SpouseEnterprise> selectBySpouseIdAndEnterpriseIdAndType(String spouseId,String enterpriseId,String type);

    public boolean insertSpouseEnterprise(String spouseId,String enterpriseId,String type);

    public int deleteByEnterpriseIdAndType(List<String> enterpriseIds,String type);
}
