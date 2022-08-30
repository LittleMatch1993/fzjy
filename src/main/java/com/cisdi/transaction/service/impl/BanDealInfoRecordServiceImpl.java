package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.domain.model.BanDealInfo;
import com.cisdi.transaction.domain.model.BanDealInfoRecord;
import com.cisdi.transaction.mapper.master.BanDealInfoMapper;
import com.cisdi.transaction.mapper.master.BanDealInfoRecordMapper;
import com.cisdi.transaction.service.BanDealInfoRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/4 11:11
 */
@Service
public class BanDealInfoRecordServiceImpl extends ServiceImpl<BanDealInfoRecordMapper, BanDealInfoRecord> implements BanDealInfoRecordService {

    @Transactional
    @Override
    public boolean insertBanDealInfoRecord(List<BanDealInfo> infoList, String operateType) {
        List<BanDealInfoRecord> records = new ArrayList<>();
        long dateLong = DateUtil.date().getTime();
        for (int i = 0; i < infoList.size(); i++) {
            BanDealInfo e = infoList.get(i);
            BanDealInfoRecord record = new BanDealInfoRecord();
            record.setOperationType(operateType);
            BeanUtil.copyProperties(e,record,new String[]{"id"});
            record.setCreateTime(DateUtil.date(dateLong+(i*1000)));
            record.setUpdateTime(DateUtil.date());
            records.add(record);
        }
        boolean b = this.saveBatch(records);
        return b;
    }
}
