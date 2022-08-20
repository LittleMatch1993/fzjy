package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.utils.AuthSqlUtil;
import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.domain.dto.CadreDTO;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.BusinessTransactionExcelVO;
import com.cisdi.transaction.domain.vo.CadreExcelVO;
import com.cisdi.transaction.mapper.master.GbBasicInfoMapper;
import com.cisdi.transaction.service.GbBasicInfoService;
import com.cisdi.transaction.service.GbBasicInfoThreeService;
import com.cisdi.transaction.service.OrgService;
import com.cisdi.transaction.service.SysDictBizService;
import com.cisdi.transaction.util.ThreadLocalUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/3 14:36
 */
@Service
@Slf4j
public class GbBasicInfoServiceImpl extends ServiceImpl<GbBasicInfoMapper, GbBasicInfo> implements GbBasicInfoService {

    @Autowired
    private GbBasicInfoThreeService gbBasicInfoThreeService;

    @Autowired
    private SysDictBizService sysDictBizService;

    @Autowired
    private OrgService orgService;

    @Override
    public List<GbBasicInfo> selectByName(String name,String orgCode) {
        List<GbBasicInfo> list =  new ArrayList<>();
         if(StrUtil.isEmpty(orgCode)){
            return list;
         }
        //List<GbBasicInfo> list = this.lambdaQuery().like(GbBasicInfo::getName, name).list();
        List<GbOrgInfo> gbOrgInfos = this.selectByOrgCode(orgCode.toString());
        if(CollectionUtil.isEmpty(gbOrgInfos)){
            return list;
        }
        List<GbOrgInfo> collect = gbOrgInfos.stream().filter(e -> e.getName().equals(name)).collect(Collectors.toList());
        if(CollectionUtil.isEmpty(collect)){
            return list;
        }
        collect.stream().forEach(e->{
            GbBasicInfo info = new GbBasicInfo();
            BeanUtil.copyProperties(e, info);
            list.add(info);
        });
        return list;
    }

    @Override
    public List<GbBasicInfo> selectGbInfoByNameAndUnitAndPost(String name,String unit,String post) {
        QueryWrapper<GbBasicInfo> queryWrapper = new QueryWrapper<>();
        List<GbBasicInfo> list = this.lambdaQuery().eq(GbBasicInfo::getName, name)
                .eq(GbBasicInfo::getUnit, unit)
                .eq(GbBasicInfo::getPost, post).list();
        return list;
    }

    @Override
    public List<GbBasicInfo> selectGbDictVoByName(String name, String orgCode) {
        List<GbBasicInfo> list =  new ArrayList<>();
        if(StrUtil.isEmpty(orgCode)){
            return list;
        }
        Org org = orgService.selectByOrgancode(orgCode);
        if(Objects.isNull(org)){
            return list;
        }
        String asglevel = org.getAsglevel();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")) { //看所有
            QueryWrapper<GbBasicInfo> queryWrapper = new QueryWrapper();
            if(StrUtil.isNotEmpty(name)){ //有名字时
                queryWrapper.lambda().like(GbBasicInfo::getName, name);
            }else{
                queryWrapper.lambda().last(SqlConstant.ONE_SQL_YB);
            }
           return this.baseMapper.selectList(queryWrapper);
        }else{ //按权限查看数据
            String pathNameCode = org.getAsgpathnamecode();
            List<GbOrgInfo> gbOrgInfoList = null;
            if(StrUtil.isNotEmpty(name)){ //有名字时
                gbOrgInfoList = this.baseMapper.selectByOrgCodeAndGbName(name, pathNameCode);
            }else{
                gbOrgInfoList = this.baseMapper.selectByOrgCodeAndGbNamePage(name, pathNameCode);
            }
            if(CollectionUtil.isEmpty(gbOrgInfoList)){
                return list;
            }
            List<GbBasicInfo>  gbBasicInfoList = new ArrayList<>();
            gbOrgInfoList.stream().forEach(e->{
                GbBasicInfo gbBasic = new GbBasicInfo();
                BeanUtil.copyProperties(e, gbBasic);
                gbBasicInfoList.add(gbBasic);
            });
            return gbBasicInfoList;
        }
    }


    @Override
    public void saveInfo(CadreDTO dto) {
        GbBasicInfo entity = this.lambdaQuery().eq(GbBasicInfo::getCardId, dto.getCardId()).last(SqlConstant.ONE_SQL).one();
        if (entity == null) {
            GbBasicInfo info = new GbBasicInfo();
            BeanUtils.copyProperties(dto, info);
            info.setCreateTime(DateUtil.date());
            info.setUpdateTime(DateUtil.date());


            info.setTenantId(dto.getServiceLesseeId());
            info.setCreatorId(dto.getServiceUserId());
            this.save(info);
        }
    }

    @Override
    public List<GbBasicInfo> selectBatchByCardIds(List<String> cardIds) {
        if(CollectionUtil.isEmpty(cardIds)){
            return null;
        }
        List<GbBasicInfo> list = this.lambdaQuery().in(GbBasicInfo::getCardId, cardIds).list();
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void syncData() {
        //删除所有数据重新添加

        List<GbBasicInfo> dataList = new ArrayList<>();
        List<GbBasicInfoThree> gbBasicInfoThrees = gbBasicInfoThreeService.selectGbBasicInfo();
        if(CollectionUtil.isNotEmpty(gbBasicInfoThrees)){
            for (GbBasicInfoThree gbBasicInfoThree : gbBasicInfoThrees) {
                GbBasicInfo info = new GbBasicInfo();
                BeanUtil.copyProperties(gbBasicInfoThree, info);
                info.setCreateTime(DateUtil.date());
                info.setUpdateTime(DateUtil.date());
                String name = gbBasicInfoThree.getName();
                name  = name.replaceAll("[　*| *| *|//s*]*", "");
                info.setName(name);
                dataList.add(info);
            }
            List<SysDictBiz> dictList = sysDictBizService.selectList();
            this.baseMapper.delete(null);
            dataList = this.repalceDictId(dataList,dictList);
            this.saveBatch(dataList);
        }

    }

    @Override
    public List<CadreExcelVO> export(CadreFamilyExportDto dto) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        /**
         *
         * QueryWrapper<GbBasicInfo> queryWrapper = new QueryWrapper<>();
         *         List<GbBasicInfo> list = this.lambdaQuery().eq(GbBasicInfo::getName, name)
         *                 .eq(GbBasicInfo::getUnit, unit)
         *                 .eq(GbBasicInfo::getPost, post).list();
         *         return list;
         */
        //字典转换
        List<CadreExcelVO> list =  this.baseMapper.selectList(new LambdaQueryWrapper<GbBasicInfo>().like(StringUtils.isNotBlank(dto.getName()),GbBasicInfo::getName,dto.getName())
        .like(StringUtils.isNotBlank(dto.getUnit()),GbBasicInfo::getUnit,dto.getUnit()).eq(StringUtils.isNotBlank(dto.getPost_type()),GbBasicInfo::getPostType,dto.getPost_type()).apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.GB_BASIC_INFO,dto.getOrgCode()))
        ).stream().map(t -> {
            CadreExcelVO vo = new CadreExcelVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).collect(Collectors.toList());
        list = this.replaceDictValue(list,dictList);
        return list;
    }

    @Override
    public List<GbOrgInfo> selectGbOrgInfoByCardIds(List<String> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return null;
        }
        return  this.baseMapper.selectByCardIds(ids);
    }

    @Override
    public List<GbOrgInfo> selectByOrgCode(String orgCode) {
        if (StrUtil.isEmpty(orgCode)){
            return null;
        }
        Org org = orgService.selectByOrgancode(orgCode);
        if (Objects.isNull(org)){
            return null;
        }
        String asglevel = org.getAsglevel();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){

        }
        String pathNamecode = org.getAsgpathnamecode();
        return this.baseMapper.selectByPathNameCode(pathNamecode);
    }

    @Override
    public List<GbOrgInfo> selectByOrgCodeAndCardIds(String orgCode, List<String> cardIds) {
        if (StrUtil.isEmpty(orgCode)){
            return null;
        }
        Org org = orgService.selectByOrgancode(orgCode);
        if (Objects.isNull(org)){
            return null;
        }
        String asglevel = org.getAsglevel();
        List<GbOrgInfo> list = new ArrayList<>();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){
            list =  this.baseMapper.selectByCardIds(cardIds);
        }else{
            String pathNamecode = org.getAsgpathnamecode();
            list =this.baseMapper.selectByPathNameCodeAndCardIds(cardIds,pathNamecode);
        }
        if(CollectionUtil.isEmpty(list)){
            return null;
        }
        List<GbOrgInfo> gbOrgInfoList = new ArrayList<>();
        //过滤公司为null的数据
        list = list.stream().filter(e->StrUtil.isNotEmpty(e.getUnit())).collect(Collectors.toList());
        Map<String, List<GbOrgInfo>> map = list.stream().collect(Collectors.groupingBy(go -> go.getCardId()));
        for (Map.Entry<String, List<GbOrgInfo>> m : map.entrySet()) {
            String key = m.getKey();//key 身份证
            List<GbOrgInfo> tempList = m.getValue();
            String gbName = tempList.get(0).getName();//干部姓名
            System.out.println("key:" + m.getKey() + " value:" + m.getValue());
            //同一用户根据单位分组
            Map<String, List<GbOrgInfo>> unitGroupMap = tempList.stream().collect(Collectors.groupingBy(e -> e.getUnit()));
            //查询同一单位下职务类型是否有多个。如果多个返回异常
            for (Map.Entry<String, List<GbOrgInfo>> unitGoup : unitGroupMap.entrySet()) {
                List<GbOrgInfo> unitGroupList = unitGoup.getValue();
                Map<String, List<GbOrgInfo>> postTypeGroupMap = unitGroupList.stream().collect(Collectors.groupingBy(e -> e.getPostType()));
                if (postTypeGroupMap.size() > 1) {//同一单位下 多个职务类型 。
                    throw new RuntimeException(gbName + "同一单位下多个职务类型");
                }
            }
            int unitSize = unitGroupMap.size();
            if(unitSize==1){
                 GbOrgInfo gbOrgInfo = tempList.get(0);
                 String unit = gbOrgInfo.getUnit();
                 //如果有多个职务，则职务类型逗号 隔开
                 List<GbOrgInfo> unitList = unitGroupMap.get(unit);
                 if(unitList.size()>1){
                     //多个职务逗号链接
                     String tempPost = unitList.stream().map(GbOrgInfo::getPost).collect(Collectors.joining(","));
                     gbOrgInfo.setPost(tempPost);
                 }
                gbOrgInfoList.add(gbOrgInfo);
                 continue;
            }
            //当前用户有多个单位 获取当前用户的最高级别单位  asglevel值越小 级别越高
            int  level  = tempList.stream().min(Comparator.comparing(GbOrgInfo::getAsglevel)).get().getAsglevel();
            //取出最高等级的单位
             List<GbOrgInfo> levelList = tempList.stream().filter(e -> level == e.getAsglevel()).collect(Collectors.toList());
             if(CollectionUtil.isNotEmpty(levelList)){
                 //1.非最高等级干部组织信息的组织链是否在最高等级干部组织信息的组织链中，
                 // 如果在 不做任何事，如果不在，将单位和单位编码拼接在最高等级干部组织信息列表的中第一条数据
                 List<GbOrgInfo> otherLevelList = tempList.stream().
                         filter(e -> !levelList.stream().map(GbOrgInfo::getId).collect(Collectors.toList()).contains(e.getId()))
                         .collect(Collectors.toList()); //非最高等级的信息
                 //2.获取组织编码链不在最高等级单位下的数据 默认放入
                 String otherPost = null;
                 String otherUnit = null;
                 String otherUnitCode = null;
                 if(CollectionUtil.isNotEmpty(otherLevelList)){
                     List<GbOrgInfo> temp = new ArrayList<>();
                     List<String> pathCodeList = levelList.stream().map(GbOrgInfo::getAsgpathnamecode).collect(Collectors.toList());
                     otherLevelList.stream().forEach(e->{
                          String asgpathnamecode = e.getAsgpathnamecode();
                          boolean b = pathCodeList.contains(asgpathnamecode);
                         if (!b) {
                             temp.add(e);
                         }
                     });
                     if(CollectionUtil.isNotEmpty(temp)){
                         otherPost = temp.stream().map(e->e.getPost()).collect(Collectors.joining(","));
                         otherUnit = temp.stream().map(e->e.getUnit()).collect(Collectors.joining(","));
                         otherUnitCode = temp.stream().map(e->e.getUnitCode()).collect(Collectors.joining(","));
                     }
                 }

                 //3.将最高等级的单位干部组织信息进行分组，因为会出现一个单位多个部门
                 Map<String, List<GbOrgInfo>> levelListMap = levelList.stream().collect(Collectors.groupingBy(e -> e.getUnit()));
                 int index =1;
                 for (Map.Entry<String, List<GbOrgInfo>> leveMap : levelListMap.entrySet()) {
                      List<GbOrgInfo> valueList = leveMap.getValue();
                      GbOrgInfo levlGbOrg = valueList.get(0);//取第一个。同单位的其他部门的职务进行逗号拼接
                      String tempPost = valueList.stream().map(GbOrgInfo::getPost).collect(Collectors.joining(","));
                      String tempUnit = valueList.stream().map(GbOrgInfo::getUnit).collect(Collectors.joining(","));
                      String tempUnitCode = valueList.stream().map(GbOrgInfo::getUnitCode).collect(Collectors.joining(","));
                      if(index==1&&StrUtil.isNotEmpty(otherPost)){
                          tempPost +=","+otherPost;
                      }
                      levlGbOrg.setPost(tempPost);
                      if(index==1&&StrUtil.isNotEmpty(otherPost)){
                         tempUnit +=","+otherUnit;
                     }
                     levlGbOrg.setUnit(tempUnit);
                     if(index==1&&StrUtil.isNotEmpty(otherPost)){
                         tempUnitCode +=","+otherUnitCode;
                     }
                     levlGbOrg.setUnitCode(tempUnitCode);
                     gbOrgInfoList.add(levlGbOrg);
                 }
             }
        }
        return gbOrgInfoList;
    }

    @Override
    public List<GbOrgInfo> selectByOrgCodeAndGbName(String orgCode,String name) {
        if (StrUtil.isEmpty(orgCode)){
            return null;
        }
        Org org = orgService.selectByOrgancode(orgCode);
        if (Objects.isNull(org)){
            return null;
        }
        String pathNamecode = org.getAsgpathnamecode();
        return this.baseMapper.selectByOrgCodeAndGbName(name,pathNamecode);

    }

    private List<CadreExcelVO> replaceDictValue(List<CadreExcelVO> list, List<SysDictBiz> dictList){
        list.parallelStream().forEach(vo->{
            String postType =sysDictBizService.getDictValue(vo.getPostType(),dictList);
            String allOtType = sysDictBizService.getDictValue(vo.getAllotType(),dictList);
            vo.setPostType(postType);
            vo.setAllotType(allOtType);
        });
        return list;
    }

    private List<GbBasicInfo>  repalceDictId(List<GbBasicInfo> list, List<SysDictBiz> dictList){
        list.parallelStream().forEach(dto->{
            //字典对应项
            String postType = sysDictBizService.getDictId(dto.getPostType(), dictList);
            if(StrUtil.isEmpty(postType)){
                postType = "1560093752752332800";
            }
            dto.setPostType(postType);

        });
        return list;
    }
}
