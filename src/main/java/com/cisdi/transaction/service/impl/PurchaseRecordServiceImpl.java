package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.domain.model.PurchaseBanDealInfo;
import com.cisdi.transaction.domain.model.PurchaseRecord;
import com.cisdi.transaction.mapper.master.PurchaseRecordMapper;
import com.cisdi.transaction.service.PurchaseRecordService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: cxh
 * @Description: 推送采购平台数据日志记录服务层
 * @Date: 2022/9/9 9:36
 */
@Service
public class PurchaseRecordServiceImpl extends ServiceImpl<PurchaseRecordMapper, PurchaseRecord> implements PurchaseRecordService {
    @Override
    public boolean insertPurchaseRecord(List<PurchaseRecord> purchaseList) {
        boolean b = false;
        List<PurchaseRecord> list = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(purchaseList)){
            purchaseList.parallelStream().forEach(e->{
                PurchaseRecord record = new PurchaseRecord();
                BeanUtil.copyProperties(e,record);
                list.add(record);
            });
            b = this.saveBatch(list);
        }
        return b;
    }

    /**
     * 逻辑删除
     * @param ids
     * @return
     */
    @Override
    public boolean deletePushDataForPurchase(List<String> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return false;
        }
        UpdateWrapper<PurchaseRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(PurchaseRecord::getDelFlag,1).in(PurchaseRecord::getRefId,ids);
        return  this.update(updateWrapper);
    }
}
