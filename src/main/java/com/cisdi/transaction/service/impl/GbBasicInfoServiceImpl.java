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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("1")) { //看所有
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
                name =name.replace(" ","");
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
        String orgCode = dto.getOrgCode();
        Org org = orgService.selectByOrgancode(orgCode);
        List<String> cardIds = orgService.getCardIdsByAsgpathnamecode(org.getAsgpathnamecode());
        if (CollectionUtils.isEmpty(cardIds)){
            return Lists.newArrayList();
        }
        //字典转换
        List<CadreExcelVO> list =  this.baseMapper.selectList(new LambdaQueryWrapper<GbBasicInfo>().like(StringUtils.isNotBlank(dto.getName()),GbBasicInfo::getName,dto.getName()).in(GbBasicInfo::getCardId,cardIds)
        .like(StringUtils.isNotBlank(dto.getUnit()),GbBasicInfo::getUnit,dto.getUnit()).eq(StringUtils.isNotBlank(dto.getPost_type()),GbBasicInfo::getPost,dto.getPost_type())
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
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("1")){

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
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("1")){
            list =  this.baseMapper.selectByCardIds(cardIds);
        }else{
            String pathNamecode = org.getAsgpathnamecode();
            list =this.baseMapper.selectByPathNameCodeAndCardIds(cardIds,pathNamecode);
        }
        List<GbOrgInfo> gbOrgInfoList = new ArrayList<>();
        Map<String, List<GbOrgInfo>> map = list.stream().collect(Collectors.groupingBy(go -> go.getCardId() + "_" +go.getUnit() + "_" + go.getPostType()));
        for (Map.Entry<String, List<GbOrgInfo>> m : map.entrySet()) {
            String key = m.getKey();//key 身份证_单位_职务类型
            List<GbOrgInfo> tempList = m.getValue();
            System.out.println("key:" + m.getKey() + " value:" + m.getValue());
            GbOrgInfo gbOrgInfo = tempList.get(0);//默认取第一个值 作为新数据保存
            //将多个职务用都好链接并保存之新数据中
            String newPost = tempList.stream().map(GbOrgInfo::getPost).collect(Collectors.joining(","));
            gbOrgInfo.setPost(newPost);
            gbOrgInfoList.add(gbOrgInfo); //数据中部门id不存在
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

            dto.setPostType(postType);

        });
        return list;
    }
}
