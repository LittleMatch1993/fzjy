package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.model.BanDealInfo;
import com.cisdi.transaction.domain.model.PrivateEquity;
import com.cisdi.transaction.domain.model.PurchaseBanDealInfo;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/5 9:45
 */
public interface PurchaseBanDealInfoSevice extends IService<PurchaseBanDealInfo> {


    /**
     * 为采购平台推送单条效数据
     * @param info
     * @return
     */
    public boolean pushDataForPurchase(BanDealInfo info);

    /**
     * 为采购平台推送多条效数据,会先删除关联数据
     * @param infos
     * @return
     */
    public boolean pushDatchDataForPurchase(List<BanDealInfo> infos);

    /**
     * 删除推送给采购平台的数据。
     * <pre>
     *     当禁止交易表的数据被编辑后重新推送给采购平台时
     * </pre>
     * @param ids
     * @return
     */
    public boolean deletePushDataForPurchase(List<String> ids);
}
