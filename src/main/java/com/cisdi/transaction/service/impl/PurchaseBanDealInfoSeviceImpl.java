package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.model.BanDealInfo;
import com.cisdi.transaction.domain.model.PurchaseBanDealInfo;
import com.cisdi.transaction.domain.model.SysDictBiz;
import com.cisdi.transaction.mapper.slave.PurchaseBanDealInfoMapper;
import com.cisdi.transaction.service.PurchaseBanDealInfoSevice;
import com.cisdi.transaction.service.SysDictBizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/5 9:46
 */
@Service
public class PurchaseBanDealInfoSeviceImpl extends ServiceImpl<PurchaseBanDealInfoMapper, PurchaseBanDealInfo> implements PurchaseBanDealInfoSevice {

    @Autowired
    private SysDictBizService sysDictBizService;

    @Override
    public boolean pushDataForPurchase(BanDealInfo info) {
        PurchaseBanDealInfo purchase = new PurchaseBanDealInfo();
        if(!SystemConstant.VALID_STATE.equals(info.getState())){
            return false;
        }
        BeanUtil.copyProperties(info, purchase);
        purchase.setCreateTime(DateUtil.date());
        boolean b = this.save(purchase);

        return b;
    }

    @Override
    public boolean pushDatchDataForPurchase(List<BanDealInfo> infos) {
         List<SysDictBiz> sysDictBizs = sysDictBizService.selectList();
        //每次推送给采购平台数据时 都认为是被编辑后重新推送的。
        List<String> ids = infos.stream().map(BanDealInfo::getId).collect(Collectors.toList());
        this.deletePushDataForPurchase(ids);
        //推送数据
        List<PurchaseBanDealInfo> purchaseList = new ArrayList<>();
        infos.stream().forEach(info->{
            if(SystemConstant.VALID_STATE.equals(info.getState())){
                PurchaseBanDealInfo purchase = new PurchaseBanDealInfo();
                String refId = info.getId();
                BeanUtil.copyProperties(info, purchase,new String[]{"id"});
                purchase.setCreateTime(DateUtil.date());
                purchase.setRefId(refId);
                purchase.setDelFlag(0);//未删除
                String temp = purchase.getIsExtends();
                String isExtends = sysDictBizService.getDictValue(temp, sysDictBizs);
                purchase.setIsExtends(StrUtil.isNotEmpty(isExtends)?isExtends:temp);
                purchaseList.add(purchase);
            }
        });
        if(CollectionUtil.isNotEmpty(purchaseList)){
            return this.saveBatch(purchaseList);
        }
        return false;
    }

    /**
     * 逻辑删除，因为没有物理删除权限
     * @param ids
     * @return
     */
    @Override
    public boolean deletePushDataForPurchase(List<String> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return false;
        }
        UpdateWrapper<PurchaseBanDealInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(PurchaseBanDealInfo::getDelFlag,1).in(PurchaseBanDealInfo::getRefId,ids);
        return  this.update(updateWrapper);
    }
}
