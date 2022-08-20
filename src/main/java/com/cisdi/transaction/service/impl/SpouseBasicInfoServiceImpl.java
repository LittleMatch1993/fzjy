package com.cisdi.transaction.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.utils.AuthSqlUtil;
import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.domain.dto.CadreFamiliesDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.SpouseBasicInfo;
import com.cisdi.transaction.domain.model.SysDictBiz;
import com.cisdi.transaction.domain.vo.CadreFamiliesExcelVO;
import com.cisdi.transaction.mapper.master.SpouseBasicInfoMapper;
import com.cisdi.transaction.service.SpouseBasicInfoService;
import com.cisdi.transaction.service.SysDictBizService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 配偶，子女及其配偶基本信息
 *
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:56
 */
@Service
public class SpouseBasicInfoServiceImpl extends ServiceImpl<SpouseBasicInfoMapper, SpouseBasicInfo> implements SpouseBasicInfoService {

    @Autowired
    private SysDictBizService sysDictBizService;
    /**
     * 检测表中是否有重复数据，不重复才添加。
     * 根据干部身份证号码 和身份证号码判断是否重复
     *
     * @param info
     */
    @Override
    public boolean insertSpouseBasicInfo(SpouseBasicInfo info) {
        info.setId("");
        info.setCreateTime(DateUtil.date());
        boolean b = this.save(info);
        return b;
    }

    @Override
    public void saveInfo(CadreFamiliesDTO dto) {
        SpouseBasicInfo entity = this.lambdaQuery().eq(SpouseBasicInfo::getCadreCardId, dto.getCadreCardId())
                .eq(SpouseBasicInfo::getName, dto.getName()).eq(SpouseBasicInfo::getTitle, dto.getTitle()).last(SqlConstant.ONE_SQL).one();
        if (entity == null) {
            SpouseBasicInfo info = new SpouseBasicInfo();
            BeanUtils.copyProperties(dto, info);
            info.setCreateTime(DateUtil.date());
            info.setUpdateTime(DateUtil.date());

            info.setTenantId(dto.getServiceLesseeId());
            info.setCreatorId(dto.getServiceUserId());
            this.save(info);
        }
    }

    @Override
    public int selectCount(String cadreCardId, String name, String title, List<SpouseBasicInfo> sbInfo) {
        if(CollectionUtil.isEmpty(sbInfo)){
            return 0;
        }
        List<SpouseBasicInfo> list = sbInfo.stream().filter(info ->
                cadreCardId.equals(info.getCadreCardId())
                        && name.equals(info.getName()) && title.equals(info.getTitle())).collect(Collectors.toList());
        return CollectionUtil.isEmpty(list)?0:list.size();
    }

    @Override
    public long selectCount(String cadreCardId, String name, String title) {
        long index = this.lambdaQuery().eq(SpouseBasicInfo::getCadreCardId, cadreCardId)
                .eq(SpouseBasicInfo::getName, name).eq(SpouseBasicInfo::getTitle, title).count();
        return index;
    }

    @Override
    public List<SpouseBasicInfo> selectAll() {
        List<SpouseBasicInfo>  list = this.lambdaQuery().list();
        return list;
    }

    @Override
    public SpouseBasicInfo selectByRefId(String refId) {
        SpouseBasicInfo spouse = this.lambdaQuery().eq(SpouseBasicInfo::getRefId,refId).one();
        return spouse;
    }

    @Override
    public Map<String,Object>  selectGbFamilyInfoByCardId(String cardId, int pageSize, int pageIndex) {
        Map<String,Object> resultMap = new HashMap<>();
        IPage<SpouseBasicInfo> page = new Page<>(pageIndex,pageSize);
        IPage<SpouseBasicInfo> pageData = this.lambdaQuery().eq(SpouseBasicInfo::getCadreCardId, cardId).page(page);
      /*  QueryWrapper<SpouseBasicInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SpouseBasicInfo::getCadreCardId, cardId);

        IPage<SpouseBasicInfo> pageData = this.baseMapper.selectPage(page, queryWrapper);
       */
        resultMap.put("total",pageData.getTotal());
        resultMap.put("records", pageData.getRecords());
        return resultMap;
    }

    @Override
    public List<CadreFamiliesExcelVO> export(List<String> ids) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        return this.baseMapper.selectBatchIds(ids).stream().map(t->{
            CadreFamiliesExcelVO vo = new CadreFamiliesExcelVO();
            BeanUtils.copyProperties(t,vo);
            String title = sysDictBizService.getDictValue(t.getTitle(), dictList);
            vo.setTitle(title);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CadreFamiliesExcelVO> export(CadreFamilyExportDto dto) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();

        return this.lambdaQuery().like(StringUtils.isNotBlank(dto.getCadre_name()),SpouseBasicInfo::getCadreName,dto.getCadre_name()).apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.SPOUSE_BASIC_INFO,dto.getOrgCode()),dto.getOrgCode()).orderByDesc(SpouseBasicInfo::getUpdateTime).list().stream().map(t->{
            CadreFamiliesExcelVO vo = new CadreFamiliesExcelVO();
            BeanUtils.copyProperties(t,vo);
            String title = sysDictBizService.getDictValue(t.getTitle(), dictList);
            vo.setTitle(title);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void addBatchSpouse(List<SpouseBasicInfo> spouseBasicInfos) {
        if (CollectionUtils.isEmpty(spouseBasicInfos)){
            return;
        }
        //所有干部身份证号
        List<String> cadreCardIds = spouseBasicInfos.stream().map(SpouseBasicInfo::getCadreCardId).distinct().collect(Collectors.toList());
        List<SpouseBasicInfo> spouseBasicInfoList = this.lambdaQuery().in(SpouseBasicInfo::getCadreCardId, cadreCardIds).list();
        if (!CollectionUtils.isEmpty(spouseBasicInfoList)){
            //筛选出不存在的
            List<SpouseBasicInfo> addSpouseBasicInfos = spouseBasicInfos.stream().filter(spouseBasicInfo -> {
                for (SpouseBasicInfo basicInfo : spouseBasicInfoList) {
                    if (basicInfo.getCadreCardId().equals(spouseBasicInfo.getCadreCardId())
                            && basicInfo.getTitle().equals(spouseBasicInfo.getTitle()) && basicInfo.getName().equals(spouseBasicInfo.getName())
                    ) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(addSpouseBasicInfos)){
                this.saveBatch(addSpouseBasicInfos);
            }
        }else {
            this.saveBatch(spouseBasicInfos);
        }
    }

}
