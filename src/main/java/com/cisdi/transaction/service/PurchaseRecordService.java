package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.model.PurchaseBanDealInfo;
import com.cisdi.transaction.domain.model.PurchaseRecord;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/9/9 9:35
 */
public interface PurchaseRecordService extends IService<PurchaseRecord> {
    public boolean insertPurchaseRecord(List<PurchaseRecord> purchaseList);

    public boolean deletePushDataForPurchase(List<String> ids);
}
