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
import com.cisdi.transaction.domain.model.SpouseEnterprise;
import com.cisdi.transaction.domain.model.SysDictBiz;
import com.cisdi.transaction.domain.vo.CadreFamiliesExcelVO;
import com.cisdi.transaction.mapper.master.SpouseBasicInfoMapper;
import com.cisdi.transaction.service.SpouseBasicInfoService;
import com.cisdi.transaction.service.SpouseEnterpriseService;
import com.cisdi.transaction.service.SysDictBizService;
import com.google.common.collect.Lists;
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
    @Autowired
    private SpouseEnterpriseService spouseEnterpriseService;
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
    public List<SpouseBasicInfo> selectSpouseInfo(String cadreCardId, String name, String title) {
        return this.lambdaQuery().eq(SpouseBasicInfo::getCadreCardId, cadreCardId)
                .eq(SpouseBasicInfo::getName, name).eq(SpouseBasicInfo::getTitle, title).list();
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
        return this.list(new QueryWrapper<SpouseBasicInfo>()
                .orderBy(StringUtils.isNotBlank(dto.getColumnName())&&Objects.nonNull(dto.getIsAsc()),dto.getIsAsc(),dto.getColumnName())
                .orderByDesc(StringUtils.isBlank(dto.getColumnName())||Objects.isNull(dto.getIsAsc()),"create_time")
                .lambda()
                .like(StringUtils.isNotBlank(dto.getCadre_name()),SpouseBasicInfo::getCadreName,dto.getCadre_name()).apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.SPOUSE_BASIC_INFO,dto.getOrgCode()),dto.getOrgCode())
        ).stream().map(t->{
            CadreFamiliesExcelVO vo = new CadreFamiliesExcelVO();
            BeanUtils.copyProperties(t,vo);
            String title = sysDictBizService.getDictValue(t.getTitle(), dictList);
            vo.setTitle(title);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void addBatchSpouse(List<SpouseBasicInfo> spouseBasicInfos,String type) {
        if (CollectionUtils.isEmpty(spouseBasicInfos)){
            return;
        }
        //将关联的企业id和家人唯一码做映射
        Map<String, String> refIdCodeMap = spouseBasicInfos.stream().collect(Collectors.toMap(SpouseBasicInfo::getRefId, spouseBasicInfo -> spouseBasicInfo.getCadreCardId() + "," + spouseBasicInfo.getTitle() + "," + spouseBasicInfo.getName()));
        Set<String> uniqueCodeSet=new HashSet<>();
        //去掉重复的数据
        spouseBasicInfos=spouseBasicInfos.stream().filter(spouseBasicInfo->{
            String uniqueCode = spouseBasicInfo.getCadreCardId()+","+spouseBasicInfo.getTitle()+","+spouseBasicInfo.getName();
            if (uniqueCodeSet.contains(uniqueCode)){
                return false;
            }else {
                uniqueCodeSet.add(uniqueCode);
                return true;
            }
        }).collect(Collectors.toList());
        //所有干部身份证号
        List<String> cadreCardIds = spouseBasicInfos.stream().map(SpouseBasicInfo::getCadreCardId).distinct().collect(Collectors.toList());
        //获取已存在的干部对应的家人
        List<SpouseBasicInfo> spouseBasicInfoList = this.lambdaQuery().in(SpouseBasicInfo::getCadreCardId, cadreCardIds).list();

        if (!CollectionUtils.isEmpty(spouseBasicInfoList)){
            //存在的家人信息
            List<SpouseBasicInfo> existSpouseBasicInfoList= Lists.newArrayList();
            //筛选出不存在的家人
            List<SpouseBasicInfo> addSpouseBasicInfos = spouseBasicInfos.stream().filter(spouseBasicInfo -> {
                for (SpouseBasicInfo basicInfo : spouseBasicInfoList) {
                    if (basicInfo.getCadreCardId().equals(spouseBasicInfo.getCadreCardId())
                            && basicInfo.getTitle().equals(spouseBasicInfo.getTitle()) && basicInfo.getName().equals(spouseBasicInfo.getName())
                    ) {
                        existSpouseBasicInfoList.add(basicInfo);
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(addSpouseBasicInfos)){
                this.saveBatch(addSpouseBasicInfos);
                SpouseBasicInfo spouseBasicInfoOne = addSpouseBasicInfos.get(0);
                //新增的家人唯一码和id映射
                Map<String, String> codeSpouseIdMap = addSpouseBasicInfos.stream().collect(Collectors.toMap(spouseBasicInfo -> spouseBasicInfo.getCadreCardId() + "," + spouseBasicInfo.getTitle() + "," + spouseBasicInfo.getName(),SpouseBasicInfo::getId));
                //新增关联的企业id和家人唯一码做映射
                Map<String, String> filterMap = refIdCodeMap.entrySet().stream().filter(refIdCode ->
                        codeSpouseIdMap.containsKey(refIdCode.getValue())
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                List<SpouseEnterprise> spouseEnterpriseList = filterMap.entrySet().stream().map(refIdCode -> {
                    SpouseEnterprise spouseEnterprise = new SpouseEnterprise();
                    BeanUtils.copyProperties(spouseBasicInfoOne, spouseEnterprise);
                    spouseEnterprise.setId(null);
                    spouseEnterprise.setEnterpriseId(refIdCode.getKey());
                    spouseEnterprise.setSpouseId(codeSpouseIdMap.get(refIdCode.getValue()));
                    spouseEnterprise.setType(type);
                    return spouseEnterprise;
                }).collect(Collectors.toList());
                spouseEnterpriseService.saveBatch(spouseEnterpriseList);
            }

            if (!existSpouseBasicInfoList.isEmpty()){
                //已存在的家人唯一码和id映射
                Map<String, String> codeSpouseIdMap = existSpouseBasicInfoList.stream().collect(Collectors.toMap(spouseBasicInfo -> spouseBasicInfo.getCadreCardId() + "," + spouseBasicInfo.getTitle() + "," + spouseBasicInfo.getName(),SpouseBasicInfo::getId));
                //筛选出本次已存在家人关联的企业id
                List<String> refIds = refIdCodeMap.entrySet().stream().filter(e -> codeSpouseIdMap.containsKey(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
                //查询出所有已关联的家人和企业信息
                List<SpouseEnterprise> spouseEnterpriseList = spouseEnterpriseService.lambdaQuery().eq(SpouseEnterprise::getType, type).in(SpouseEnterprise::getSpouseId, codeSpouseIdMap.values()).in(SpouseEnterprise::getEnterpriseId, refIds).list();
                SpouseBasicInfo spouseBasicInfo = spouseBasicInfos.get(0);
                if (CollectionUtils.isEmpty(spouseEnterpriseList)){
                    //如果没有关联的则全部关联
                    List<SpouseEnterprise> spouseEnterpriseAddList=refIds.stream().map(refId->{
                        SpouseEnterprise spouseEnterprise = new SpouseEnterprise();
                        BeanUtils.copyProperties(spouseBasicInfo, spouseEnterprise);
                        spouseEnterprise.setId(null);
                        spouseEnterprise.setEnterpriseId(refId);
                        spouseEnterprise.setSpouseId(codeSpouseIdMap.get(refIdCodeMap.get(refId)));
                        spouseEnterprise.setType(type);
                        return spouseEnterprise;
                    }).collect(Collectors.toList());
                    spouseEnterpriseService.saveBatch(spouseEnterpriseAddList);
                }else {
                    //将有关联的去除掉再保存
                    List<SpouseEnterprise> spouseEnterpriseAddList=refIds.stream()
                            .filter(refId->{
                                for (SpouseEnterprise spouseEnterprise : spouseEnterpriseList) {
                                    if (spouseEnterprise.getEnterpriseId().equals(refId)&&spouseEnterprise.getSpouseId().equals(
                                            codeSpouseIdMap.get(refIdCodeMap.get(refId))
                                    )){
                                        return false;
                                    }
                                }
                                return true;
                            })
                            .map(refId->{
                        SpouseEnterprise spouseEnterprise = new SpouseEnterprise();
                        BeanUtils.copyProperties(spouseBasicInfo, spouseEnterprise);
                        spouseEnterprise.setId(null);
                        spouseEnterprise.setEnterpriseId(refId);
                        spouseEnterprise.setSpouseId(codeSpouseIdMap.get(refIdCodeMap.get(refId)));
                        spouseEnterprise.setType(type);
                        return spouseEnterprise;
                    }).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(spouseEnterpriseAddList)){
                        spouseEnterpriseService.saveBatch(spouseEnterpriseAddList);
                    }
                }
            }
        }else {
            this.saveBatch(spouseBasicInfos);
            SpouseBasicInfo spouseBasicInfoOne = spouseBasicInfos.get(0);
            Map<String, String> codeSpouseIdMap = spouseBasicInfos.stream().collect(Collectors.toMap(spouseBasicInfo -> spouseBasicInfo.getCadreCardId() + "," + spouseBasicInfo.getTitle() + "," + spouseBasicInfo.getName(),SpouseBasicInfo::getId));
            //家人企业关联全部保存
            List<SpouseEnterprise> spouseEnterpriseList = refIdCodeMap.entrySet().stream().map(refIdCode -> {
                SpouseEnterprise spouseEnterprise = new SpouseEnterprise();
                BeanUtils.copyProperties(spouseBasicInfoOne, spouseEnterprise);
                spouseEnterprise.setId(null);
                spouseEnterprise.setEnterpriseId(refIdCode.getKey());
                spouseEnterprise.setSpouseId(codeSpouseIdMap.get(refIdCode.getValue()));
                spouseEnterprise.setType(type);
                return spouseEnterprise;
            }).collect(Collectors.toList());
            spouseEnterpriseService.saveBatch(spouseEnterpriseList);
        }
    }

}
