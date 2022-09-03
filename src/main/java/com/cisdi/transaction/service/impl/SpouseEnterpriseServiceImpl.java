package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.base.ResultCode;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.exception.BusinessException;
import com.cisdi.transaction.config.utils.AuthSqlUtil;
import com.cisdi.transaction.config.utils.CalendarUtil;
import com.cisdi.transaction.config.utils.NumberUtils;
import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.ExportReturnMessageVO;
import com.cisdi.transaction.domain.vo.ExportReturnVO;
import com.cisdi.transaction.domain.vo.KVVO;
import com.cisdi.transaction.mapper.master.InvestInfoMapper;
import com.cisdi.transaction.mapper.master.SpouseEnterpriseMapper;
import com.cisdi.transaction.service.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @Author: tgl
 * @Description:
 * @Date: 2022/9/1 15:17
 */
@Service
public class SpouseEnterpriseServiceImpl extends ServiceImpl<SpouseEnterpriseMapper, SpouseEnterprise> implements SpouseEnterpriseService {

    @Autowired
    private SpouseBasicInfoService spouseBasicInfoService;
    @Override
    public List<SpouseEnterprise> selectBySpouseIdAndEnterpriseIdAndType(String spouseId, String enterpriseId, String type) {
        return this.lambdaQuery().eq(SpouseEnterprise::getSpouseId,spouseId)
                .eq(SpouseEnterprise::getEnterpriseId,enterpriseId)
                .eq(StrUtil.isNotEmpty(type),SpouseEnterprise::getType,type).list();
    }

    @Override
    public boolean insertSpouseEnterprise(String spouseId, String enterpriseId, String type) {
        SpouseEnterprise enterprise = new SpouseEnterprise();
        enterprise.setSpouseId(spouseId);
        enterprise.setEnterpriseId(enterpriseId);
        enterprise.setCreateTime(DateUtil.date());
        enterprise.setType(type);
        return this.save(enterprise);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteByEnterpriseIdAndType(List<String> enterpriseIds, String type) {
        //1.获取中间表中要删除数据的管理数据
        List<SpouseEnterprise> enterpriseList = this.lambdaQuery().in(SpouseEnterprise::getEnterpriseId,enterpriseIds)
                .eq(SpouseEnterprise::getType,type).list();
        if(CollectionUtil.isEmpty(enterpriseList)){
            return 0;
        }
        //2.删除中间表的关联数据
        QueryWrapper<SpouseEnterprise> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().in(SpouseEnterprise::getEnterpriseId,enterpriseIds)
                .eq(SpouseEnterprise::getType,type);
        int index = this.baseMapper.delete(queryWrapper);
        if(index>0){
            //获取关联数据删除之前的家属信息id
            List<String> spouseIdList = enterpriseList.stream().map(SpouseEnterprise::getSpouseId).collect(Collectors.toList());
            List<SpouseEnterprise> list = this.lambdaQuery().in(SpouseEnterprise::getSpouseId, spouseIdList).list();
            List<String> deleteSpouseIdList = new ArrayList<>();
            if(CollectionUtil.isNotEmpty(list)){
                //获取关联数据删除之后的家属信息id
                List<String> endSpouseIdList = list.stream().map(SpouseEnterprise::getSpouseId).collect(Collectors.toList());
                //取差集,差集中的值
                deleteSpouseIdList = spouseIdList.stream().filter(e->!endSpouseIdList.contains(e)).collect(Collectors.toList());
            }else{
                deleteSpouseIdList.addAll(spouseIdList);
            }
            if(CollectionUtil.isNotEmpty(deleteSpouseIdList)){
                spouseBasicInfoService.removeByIds(deleteSpouseIdList);
            }
        }

        return index;
    }
}