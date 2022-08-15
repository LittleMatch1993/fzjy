package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.mapper.master.PrivateEquityMapper;
import com.cisdi.transaction.service.BanDealInfoService;
import com.cisdi.transaction.service.PrivateEquityService;
import com.cisdi.transaction.service.SpouseBasicInfoService;
import com.cisdi.transaction.service.SysDictBizService;
import com.cisdi.transaction.util.ThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.transform.Result;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况
 *
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:07
 */
@Service
public class PrivateEquityServiceImpl extends ServiceImpl<PrivateEquityMapper, PrivateEquity> implements PrivateEquityService {

    @Autowired
    private SpouseBasicInfoService spouseBasicInfoService;

    @Autowired
    private BanDealInfoService banDealInfoService;

    @Autowired
    private GbBasicInfoServiceImpl gbBasicInfoService;

    @Autowired
    private SysDictBizService sysDictBizService;

    @Override
    public boolean updateState(List<String> ids, String state) {
        List<PrivateEquity> list = ids.stream().map(e -> new PrivateEquity().setId(e).setState(state)).collect(Collectors.toList());
        boolean b = this.updateBatchById(list);
        return b;
    }

    @Override
    public int countByNameAndCardIdAndCode(String name, String cardId, String code) {
        Integer count = this.lambdaQuery().eq(PrivateEquity::getName, name).eq(PrivateEquity::getCardId, cardId).eq(PrivateEquity::getCode, code).count();
        return Objects.isNull(count) ? 0 : count.intValue();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void overrideInvestInfo(String id, PrivateEquityDTO dto) {
        PrivateEquity info = new PrivateEquity();
        BeanUtil.copyProperties(dto,info);
        info.setId(id);
        //校验国家/省份/市
        //checkArea(dto, info);
        info.setState(SystemConstant.SAVE_STATE);
        info.setCreateTime(DateUtil.date());
        info.setUpdateTime(DateUtil.date());
        info.setTenantId(dto.getServiceLesseeId());
        info.setCreatorId(dto.getServiceUserId());

        info.setCreateAccount(dto.getServiceUserAccount());
        info.setCreateName(dto.getServiceUserName());
        info.setOrgCode(dto.getServiceLesseeId());
        info.setOrgName(dto.getServiceLesseeName());
        this.save(info);
    }

    @Override
    public PrivateEquity getRepeatInvestInfo(String name, String cardId, String code) {
        PrivateEquity one = this.lambdaQuery().eq(PrivateEquity::getName, name)
                .eq(PrivateEquity::getCardId, cardId)
                .eq(PrivateEquity::getCode, code)
                .last(SqlConstant.ONE_SQL).one();
        return one;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMsgUtil<String> submitPrivateEquity(SubmitDto subDto) {
        String resutStr = "提交成功";
        List<String> ids = subDto.getIds();
        List<PrivateEquity> infoList = this.lambdaQuery().in(PrivateEquity::getId, ids).list();
        if (CollectionUtil.isEmpty(infoList)) {
            return ResultMsgUtil.failure("数据不存在了");
        }
        long count = infoList.stream().filter(e -> SystemConstant.VALID_STATE.equals(e.getState())).count();
        if (count > 0) {
            return ResultMsgUtil.failure("当前表中的有效数据不能重复提交到禁止交易信息表中!");
        }

       // boolean b = this.updateState(ids, SystemConstant.VALID_STATE);
        boolean b = true;

        if (b) { //配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况 表数据改为有效状态 并且修改成功 往 配偶，子女及其配偶表中添加数据。
            // 配偶，子女及其配偶表中添加数据。如果 干部身份证号 姓名 称谓 重复则不添加
            List<SpouseBasicInfo> sbInfoList = spouseBasicInfoService.selectAll();//查询所有干部家属信息
            List<SpouseBasicInfo> sbiList = new ArrayList<>();
            for (PrivateEquity info : infoList) {
                String cardId = info.getCardId();
                String name = info.getName();
                String title = info.getTitle();
                int i = spouseBasicInfoService.selectCount(cardId, name, title, sbInfoList);
                if (i > 0) { //i>0 说明当前数据重复了
                    continue;
                }
                SpouseBasicInfo temp = new SpouseBasicInfo();
                temp.setCreateTime(DateUtil.date());
                temp.setUpdateTime(DateUtil.date());
                temp.setCadreName(info.getGbName());
                temp.setCadreCardId(cardId);
                temp.setName(name);
                temp.setTitle(title);
                sbiList.add(temp);
            }
            if (CollectionUtil.isNotEmpty(sbiList)) {
                //添加干部配偶，子女及其配偶数据
                try {
                    spouseBasicInfoService.saveBatch(sbiList);
                }catch (Exception e){
                    e.printStackTrace();
                    return ResultMsgUtil.failure("添加家属信息失败");
                }
            }

            banDealInfoService.deleteBanDealInfoByRefId(ids);
            //获取干部的基本信息
            //List<String> cardIds = infoList.stream().map(PrivateEquity::getCardId).collect(Collectors.toList());
           // List<GbBasicInfo> gbList = gbBasicInfoService.selectBatchByCardIds(cardIds);
            //获取干部组织的基本信息
            List<GbOrgInfo> gbOrgList = null;
            try {
                //gbOrgList = gbBasicInfoService.selectGbOrgInfoByCardIds(cardIds);
                String orgCode = subDto.getOrgCode();
                gbOrgList = gbBasicInfoService.selectByOrgode(orgCode);
            }catch (Exception e){
                e.printStackTrace();
                this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("干部组织信息查询失败");
            }
            if(CollectionUtil.isEmpty(gbOrgList)){
                this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("没有找到干部组织信息");
            }
            //向禁止交易信息表中添加数据 并进行验证 及其他逻辑处理
            ResultMsgUtil<Map<String, Object>> mapResult = banDealInfoService.insertBanDealInfoOfPrivateEquity(infoList, gbOrgList);
            //处理提交数据后
            Map<String, Object> data = mapResult.getData();
            String banDeal = data.get("banDeal").toString();
            List<String> submitIds = (List<String>)data.get("submitIds");
            StringJoiner sj = new StringJoiner(",");
            if(CollectionUtil.isNotEmpty(submitIds)){
                this.updateState(submitIds,SystemConstant.VALID_STATE);
            }
            if(!Boolean.valueOf(banDeal)){
                sj.add("提交数据失败");
            }else{
                sj.add("提交数据成功");
                if(CollectionUtil.isNotEmpty(submitIds)){
                    int beferIndex = infoList.size();
                    int afterIndex = submitIds.size();
                    sj.add(",其中"+(beferIndex-afterIndex)+"数据提交失败");
                }
            }
            resutStr = sj.toString();
        }
        return ResultMsgUtil.success(resutStr);
    }

    @Override
    public void savePrivateEquity(PrivateEquityDTO dto) {
      /*  PrivateEquity one = null;
        if (dto.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
            if (StringUtils.isNotBlank(dto.getName()) && StringUtils.isNotBlank(dto.getCode())) {
                one = this.lambdaQuery().eq(PrivateEquity::getCardId, dto.getCardId()).eq(PrivateEquity::getName, dto.getName())
                        .eq(PrivateEquity::getCode, dto.getCode()).last(SqlConstant.ONE_SQL).one();
            } else {
                throw new RuntimeException("有此类情况下，请填写完整");
            }
        } else {
            one = this.lambdaQuery().eq(PrivateEquity::getCardId, dto.getCardId()).eq(PrivateEquity::getIsRelation, SystemConstant.IS_SITUATION_NO)
                    .last(SqlConstant.ONE_SQL).one();
        }*/
        PrivateEquity equity = new PrivateEquity();
        BeanUtil.copyProperties(dto,equity,new String[]{"id"});
        equity.setState(SystemConstant.SAVE_STATE);
        equity.setCreateTime(DateUtil.date());
        equity.setUpdateTime(DateUtil.date());
        equity.setTenantId(dto.getServiceLesseeId());
        equity.setCreatorId(dto.getServiceUserId());
        equity.setCreateAccount(dto.getServiceUserAccount());
        equity.setCreateName(dto.getServiceUserName());
        equity.setOrgCode(dto.getServiceLesseeId());
        equity.setOrgName(dto.getServiceLesseeName());
        //新增
        this.save(equity);
      /*  if (one == null) {
            //新增
            this.save(equity);
        } else {
            //覆盖
            equity.setId(one.getId());
            this.updateById(equity);
        }*/
    }

    @Override
    public void editPrivateEquity(PrivateEquityDTO dto) {
        PrivateEquity equity = new PrivateEquity();
        BeanUtil.copyProperties(dto, equity);
        equity.setState(SystemConstant.SAVE_STATE);
        equity.setUpdateTime(DateUtil.date());
        equity.setUpdaterId(dto.getServiceUserId());
        this.updateById(equity);
    }

    @Override
    public void saveBatchInvestmentInfo(List<EquityFundsDTO> list) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<PrivateEquity> privateEquity = new ArrayList<>();
        List<String> cardIds = list.stream().distinct().map(t -> t.getCardId()).collect(Collectors.toList());
        List<PrivateEquity> infoList = this.lambdaQuery().in(PrivateEquity::getCardId, cardIds).list();
        if (infoList.isEmpty()) {
            list.stream().forEach(t -> {
                if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        PrivateEquity investInfo = new PrivateEquity();
                        BeanUtils.copyProperties(t, investInfo);
                        investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(new Date())
                                .setUpdateTime(new Date());
                        //字典替换
                        investInfo = this.replaceDictId(investInfo,dictList);
                        privateEquity.add(investInfo);
                    }
                } else {
                    PrivateEquity investInfo = new PrivateEquity();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
                    //字典替换
                    investInfo = this.replaceDictId(investInfo,dictList);
                    privateEquity.add(investInfo);
                }
            });
            if (!privateEquity.isEmpty()) {
                this.saveBatch(privateEquity);
            }
            return;
        }
        List<PrivateEquity> updateList = new ArrayList<>();
        Map<String, List<PrivateEquity>> infoMap = infoList.stream().collect(Collectors.groupingBy(PrivateEquity::getCardId));
        list.stream().forEach(t -> {
            //List<PrivateEquity> infos = infoMap.get(t.getCardId());
            List<PrivateEquity> infos = infoMap.containsKey(t.getCardId())?infoMap.get(t.getCardId()):null;
            if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                if (CollectionUtil.isNotEmpty(infos)) {//如果不为空，进行比较
                    //有此类情况
                    //校验姓名和统一社会信用代码不能为空
                    if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        PrivateEquity info = new PrivateEquity();
                        BeanUtils.copyProperties(t, info);
                        info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setUpdateTime(DateUtil.date());
                        //判断该干部下的其他子项名称和代码是否相同
                        long nameIndex = infos.stream().filter(e->t.getName().equals(e.getName())).count();
                        long titleIndex = infos.stream().filter(e->sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())).count();
                        long codeIndex = infos.stream().filter(e->t.getCode().equals(e.getCode())).count();
                        info = this.replaceDictId(info,dictList);
                        if(nameIndex==0|titleIndex==0|codeIndex==0){ //一个都不重复
                            //如果不相同，新增，否则就是覆盖
                            info.setCreateTime(DateUtil.date());
                            privateEquity.add(info);
                        }else{ //有重复数据了
                            PrivateEquity existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
                                    &&sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())
                                    &&t.getCode().equals(e.getCode())).findAny().orElse(null);
                            if(Objects.nonNull(existInfo)){
                                info.setId(existInfo.getId());
                                updateList.add(info);
                            }
                        }
                        /*if (!t.getName().equals(e.getName()) &&!info.getTitle().equals(e.getTitle())&& !t.getCode().equals(e.getCode())) {
                            //如果不相同，新增，否则就是覆盖
                            info.setCreateTime(DateUtil.date());
                            investInfoList.add(info);
                        } else {
                            info.setId(e.getId());
                            updateList.add(info);
                        }*/
                    }
                    /*infos.stream().forEach(e -> {
                        //判断该干部下的其他子项名称和代码是否相同，不相同则添加数据库
                        if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getTitle()) && StringUtils.isNotBlank(t.getRegistrationNumber())) {
                            PrivateEquity info = new PrivateEquity();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(new Date());
                            //字典替换
                            info = this.replaceDictId(info,dictList);
                            if (!t.getName().equals(e.getName()) || !info.getTitle().equals(e.getTitle())||!t.getRegistrationNumber().equals(e.getRegistrationNumber())) {
                                info.setCreateTime(new Date());
                                privateEquity.add(info);
                            } else {
                                info.setId(e.getId());
                                updateList.add(info);
                            }
                        }
                    });*/
                } else {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //数据库为空，直接add
                        PrivateEquity info = new PrivateEquity();
                        BeanUtils.copyProperties(t, info);
                        info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(new Date())
                                .setUpdateTime(new Date());
                        //字典替换
                        info = this.replaceDictId(info,dictList);
                        privateEquity.add(info);
                    }
                }
            } else {
                //说明无此类情况
                PrivateEquity info = new PrivateEquity();
                BeanUtils.copyProperties(t, info);
                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setUpdateTime(new Date());
                //字典替换
                info = this.replaceDictId(info,dictList);
                // 数据库中如果不存在数据
                if (CollectionUtil.isEmpty(infos)) {
                    info.setCreateTime(new Date());
                    privateEquity.add(info);//可添加到数据库中
                } else {
                    info.setId(infos.get(0).getId());
                    updateList.add(info);
                }
            }

        });
        if (!privateEquity.isEmpty()) {
            this.saveBatch(privateEquity);
        }
        if (!updateList.isEmpty()) {
            this.updateBatchById(updateList);
        }
    }

    @Override
    public List<EquityFundsDTO> exportEquityFundsExcel(List<String> ids) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<EquityFundsDTO> list =  this.lambdaQuery().in(PrivateEquity::getId,ids).list().stream().map(t -> {
            EquityFundsDTO dto = new EquityFundsDTO();
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
        list  = this.replaceDictValue(list,dictList);
        return list;
    }

    private List<EquityFundsDTO> replaceDictValue(List<EquityFundsDTO> list,List<SysDictBiz> dictList){
            list.parallelStream().forEach(t->{
                System.out.println("123--"+t.getTitle());
                //字典对应项
                String isSituation =sysDictBizService.getDictValue(t.getIsSituation(),dictList);
                String title =sysDictBizService.getDictValue(t.getTitle(),dictList);
                String controller =sysDictBizService.getDictValue(t.getController(),dictList);
                String shareholder =sysDictBizService.getDictValue(t.getShareholder(),dictList);
                String practice =sysDictBizService.getDictValue(t.getPractice(),dictList);
                String isRelation =sysDictBizService.getDictValue(t.getIsRelation(),dictList);
                System.out.println("--"+title);
                t.setIsSituation(isSituation);
                t.setTitle(title);
                t.setController(controller);
                t.setShareholder(shareholder);
                t.setPractice(practice);
                t.setIsRelation(isRelation);
            });
         return list;
    }
    private PrivateEquity replaceDictId(PrivateEquity t,List<SysDictBiz> dictList){

        System.out.println("123--"+t.getTitle());
        //字典对应项
        String isSituation =sysDictBizService.getDictId(t.getIsSituation(),dictList);
        String title =sysDictBizService.getDictId(t.getTitle(),dictList);
        String controller =sysDictBizService.getDictId(t.getController(),dictList);
        String shareholder =sysDictBizService.getDictId(t.getShareholder(),dictList);
        String practice =sysDictBizService.getDictId(t.getPractice(),dictList);
        String isRelation =sysDictBizService.getDictId(t.getIsRelation(),dictList);
        System.out.println("--"+title);
        t.setIsSituation(isSituation);
        t.setTitle(title);
        t.setController(controller);
        t.setShareholder(shareholder);
        t.setPractice(practice);
        t.setIsRelation(isRelation);
        return t;
    }
}
