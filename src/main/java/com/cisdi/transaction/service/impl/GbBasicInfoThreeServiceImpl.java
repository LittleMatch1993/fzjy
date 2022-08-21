package com.cisdi.transaction.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.GbBasicInfoThree;
import com.cisdi.transaction.mapper.three.GbBasicInfoThreeMapper;
import com.cisdi.transaction.service.GbBasicInfoThreeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/7 22:57
 */
@Service
public class GbBasicInfoThreeServiceImpl extends ServiceImpl<GbBasicInfoThreeMapper,GbBasicInfoThree> implements GbBasicInfoThreeService {
    @Override
    public List<GbBasicInfoThree> selectGbBasicInfo() {
        return this.baseMapper.selectGbBasicInfo();
    }

    @Override
    public List<GbBasicInfoThree> selectOldGbBasicInfo() {
        return this.baseMapper.selectOldGbBasicInfo();
    }
}
