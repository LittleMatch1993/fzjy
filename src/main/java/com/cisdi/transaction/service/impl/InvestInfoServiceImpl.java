package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.utils.NumberUtils;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.ExportReturnMessageVO;
import com.cisdi.transaction.domain.vo.ExportReturnVO;
import com.cisdi.transaction.domain.vo.KVVO;
import com.cisdi.transaction.mapper.master.InvestInfoMapper;
import com.cisdi.transaction.service.*;
import com.cisdi.transaction.util.ThreadLocalUtils;
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
 * 投资企业或担任高级职务情况
 *
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:17
 */
@Service
public class InvestInfoServiceImpl extends ServiceImpl<InvestInfoMapper, InvestInfo> implements InvestInfoService {

    @Autowired
    private SpouseBasicInfoService spouseBasicInfoService;

    @Autowired
    private BanDealInfoService banDealInfoService;

    @Autowired
    private GbBasicInfoService gbBasicInfoService;

    @Autowired
    private GlobalCityInfoService globalCityInfoService;

    @Autowired
    private SysDictBizService sysDictBizService;

    @Transactional
    @Override
    public boolean updateState(List<String> ids, String state) {
        List<PrivateEquity> list = ids.stream().map(e -> new PrivateEquity().setId(e).setState(state)).collect(Collectors.toList());
        UpdateWrapper<InvestInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(InvestInfo::getState,state).in(InvestInfo::getId,ids);

        boolean b = this.update(updateWrapper);
        return b;
    }

    @Override
    public int updateTips(List<KVVO> kvList) {
        int i = this.baseMapper.updateTips(kvList);
        return i;
    }

    @Override
    public boolean updateBathTips(List<KVVO> kvList) {
        if(CollectionUtil.isEmpty(kvList)){
            return false;
        }
        String tips = kvList.get(0).getName();
        List<String> ids = kvList.stream().map(e -> e.getId()).collect(Collectors.toList());
        UpdateWrapper<InvestInfo> updateWrapper  = new UpdateWrapper<>();
        updateWrapper.lambda().set(InvestInfo::getTips,tips)
                .in(InvestInfo::getId,ids);
        boolean b = this.update(updateWrapper);
        return b;
    }

    @Override
    public int countByNameAndCardIdAndCode(String name, String cardId, String code) {
        Integer count = this.lambdaQuery().eq(InvestInfo::getName, name).eq(InvestInfo::getCardId, cardId).eq(InvestInfo::getCode, code).count();
        return Objects.isNull(count) ? 0 : count.intValue();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMsgUtil<String> submitInvestInfo(SubmitDto subDto) {
        String resutStr = "提交成功";
        List<String> ids = subDto.getIds();
        List<InvestInfo> infoList = this.lambdaQuery().in(InvestInfo::getId, ids).list();
        if (CollectionUtil.isEmpty(infoList)) {
            return ResultMsgUtil.failure("数据不存在了");
        }
        long count = infoList.stream().filter(e -> SystemConstant.VALID_STATE.equals(e.getState())).count();
        if (count > 0) {
            return ResultMsgUtil.failure("当前表中的有效数据不能重复提交到禁止交易信息表中!");
        }
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        long j = infoList.stream().filter(e -> "无此类情况".equals(sysDictBizService.getDictValue(e.getIsSituation(),dictList))).count();
        if (j > 0) {
            return ResultMsgUtil.failure("当前表中的无此类情况数据不能提交到禁止交易信息表中!");
        }
        //boolean b = this.updateState(ids, SystemConstant.VALID_STATE);
        boolean b = true;

        if (b) { //投资企业或担任高级职务情况 表数据改为有效状态 并且修改成功 往 配偶，子女及其配偶表中添加数据。
            // 配偶，子女及其配偶表中添加数据。如果 干部身份证号 姓名 称谓 重复则不添加
            List<SpouseBasicInfo> sbInfoList = spouseBasicInfoService.selectAll();//查询所有干部家属信息
            List<SpouseBasicInfo> sbiList = new ArrayList<>();
           // List<String> tempList = new ArrayList<>();// 存储无此类情况的数据

            for (InvestInfo info : infoList) {
                //无此类情况不提交数据
                String isSitution = info.getIsSituation();
                /*if("无此类情况".equals(sysDictBizService.getDictValue(isSitution,dictList))){
                    tempList.add(info.getId());
                    continue;
                }*/
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
                   // this.updateState(ids, SystemConstant.SAVE_STATE);
                    return ResultMsgUtil.failure("添加家属信息失败");
                }
            }
            //获取干部的基本信息
            List<String> cardIds = infoList.stream().map(InvestInfo::getCardId).collect(Collectors.toList());
           // List<GbBasicInfo> gbList = gbBasicInfoService.selectBatchByCardIds(cardIds);
            //获取干部组织的基本信息
            List<GbOrgInfo> gbOrgList = null;
            try {
                //gbOrgList = gbBasicInfoService.selectGbOrgInfoByCardIds(cardIds);
                String orgCode = subDto.getOrgCode();
                gbOrgList = gbBasicInfoService.selectByOrgCodeAndCardIds(orgCode,cardIds);
            }catch (Exception e){
                e.printStackTrace();
                return ResultMsgUtil.failure("干部组织信息查询失败");
            }
            if(CollectionUtil.isEmpty(gbOrgList)){
                //this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("没有找到干部组织信息");
            }
            /*if(CollectionUtil.isNotEmpty(tempList)){
                infoList = infoList.stream().filter(e->tempList.contains(e.getId())).collect(Collectors.toList());
                ids = infoList.stream().map(InvestInfo::getCardId).collect(Collectors.toList());
            }*/
            banDealInfoService.deleteBanDealInfoByRefId(ids);
            //向禁止交易信息表中添加数据 并进行验证 及其他逻辑处理
            ResultMsgUtil<Map<String, Object>> mapResult = banDealInfoService.insertBanDealInfoOfInvestInfo(infoList, gbOrgList);
            //处理提交数据后
            Map<String, Object> data = mapResult.getData();
            String banDeal = data.get("banDeal").toString();
            List<String> submitIds = (List<String>)data.get("submitIds");
            List<KVVO> submitFailId = (List<KVVO>)data.get("submitFailId"); //无法提交的数据id
            StringJoiner sj = new StringJoiner(",");
            if(CollectionUtil.isNotEmpty(submitFailId)){
                this.updateBathTips(submitFailId);
            }
            if(!Boolean.valueOf(banDeal)){
                sj.add("提交数据失败");
                return ResultMsgUtil.failure(sj.toString());
            }else{
                sj.add("提交数据成功");
                if(CollectionUtil.isNotEmpty(submitIds)){
                    this.updateState(submitIds,SystemConstant.VALID_STATE);
                    int beferIndex = infoList.size();
                    int afterIndex = submitIds.size();
                    int index = beferIndex-afterIndex;
                    if(index>0){
                        sj.add(",其中"+index+"条数据提交失败");
                    }

                }
            }
            resutStr = sj.toString();
        }
        return ResultMsgUtil.success(resutStr);
    }

    @Override
    public void saveInvestInfo(InvestInfoDTO dto) {
       /* InvestInfo one = null;
        if (dto.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
            if (StringUtils.isNotBlank(dto.getName()) && StringUtils.isNotBlank(dto.getCode())) {
                one = this.lambdaQuery().eq(InvestInfo::getCardId, dto.getCardId()).eq(InvestInfo::getName, dto.getName())
                        .eq(InvestInfo::getCode, dto.getCode()).last(SqlConstant.ONE_SQL).one();
            } else {
                throw new RuntimeException("有此类情况下，请填写完整");
            }
        } else {
            one = this.lambdaQuery().eq(InvestInfo::getCardId, dto.getCardId()).eq(InvestInfo::getIsRelation, SystemConstant.IS_SITUATION_NO)
                    .last(SqlConstant.ONE_SQL).one();
        }*/
        InvestInfo info = new InvestInfo();
        BeanUtil.copyProperties(dto, info, new String[]{"id"});
        //校验国家/省份/市
        checkArea(dto, info);
        info.setState(SystemConstant.SAVE_STATE);
        info.setCreateTime(DateUtil.date());
        info.setUpdateTime(DateUtil.date());
        info.setTenantId(dto.getServiceLesseeId());
        info.setCreatorId(dto.getServiceUserId());
        info.setCreateAccount(dto.getServiceUserAccount());
        info.setCreateName(dto.getServiceUserName());
        info.setOrgCode(dto.getOrgCode());
        info.setOrgName(dto.getOrgName());
        info = this.valid(info);
        //新增
        this.save(info);
    }

    @Override
    public void overrideInvestInfo(String id ,InvestInfoDTO dto) {
        InvestInfo info = new InvestInfo();
        BeanUtil.copyProperties(dto,info);
        info.setId(id);
        //校验国家/省份/市
        checkArea(dto, info);
        info.setState(SystemConstant.SAVE_STATE);
        info.setCreateTime(DateUtil.date());
        info.setUpdateTime(DateUtil.date());
        info.setTenantId(dto.getServiceLesseeId());
        info.setCreatorId(dto.getServiceUserId());

        info.setCreateAccount(dto.getServiceUserAccount());
        info.setCreateName(dto.getServiceUserName());
        info.setOrgCode(dto.getOrgCode());
        info.setOrgName(dto.getOrgName());
        info = this.valid(info);
        this.save(info);

    }

    private InvestInfo  valid(InvestInfo info){
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        if("无此类情况".equals(sysDictBizService.getDictValue(info.getIsSituation(),dictList))){
            info.setName(null);
            info.setTitle(null);
            info.setCode(null);
            info.setEnterpriseName(null);
            info.setEstablishTime(null);
            info.setOperatScope(null);
            info.setRegisterCountry(null);
            info.setRegisterProvince(null);
            info.setCity(null);
            info.setOperatAddr(null);
            info.setEnterpriseType(null);
            info.setRegisterCapital(null);
            info.setEnterpriseState(null);
            info.setShareholder(null);
            info.setPersonalCapital(null);
            info.setPersonalRatio(null);
            info.setInvestTime(null);
            info.setSeniorPosition(null);
            info.setSeniorPositionName(null);
            info.setSeniorPositionStartTime(null);
            info.setSeniorPositionEndTime(null);
            info.setIsRelation(null);
            info.setRemarks(null);
            info.setTbType(null);
            info.setYear(null);
        }else{
            //是否为股东（合伙人、所有人）
            if("否".equals(sysDictBizService.getDictValue(info.getShareholder(),dictList))){
                info.setPersonalCapital(null);
                info.setPersonalRatio(null);
                info.setInvestTime(null);
            }
            //是否担任高级职务
            if("否".equals(sysDictBizService.getDictValue(info.getSeniorPosition(),dictList))){
                info.setSeniorPositionName(null);
                info.setSeniorPositionStartTime(null);
                info.setSeniorPositionEndTime(null);
            }
            //该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系
            /*if("否".equals(sysDictBizService.getDictValue(info.getIsRelation(),dictList))){
                info.setRemarks(null);
            }*/
        }
        return info;
    }

    @Override
    public InvestInfo getRepeatInvestInfo(String name, String cardId, String code) {
        InvestInfo one = this.lambdaQuery().eq(InvestInfo::getName, name)
                .eq(InvestInfo::getCardId, cardId)
                .eq(InvestInfo::getCode, code)
                .last(SqlConstant.ONE_SQL).one();
        return one;
    }

    private void checkArea(InvestInfoDTO dto, InvestInfo investInfo) {
        //校验国家/省份/市是否合法
        if (dto.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
            //国家
            GlobalCityInfo country = globalCityInfoService.lambdaQuery().eq(GlobalCityInfo::getName, dto.getRegisterCountry()).last(SqlConstant.ONE_SQL).one();
            if (country == null) {
                throw new RuntimeException("该国家不存在");
            }
            //如果是中国下的，校验省份和市
            if (country.getAreaCode().equals(SystemConstant.CHINA_AREA_CODE)) {
                List<GlobalCityInfo> infoList = globalCityInfoService.lambdaQuery().in(GlobalCityInfo::getName, dto.getRegisterProvince(), dto.getCity()).list();
                if (infoList.isEmpty()) {
                    throw new RuntimeException("该国家下的省份和地级市不存在");
                }
                Map<String, GlobalCityInfo> infoMap = infoList.stream().collect(Collectors.toMap(GlobalCityInfo::getParentId, Function.identity()));
                GlobalCityInfo info = infoMap.get(country.getCountryId());//省份
                if (info == null) {
                    throw new RuntimeException("该国家下的省份不匹配");
                }
                GlobalCityInfo city = infoMap.get(info.getCountryId());//地级市
                if (city == null) {
                    throw new RuntimeException("该省份下的地级市不匹配");
                }
                investInfo.setRegisterProvince(info.getName())//省份
                        .setCity(city.getName());//地级市
            }
            investInfo.setRegisterCountry(country.getName());//国家
        }
    }

    @Override
    public void editInvestInfo(InvestInfoDTO dto) {
        InvestInfo info = new InvestInfo();
        BeanUtil.copyProperties(dto, info);
        //校验国家/省份/市
        checkArea(dto, info);
        info.setState(SystemConstant.SAVE_STATE);
        info.setUpdateTime(DateUtil.date());
        info.setUpdaterId(dto.getServiceUserId());
        info = this.valid(info);
        this.updateById(info);
    }

    @Override
    public void saveBatchInvestmentInfo(List<InvestmentDTO> list) { List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<InvestInfo> investInfoList = new ArrayList<>();

        List<String> cardIds = list.stream().distinct().map(t -> t.getCardId()).collect(Collectors.toList());
        List<InvestInfo> infoList = this.lambdaQuery().in(InvestInfo::getCardId, cardIds).list();
        if (infoList.isEmpty()) {
            list.stream().forEach(t -> {
                if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        checkArea(t);
                        InvestInfo investInfo = new InvestInfo();
                        BeanUtils.copyProperties(t, investInfo);
                        investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(new Date())
                                .setUpdateTime(new Date());
                        this.repalceDictId(investInfo,dictList);
                        investInfoList.add(investInfo);
                    }
                } else {
                    InvestInfo investInfo = new InvestInfo();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
                    this.repalceDictId(investInfo,dictList);
                    investInfoList.add(investInfo);
                }
            });
            if (!investInfoList.isEmpty()) {
                this.saveBatch(investInfoList);
            }
            return;
        }
        List<InvestInfo> updateList = new ArrayList<>();
        Map<String, List<InvestInfo>> infoMap = infoList.stream().collect(Collectors.groupingBy(InvestInfo::getCardId));
        list.stream().forEach(t -> {
            //List<InvestInfo> infos = infoMap.get(t.getCardId());
            List<InvestInfo> infos = infoMap.containsKey(t.getCardId())?infoMap.get(t.getCardId()):null;
            if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {//有此类情况
                if (CollectionUtil.isNotEmpty(infos)) {//如果不为空，进行比较
                    //校验姓名和统一社会信用代码不能为空
                    if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        checkArea(t);
                        InvestInfo info = new InvestInfo();
                        BeanUtils.copyProperties(t, info);
                        info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setUpdateTime(DateUtil.date());
                        //判断该干部下的其他子项名称和代码是否相同
                        long nameIndex = infos.stream().filter(e->t.getName().equals(e.getName())).count();
                        long titleIndex = infos.stream().filter(e->sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())).count();
                        long codeIndex = infos.stream().filter(e->t.getCode().equals(e.getCode())).count();
                        this.repalceDictId(info,dictList);
                        if(nameIndex==0|titleIndex==0|codeIndex==0){ //一个都不重复
                            //如果不相同，新增，否则就是覆盖
                            info.setCreateTime(DateUtil.date());
                            investInfoList.add(info);
                        }else{ //有重复数据了
                            InvestInfo existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
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
                        //校验姓名和统一社会信用代码不能为空
                        if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                            //校验国家/省/市
                            checkArea(t);
                            InvestInfo info = new InvestInfo();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(DateUtil.date());
                            info = this.repalceDictId(info,dictList);
                            //判断该干部下的其他子项名称和代码是否相同
                            if (!t.getName().equals(e.getName()) &&!info.getTitle().equals(e.getTitle())&& !t.getCode().equals(e.getCode())) {
                                //如果不相同，新增，否则就是覆盖
                                info.setCreateTime(DateUtil.date());
                                investInfoList.add(info);
                            } else {
                                info.setId(e.getId());
                                updateList.add(info);
                            }
                        }
                    });*/
                } else {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        checkArea(t);
                        //数据库为空，直接add
                        InvestInfo info = new InvestInfo();
                        BeanUtils.copyProperties(t, info);
                        info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(DateUtil.date())
                                .setUpdateTime(DateUtil.date());
                        investInfoList.add(info);
                    }
                }
            } else {
                //说明无此类情况
                InvestInfo info = new InvestInfo();
                BeanUtils.copyProperties(t, info);
                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setUpdateTime(DateUtil.date());
                this.repalceDictId(info,dictList);
                //数据库中如果不存在数据
                if (CollectionUtil.isEmpty(infos)) {
                    info.setCreateTime(DateUtil.date());
                    investInfoList.add(info);
                } else {
                    //覆盖
                    info.setId(infos.get(0).getId());
                    updateList.add(info);
                }
            }

        });
        if (!investInfoList.isEmpty()) {
            this.saveBatch(investInfoList);
        }
        if (!updateList.isEmpty()) {
            this.updateBatchById(updateList);
        }
    }

    private String checkArea(InvestmentDTO dto) {
        //校验国家/省份/市是否合法
        //国家
            if (StringUtils.isBlank(dto.getRegisterCountry())) {
//                throw new RuntimeException("在有此类情况下，注册地国家信息不能为空");
                return "在有此类情况下，注册地国家信息不能为空";
            }
            GlobalCityInfo country = globalCityInfoService.lambdaQuery().eq(GlobalCityInfo::getName, dto.getRegisterCountry()).last(SqlConstant.ONE_SQL).one();
            if (country == null) {
//                throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家不存在");
                return dto.getRegisterCountry() + ":" + "国家不存在";
            }
            //如果是中国下的，校验省份和市
            if (country.getAreaCode().equals(SystemConstant.CHINA_AREA_CODE)) {
                if (StringUtils.isBlank(dto.getRegisterProvince()) || StringUtils.isBlank(dto.getCity())) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份或地级市信息不能为空");
                    return dto.getRegisterCountry() + ":" + "国家下的省份或地级市信息不能为空";
                }
                //省份及城市
                List<GlobalCityInfo> infoList = globalCityInfoService.lambdaQuery().in(GlobalCityInfo::getName, dto.getRegisterProvince(), dto.getCity()).list();

                if (infoList.isEmpty()) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份和地级市不存在");
                    return dto.getRegisterCountry() + ":" + "国家下的省份和地级市不存在";
                }
                //Map<String, GlobalCityInfo> infoMap = infoList.stream().collect(Collectors.toMap(GlobalCityInfo::getParentId, Function.identity()));
                //Map<String, List<GlobalCityInfo>> collect = infoList.stream().collect(Collectors.groupingBy(GlobalCityInfo::getParentId));
                //GlobalCityInfo info = infoMap.get(country.getCountryId());//省份
                //查询字相同且地区号为0086并且地区id不为null的省份
                GlobalCityInfo  province= infoList.stream().
                        filter(e->e.getName().equals(dto.getRegisterProvince())
                                &&e.getAreaCode().equals(SystemConstant.CHINA_AREA_CODE)
                                && StrUtil.isNotEmpty(e.getCountryId())).findAny().orElse(null);
                //infoMap.
                if (Objects.isNull(province)) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份不匹配");
                    return dto.getRegisterCountry() + ":" + "国家下的省份不匹配";
                }
                //
                String provinceId = province.getCountryId(); //省份地区号
                //查询省份下的城市
                List<GlobalCityInfo> cityList = infoList.stream().filter(e -> e.getParentId().equals(provinceId)).collect(Collectors.toList());
                //查询当前城市列表中满足条件的数据
                long i = cityList.stream().filter(e -> e.getName().equals(dto.getCity())).count();
                //GlobalCityInfo city = infoMap.get(info.getCountryId());//地级市
                if (i == 0) {
//                    throw new RuntimeException(dto.getRegisterProvince() + ":" + "省份下的地级市不匹配");
                    return dto.getRegisterProvince() + ":" + "省份下的地级市不匹配";
                }
            }
            return null;

    }

    @Override
    public List<InvestmentDTO> exportInvestmentExcel(List<String> ids) {

        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<InvestmentDTO> list = this.lambdaQuery().in(InvestInfo::getId, ids).list().stream().map(t -> {
            InvestmentDTO dto = new InvestmentDTO();
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
        list = this.repalceDictValue(list,dictList);
        return list;
    }

    @Override
    public void saveBatchInvestmentInfo(List<InvestmentDTO> lists, BaseDTO baseDTO, ExportReturnVO exportReturnVO) {
        //过滤掉必填校验未通过的字段
        List<InvestmentDTO> list = lists.stream().filter(e -> StringUtils.isBlank(e.getMessage())).collect(Collectors.toList());
        list=checkParams(list,exportReturnVO);
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<InvestInfo> investInfoList = new ArrayList<>();

        List<String> cardIds = list.stream().distinct().map(t -> t.getCardId()).collect(Collectors.toList());
        List<InvestInfo> infoList = this.lambdaQuery().in(InvestInfo::getCardId, cardIds).list();
        if (infoList.isEmpty()) {
            list.stream().forEach(t -> {
                if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        String returnMessage = checkArea(t);
                        if (StringUtils.isBlank(returnMessage)){
                            InvestInfo investInfo = new InvestInfo();
                            BeanUtils.copyProperties(t, investInfo);
                            investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setCreateTime(new Date())
                                    .setUpdateTime(new Date());
                            String checkDict = this.repalceDictId(investInfo,dictList);
                            if (StringUtils.isBlank(checkDict)){
                                investInfo.setCreateName(baseDTO.getServiceUserName());
                                investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                                investInfoList.add(investInfo);

                            }else {
                                exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            }

                        }else {
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),returnMessage));
                        }

                    }
                } else {
                    InvestInfo investInfo = new InvestInfo();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
                    String checkDict = this.repalceDictId(investInfo,dictList);
                    if (StringUtils.isBlank(checkDict)){
                        investInfo.setCreateName(baseDTO.getServiceUserName());
                        investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                        investInfoList.add(investInfo);
                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                    }else {
                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                    }

                }
            });
            if (!investInfoList.isEmpty()) {
                this.saveBatch(investInfoList);
            }
            return;
        }
        List<InvestInfo> updateList = new ArrayList<>();
        Map<String, List<InvestInfo>> infoMap = infoList.stream().collect(Collectors.groupingBy(InvestInfo::getCardId));
        list.stream().forEach(t -> {
            //List<InvestInfo> infos = infoMap.get(t.getCardId());
            List<InvestInfo> infos = infoMap.containsKey(t.getCardId())?infoMap.get(t.getCardId()):null;
            if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {//有此类情况
                if (CollectionUtil.isNotEmpty(infos)) {//如果不为空，进行比较
                    //校验姓名和统一社会信用代码不能为空
                    if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        String returnMessage = checkArea(t);
                        if (StringUtils.isBlank(returnMessage)){
                            InvestInfo info = new InvestInfo();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(DateUtil.date());
                            //判断该干部下的其他子项名称和代码是否相同
                            long nameIndex = infos.stream().filter(e->t.getName().equals(e.getName())).count();
                            long titleIndex = infos.stream().filter(e->sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())).count();
                            long codeIndex = infos.stream().filter(e->t.getCode().equals(e.getCode())).count();
                            String checkDict = this.repalceDictId(info,dictList);
                            if (StringUtils.isBlank(checkDict)){
                                if(nameIndex==0|titleIndex==0|codeIndex==0){ //一个都不重复
                                    //如果不相同，新增，否则就是覆盖
                                    info.setCreateTime(DateUtil.date());
                                    info.setCreateName(baseDTO.getServiceUserName());
                                    info.setCreateAccount(baseDTO.getServiceUserAccount());
                                    investInfoList.add(info);
                                    exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                }else{ //有重复数据了
                                    InvestInfo existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
                                            &&sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())
                                            &&t.getCode().equals(e.getCode())).findAny().orElse(null);
                                    String title = info.getTitle();
                                    if(Objects.nonNull(existInfo)){
                                        info.setId(existInfo.getId());
                                        updateList.add(info);
                                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                    }else if (investInfoList.isEmpty()||investInfoList.stream().filter(privateEquity1 -> t.getName().equals(privateEquity1.getName())&&t.getCode().equals(privateEquity1.getCode())&&title.equals(privateEquity1.getTitle())).count()==0){
                                        info.setCreateTime(DateUtil.date());
                                        info.setCreateName(baseDTO.getServiceUserName());
                                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                                        investInfoList.add(info);
                                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                    }else {
                                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复"));
                                    }
                                }
                            }else {
                                exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            }

                        }else {
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),returnMessage));
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
                        //校验姓名和统一社会信用代码不能为空
                        if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                            //校验国家/省/市
                            checkArea(t);
                            InvestInfo info = new InvestInfo();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(DateUtil.date());
                            info = this.repalceDictId(info,dictList);
                            //判断该干部下的其他子项名称和代码是否相同
                            if (!t.getName().equals(e.getName()) &&!info.getTitle().equals(e.getTitle())&& !t.getCode().equals(e.getCode())) {
                                //如果不相同，新增，否则就是覆盖
                                info.setCreateTime(DateUtil.date());
                                investInfoList.add(info);
                            } else {
                                info.setId(e.getId());
                                updateList.add(info);
                            }
                        }
                    });*/
                } else {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        checkArea(t);
                        //数据库为空，直接add
                        InvestInfo info = new InvestInfo();
                        BeanUtils.copyProperties(t, info);
                        String checkDict = this.repalceDictId(info,dictList);
                        if (StringUtils.isBlank(checkDict)){
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setCreateTime(DateUtil.date())
                                    .setUpdateTime(DateUtil.date());
                            info.setCreateName(baseDTO.getServiceUserName());
                            info.setCreateAccount(baseDTO.getServiceUserAccount());
                            investInfoList.add(info);
                            exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                        }else {
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                        }

                    }
                }
            } else {
                //说明无此类情况
                InvestInfo info = new InvestInfo();
                BeanUtils.copyProperties(t, info);
                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setUpdateTime(DateUtil.date());
                String checkDict = this.repalceDictId(info,dictList);
                if (StringUtils.isBlank(checkDict)){
                    //数据库中如果不存在数据
                    if (CollectionUtil.isEmpty(infos)) {
                        info.setCreateTime(DateUtil.date());
                        info.setCreateName(baseDTO.getServiceUserName());
                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                        investInfoList.add(info);
                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                    } else {
                        //覆盖
                        info.setId(infos.get(0).getId());
                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                        updateList.add(info);
                    }
                }else {
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                }

            }

        });
        if (!investInfoList.isEmpty()) {
            this.saveBatch(investInfoList);
        }
        if (!updateList.isEmpty()) {
            this.updateBatchById(updateList);
        }
    }

    private List<InvestmentDTO> checkParams(List<InvestmentDTO> list, ExportReturnVO exportReturnVO) {
        List<String> isOrNotList = Arrays.asList(SystemConstant.WHETHER_YES, SystemConstant.WHETHER_NO);
        return list.stream().filter(e->{
            if (SystemConstant.IS_SITUATION_YES.equals(e.getIsSituation())){
                if (StringUtils.isBlank(e.getName())||StringUtils.isBlank(e.getTitle())||StringUtils.isBlank(e.getEnterpriseName())||StringUtils.isBlank(e.getEnterpriseType())||StringUtils.isBlank(e.getCode())||StringUtils.isBlank(e.getEstablishTime())||StringUtils.isBlank(e.getRegisterCountry())||StringUtils.isBlank(e.getRegisterProvince())||StringUtils.isBlank(e.getCity())
                ||StringUtils.isBlank(e.getOperatAddr())||StringUtils.isBlank(e.getRegisterCapital())||StringUtils.isBlank(e.getEnterpriseState())||StringUtils.isBlank(e.getOperatScope())||StringUtils.isBlank(e.getShareholder())||StringUtils.isBlank(e.getSeniorPosition())||StringUtils.isBlank(e.getIsRelation())||StringUtils.isBlank(e.getTbType())||StringUtils.isBlank(e.getYear())
                ){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容不能为空：姓名,称谓,企业或其他市场主体名称,企业或其他市场主体类型,统一社会信用代码/注册号,成立日期,注册地（国家）,注册地（省）,注册地（市）,注册资本或资金数额,企业状态,经营范围,是否为股东（合伙人、所有人）,是否担任高级职务,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系,填报类型,年度。"));
                    return false;
                }
                if (!isOrNotList.contains(e.getShareholder())||!isOrNotList.contains(e.getSeniorPosition())||!isOrNotList.contains(e.getIsRelation())){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容只能填是否：是否为股东（合伙人、所有人）,是否担任高级职务,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系。"));
                    return false;
                }
                if (SystemConstant.WHETHER_YES.equals(e.getShareholder())&&(StringUtils.isBlank(e.getPersonalCapital()))||StringUtils.isBlank(e.getPersonalRatio())||StringUtils.isBlank(e.getInvestTime())){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"是为机构股东（合伙人、所有人等）时以下内容不能为空： 个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例,投资时间。"));
                    return false;
                }
                if (SystemConstant.WHETHER_YES.equals(e.getSeniorPosition())&&(StringUtils.isBlank(e.getSeniorPositionName())||StringUtils.isBlank(e.getSeniorPositionStartTime())||StringUtils.isBlank(e.getSeniorPositionEndTime()))){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"担任高级职务时以下内容不能为空： 所担任的高级职务名称,担任高级职务的时间。"));
                    return false;
                }
                if (SystemConstant.WHETHER_YES.equals(e.getIsRelation())&&StringUtils.isBlank(e.getRemarks())){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系时以下内容不能为空：备注。"));
                    return false;
                }
                List<String> numbers = Stream.of(e.getRegisterCapital(), e.getPersonalCapital(), e.getPersonalRatio(), e.getYear()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (!NumberUtils.isAllNumeric(numbers)){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"以下内容必须为数：注册资本（金）或资金数额（出资额）,个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例,年度。"));
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    private List<InvestmentDTO>  repalceDictValue(List<InvestmentDTO> list,List<SysDictBiz> dictList){
        list.parallelStream().forEach(dto->{
            //字典对应项
            String isSituation = sysDictBizService.getDictValue(dto.getIsSituation(), dictList);
            String title = sysDictBizService.getDictValue(dto.getTitle(), dictList);
            String enterpriseState = sysDictBizService.getDictValue(dto.getEnterpriseState(), dictList);
            String enterpriseType = sysDictBizService.getDictValue(dto.getEnterpriseType(), dictList);
            String shareholder = sysDictBizService.getDictValue(dto.getShareholder(), dictList);
            String seniorPosition = sysDictBizService.getDictValue(dto.getSeniorPosition(), dictList);
            String isRelation = sysDictBizService.getDictValue(dto.getIsRelation(), dictList);

            dto.setIsSituation(isSituation);
            dto.setTitle(title);
            dto.setEnterpriseState(enterpriseState);
            dto.setEnterpriseType(enterpriseType);
            dto.setShareholder(shareholder);
            dto.setSeniorPosition(seniorPosition);
            dto.setIsRelation(isRelation);
        });

        return list;
    }

    private String repalceDictId(InvestInfo dto,List<SysDictBiz> dictList){
        //字典对应项
        String isSituation = null;
        String title = null;
        String enterpriseState = null;
        String enterpriseType = null;
        String shareholder = null;
        String seniorPosition = null;
        String isRelation = null;

        if (StringUtils.isNotBlank(dto.getIsSituation())){
            isSituation=sysDictBizService.getDictId(dto.getIsSituation(),dictList);
            if (StringUtils.isBlank(isSituation)){
                return "有无此类情况字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(dto.getTitle())){
            title=sysDictBizService.getDictId(dto.getTitle(),dictList);
            if (StringUtils.isBlank(title)){
                return "称谓字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(dto.getEnterpriseState())){
            enterpriseState=sysDictBizService.getDictId(dto.getEnterpriseState(),dictList);
            if (StringUtils.isBlank(enterpriseState)){
                return "企业状态字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(dto.getEnterpriseType())){
            enterpriseType=sysDictBizService.getDictId(dto.getEnterpriseType(),dictList);
            if (StringUtils.isBlank(enterpriseType)){
                return "企业或其他市场主体类型字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(dto.getShareholder())){
            shareholder=sysDictBizService.getDictId(dto.getShareholder(),dictList);
            if (StringUtils.isBlank(shareholder)){
                return "是否为股东（合伙人、所有人）字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(dto.getSeniorPosition())){
            seniorPosition=sysDictBizService.getDictId(dto.getSeniorPosition(),dictList);
            if (StringUtils.isBlank(seniorPosition)){
                return "是否担任高级职务字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(dto.getIsRelation())){
            isRelation=sysDictBizService.getDictId(dto.getIsRelation(),dictList);
            if (StringUtils.isBlank(isRelation)){
                return "该企业或其他市场主体是否与报告人所在单位字典项不存在";
            }
        }
        dto.setIsSituation(isSituation);
        dto.setTitle(title);
        dto.setEnterpriseState(enterpriseState);
        dto.setEnterpriseType(enterpriseType);
        dto.setShareholder(shareholder);
        dto.setSeniorPosition(seniorPosition);
        dto.setIsRelation(isRelation);
        return null;
    }
}