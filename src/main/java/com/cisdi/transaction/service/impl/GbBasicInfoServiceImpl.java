package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.exception.BusinessException;
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
    public List<GbBasicInfo> selectGbInfoByNameAndUnitAndPost(String name,String unit,String post,String cardId) {
        QueryWrapper<GbBasicInfo> queryWrapper = new QueryWrapper<>();
        List<GbBasicInfo> list = this.lambdaQuery().eq(GbBasicInfo::getName, name)
                .eq(GbBasicInfo::getUnit, unit)
                .eq(GbBasicInfo::getPost, post).eq(StrUtil.isNotEmpty(cardId),GbBasicInfo::getCardId,cardId).list();
        return list;
    }

    @Override
    public List<GbBasicInfo> selectGbDictVoByName(String name, String orgCode) {
        List<GbBasicInfo> list =  new ArrayList<>();
        if(StrUtil.isEmpty(orgCode)){
            return list;
        }
        List<String> orgCodeList = new ArrayList<String>(Arrays.asList(orgCode.split(",")));

        List<Org> orgs = orgService.selectByOrgancode(orgCodeList);
        if (CollectionUtil.isEmpty(orgs)){
            return list;
        }
        String asglevel = orgService.getHighestLevel(orgs);
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")) { //?????????
            QueryWrapper<GbBasicInfo> queryWrapper = new QueryWrapper();
            if(StrUtil.isNotEmpty(name)){ //????????????
                queryWrapper.lambda().like(GbBasicInfo::getName, name);
            }else{
                queryWrapper.lambda().last(SqlConstant.ONE_SQL_YB);
            }
           return this.baseMapper.selectList(queryWrapper);
        }else{ //?????????????????????
            List<String> pathNameCode = orgs.stream().map(Org::getAsgpathnamecode).collect(Collectors.toList());  //org.getAsgpathnamecode();
            List<GbOrgInfo> gbOrgInfoList = null;
            if(StrUtil.isNotEmpty(name)){ //????????????
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
        //??????????????????????????????

        List<GbBasicInfo> dataList = new ArrayList<>();
        List<GbBasicInfoThree> gbBasicInfoThrees = gbBasicInfoThreeService.selectGbBasicInfo();
        if(CollectionUtil.isNotEmpty(gbBasicInfoThrees)){
            long dateLong = DateUtil.date().getTime();
            int i = 0;
            for (GbBasicInfoThree gbBasicInfoThree : gbBasicInfoThrees) {
                String name = gbBasicInfoThree.getName();
                name  = name.replaceAll("[???*| *| *|//s*]*", "");
                gbBasicInfoThree.setName(name);
                GbBasicInfo info = new GbBasicInfo();
                BeanUtil.copyProperties(gbBasicInfoThree, info);
                DateTime date = DateUtil.date(dateLong + (i*1000));
                info.setCreateTime(date);
                info.setUpdateTime(date);
                dataList.add(info);
                i++;
            }
            List<SysDictBiz> dictList = sysDictBizService.selectList();
            this.baseMapper.delete(null);
            dataList = this.repalceDictId(dataList,dictList);
            this.saveBatch(dataList);
        }

    }

    @Override
    public List<GbBasicInfoThree> selectOldGbBasicInfo() {
        return gbBasicInfoThreeService.selectOldGbBasicInfo();
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
        //????????????
        List<CadreExcelVO> list =  this.baseMapper.selectList(new QueryWrapper<GbBasicInfo>()
                        .orderBy(StringUtils.isNotBlank(dto.getColumnName())&&Objects.nonNull(dto.getIsAsc()),dto.getIsAsc(),dto.getColumnName())
                        .orderByDesc(StringUtils.isBlank(dto.getColumnName())||Objects.isNull(dto.getIsAsc()),"create_time")
                        .lambda().like(StringUtils.isNotBlank(dto.getName()),GbBasicInfo::getName,dto.getName())
        .like(StringUtils.isNotBlank(dto.getUnit()),GbBasicInfo::getUnit,dto.getUnit()).in(CollectionUtil.isNotEmpty(dto.getPost_type()),GbBasicInfo::getPostType,dto.getPost_type()).apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.GB_BASIC_INFO,dto.getOrgCode()))
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
        List<String> orgCodeList = new ArrayList<String>(Arrays.asList(orgCode.split(",")));

        List<Org> orgs = orgService.selectByOrgancode(orgCodeList);
        if (CollectionUtil.isEmpty(orgs)){
            return null;
        }
        String asglevel = orgService.getHighestLevel(orgs);

        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){

        }
        List<String> pathNamecode = orgs.stream().map(Org::getAsgpathnamecode).collect(Collectors.toList());
        return this.baseMapper.selectByPathNameCode(pathNamecode);
    }

    @Override
    public List<GbOrgInfo> selectByOrgCodeAndCardIds(String orgCode, List<String> cardIds) {
        if (StrUtil.isEmpty(orgCode)){
            return null;
        }

        List<String> orgCodeList = new ArrayList<String>(Arrays.asList(orgCode.split(",")));

        List<Org> orgs = orgService.selectByOrgancode(orgCodeList);
        if (CollectionUtil.isEmpty(orgs)){
            return null;
        }
        String asglevel = orgService.getHighestLevel(orgs);
        List<GbOrgInfo> list = new ArrayList<>();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){
            list =  this.baseMapper.selectByCardIds(cardIds);
        }else{
            List<String> pathNamecode = orgs.stream().map(Org::getAsgpathnamecode).collect(Collectors.toList());
            list =this.baseMapper.selectByPathNameCodeAndCardIds(cardIds,pathNamecode);
        }
        if(CollectionUtil.isEmpty(list)){
            return null;
        }
        List<GbOrgInfo> gbOrgInfoList = new ArrayList<>();
        //????????????????????????????????????
        List<GbBasicInfo> gbBasicInfoList = this.lambdaQuery().in(GbBasicInfo::getCardId, cardIds).list();
        //???????????????null?????????
        list = list.stream().filter(e->StrUtil.isNotEmpty(e.getUnit())).collect(Collectors.toList());
        if(CollectionUtil.isNotEmpty(gbBasicInfoList)){
            //???id??????
            Map<String, List<GbBasicInfo>> map = gbBasicInfoList.stream().collect(Collectors.groupingBy(GbBasicInfo::getId));
            list.stream().forEach(e->{
                 String id = e.getId();
                 boolean b = map.containsKey(id);
                 if(b){
                     List<GbBasicInfo> gbBasicInfos = map.get(id);
                     if(CollectionUtil.isNotEmpty(gbBasicInfos)){
                          GbBasicInfo gb = gbBasicInfos.get(0);
                          e.setDeparment(gb.getDepartment());
                     }
                 }
            });
        }
        Map<String, List<GbOrgInfo>> map = list.stream().collect(Collectors.groupingBy(go -> go.getCardId()));
        for (Map.Entry<String, List<GbOrgInfo>> m : map.entrySet()) {
            String key = m.getKey();//key ?????????
            List<GbOrgInfo> tempList = m.getValue();
            String gbName = tempList.get(0).getName();//????????????
            System.out.println("key:" + m.getKey() + " value:" + m.getValue());
            //??????????????????????????????
            Map<String, List<GbOrgInfo>> unitGroupMap = tempList.stream().collect(Collectors.groupingBy(e -> e.getUnit()));
            //???????????????????????????????????????????????????????????????????????????

            for (Map.Entry<String, List<GbOrgInfo>> unitGoup : unitGroupMap.entrySet()) {
                List<GbOrgInfo> unitGroupList = unitGoup.getValue();
                Map<String, List<GbOrgInfo>> postTypeGroupMap = unitGroupList.stream().collect(Collectors.groupingBy(e -> e.getPostType()));
                if (postTypeGroupMap.size() > 1) {//??????????????? ?????????????????? ???
                    throw new BusinessException(gbName + "?????????????????????????????????");
                }
            }
            int unitSize = unitGroupMap.size();
            if(unitSize==1){
                 GbOrgInfo gbOrgInfo = tempList.get(0);
                 String unit = gbOrgInfo.getUnit();
                 //????????????????????????????????????????????? ??????
                 List<GbOrgInfo> unitList = unitGroupMap.get(unit);
                 if(unitList.size()>1){
                     //????????????????????????
                     String tempPost = unitList.stream().filter(e->StrUtil.isNotEmpty(e.getPost())).map(GbOrgInfo::getPost).collect(Collectors.joining(","));
                     gbOrgInfo.setPost(tempPost);
                 }
                gbOrgInfoList.add(gbOrgInfo);
                 continue;
            }
            //??????????????????????????? ???????????????????????????????????????  asglevel????????? ????????????
            int  level  = tempList.stream().min(Comparator.comparing(GbOrgInfo::getAsglevel)).get().getAsglevel();
            //???????????????????????????
             List<GbOrgInfo> levelList = tempList.stream().filter(e -> level == e.getAsglevel()).collect(Collectors.toList());
             if(CollectionUtil.isNotEmpty(levelList)){
                 //1.??????????????????????????????????????????????????????????????????????????????????????????????????????
                 // ????????? ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                 List<GbOrgInfo> otherLevelList = tempList.stream().
                         filter(e -> !levelList.stream().map(GbOrgInfo::getId).collect(Collectors.toList()).contains(e.getId()))
                         .collect(Collectors.toList()); //????????????????????????
                 //2.????????????????????????????????????????????????????????? ????????????
                 String otherPost = null;
                 String otherUnit = null;
                 String otherUnitCode = null;
                 if(CollectionUtil.isNotEmpty(otherLevelList)){
                     List<GbOrgInfo> temp = new ArrayList<>(); //???????????????????????????????????????????????????
                     //???????????????????????????code??????
                     List<String> pathCodeList = levelList.stream().filter(e->StrUtil.isNotEmpty(e.getAsgpathnamecode())).map(GbOrgInfo::getAsgpathnamecode).collect(Collectors.toList());
                     if(CollectionUtil.isEmpty(pathCodeList)){
                         throw new BusinessException(gbName + "??????????????????????????????code?????????");
                     }
                     otherLevelList.stream().forEach(e->{
                         String asgpathnamecode = e.getAsgpathnamecode();//????????????????????????code?????????
                         //????????????????????????code????????? ??? ???????????????????????????code?????????
                         //boolean b = pathCodeList.contains(asgpathnamecode);
                         boolean b = pathCodeList.stream().anyMatch(a->asgpathnamecode.startsWith(a));
                         if (!b) {
                             temp.add(e);
                         }
                     });
                     if(CollectionUtil.isNotEmpty(temp)){
                         otherPost = temp.stream().filter(e->StrUtil.isNotEmpty(e.getPost())).map(e->e.getPost()).collect(Collectors.joining(","));
                         otherUnit = temp.stream().filter(e->StrUtil.isNotEmpty(e.getUnit())).map(e->e.getUnit()).collect(Collectors.joining(","));
                         otherUnitCode = temp.stream().filter(e->StrUtil.isNotEmpty(e.getUnitCode())).map(e->e.getUnitCode()).collect(Collectors.joining(","));
                     }
                 }

                 //3.????????????????????????????????????????????????????????????????????????????????????????????????
                 Map<String, List<GbOrgInfo>> levelListMap = levelList.stream().collect(Collectors.groupingBy(e -> e.getUnit()));
                 int index =1;
                 for (Map.Entry<String, List<GbOrgInfo>> leveMap : levelListMap.entrySet()) {
                      List<GbOrgInfo> valueList = leveMap.getValue();
                      GbOrgInfo levlGbOrg = valueList.get(0);//??????????????????????????????????????????????????????????????????
                      String tempPost = valueList.stream().map(GbOrgInfo::getPost).collect(Collectors.joining(","));
                      String tempUnit = valueList.stream().map(GbOrgInfo::getUnit).collect(Collectors.joining(","));
                      String tempUnitCode = valueList.stream().map(GbOrgInfo::getUnitCode).collect(Collectors.joining(","));
                      if(index==1&&StrUtil.isNotEmpty(otherPost)){
                          if(StrUtil.isNotEmpty(tempPost)){
                              tempPost +=","+otherPost;
                          }else{
                              tempPost =otherPost;

                          }
                      }
                      levlGbOrg.setPost(tempPost);
                      if(index==1&&StrUtil.isNotEmpty(otherUnit)){
                          if(StrUtil.isNotEmpty(tempUnit)){
                              tempUnit +=","+otherUnit;
                          }else{
                              tempUnit =otherUnit;
                          }

                     }
                     levlGbOrg.setUnit(tempUnit);
                     if(index==1&&StrUtil.isNotEmpty(otherUnitCode)){
                         if(StrUtil.isNotEmpty(tempUnitCode)){
                             tempUnitCode +=","+otherUnitCode;
                         }else{
                             tempUnitCode =otherUnitCode;
                         }
                     }
                     levlGbOrg.setUnitCode(tempUnitCode);
                     gbOrgInfoList.add(levlGbOrg);
                     index++;
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
        List<String> orgCodeList = new ArrayList<String>(Arrays.asList(orgCode.split(",")));

        List<Org> orgs = orgService.selectByOrgancode(orgCodeList);
        if (CollectionUtil.isEmpty(orgs)){
            return null;
        }
        List<String> pathNamecode =orgs.stream().map(Org::getAsgpathnamecode).collect(Collectors.toList());  //org.getAsgpathnamecode();
        return this.baseMapper.selectByOrgCodeAndGbName(name,pathNamecode);

    }

    @Override
    public List<String> selectNoAuthCardIds(String orgCode) {

        List<String> list =  this.baseMapper.selectList(new LambdaQueryWrapper<GbBasicInfo>().apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.GB_BASIC_INFO,orgCode))
        ).stream().map(GbBasicInfo::getCardId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)){
            return this.list().stream().map(GbBasicInfo::getCardId).collect(Collectors.toList());
        }
        return this.baseMapper.selectList(new LambdaQueryWrapper<GbBasicInfo>().notIn(GbBasicInfo::getCardId,list)).stream().map(GbBasicInfo::getCardId).collect(Collectors.toList());
    }

    @Override
    public List<AuthUser> selectAuthUser() {
        return this.baseMapper.selectAuthUser();
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
            //???????????????
            String postType = sysDictBizService.getDictId(dto.getPostType(), dictList);
            if(StrUtil.isEmpty(postType)){
                postType = "1560093752752332800";
            }
            dto.setPostType(postType);

        });
        return list;
    }
}
