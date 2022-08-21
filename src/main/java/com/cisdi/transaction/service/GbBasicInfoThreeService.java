package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.GbBasicInfoThree;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/7 22:55
 */
public interface GbBasicInfoThreeService extends IService<GbBasicInfoThree> {

    public List<GbBasicInfoThree> selectGbBasicInfo();
    public List<GbBasicInfoThree> selectOldGbBasicInfo();
}
