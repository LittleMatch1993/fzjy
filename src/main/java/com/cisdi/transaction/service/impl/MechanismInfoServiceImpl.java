package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
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
import com.cisdi.transaction.mapper.master.MechanismInfoMapper;
import com.cisdi.transaction.service.*;
import com.cisdi.transaction.util.ThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
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
 * 配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况
 *
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:16
 */
@Service
@Slf4j
public class MechanismInfoServiceImpl extends ServiceImpl<MechanismInfoMapper, MechanismInfo> implements MechanismInfoService {

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

    @Override
    public boolean updateState(List<String> ids, String state) {
        UpdateWrapper<MechanismInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(MechanismInfo::getState,state).in(MechanismInfo::getId,ids);

        boolean b = this.update(updateWrapper);
        return b;
    }

    @Override
    public int countByNameAndCardIdAndCode(String name, String cardId, String code) {
        Integer count = this.lambdaQuery().eq(MechanismInfo::getName, name).eq(MechanismInfo::getCardId, cardId).eq(MechanismInfo::getCode, code).count();
        return Objects.isNull(count) ? 0 : count.intValue();
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
        Map<String, List<KVVO>> map = kvList.stream().collect(Collectors.groupingBy(KVVO::getId));
        List<KVVO> tempList = new ArrayList<>();
        for (Map.Entry<String, List<KVVO>> m : map.entrySet()) {
            String id = m.getKey();
            List<KVVO> list = m.getValue();
            String tips = list.stream().map(KVVO::getName).collect(Collectors.joining(","));
            KVVO vo = new KVVO();
            vo.setId(id);
            vo.setName(tips);
            tempList.add(vo);
        }
        if(CollectionUtil.isEmpty(tempList)){
            return false;
        }
        this.baseMapper.updateBatchTips(tempList);
        return true;
    }

    @Override
    public void overrideInvestInfo(String id, MechanismInfoDTO dto) {
        MechanismInfo info = new MechanismInfo();
        BeanUtil.copyProperties(dto,info);
        info.setId(id);
        //校验国家/省份/市
        checkArea(dto, info);
        info.setState(SystemConstant.SAVE_STATE);
        //info.setCreateTime(DateUtil.date());
        info.setUpdateTime(DateUtil.date());
        info.setTenantId(dto.getServiceLesseeId());
        info.setCreatorId(dto.getServiceUserId());
        info = this.valid(info);
        this.updateById(info);
        addFamilyInfo(info);
    }

    @Override
    public MechanismInfo getRepeatInvestInfo(String name, String cardId, String code) {
        MechanismInfo one = this.lambdaQuery().eq(MechanismInfo::getName, name)
                .eq(MechanismInfo::getCardId, cardId)
                .eq(MechanismInfo::getCode, code)
                .last(SqlConstant.ONE_SQL).one();
        return one;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMsgUtil<Object> submitMechanismInfo(SubmitDto submitDto) {
        List<String> submitFailIdList = null;
        String resutStr = "提交成功";
        List<String> ids = submitDto.getIds();
        List<MechanismInfo> infoList = this.lambdaQuery().in(MechanismInfo::getId, ids).list();
        if (CollectionUtil.isEmpty(infoList)) {
            return ResultMsgUtil.failure("数据不存在了");
        }
      /*  long count = infoList.stream().filter(e -> SystemConstant.VALID_STATE.equals(e.getState())).count();
        if (count > 0) {
            return ResultMsgUtil.failure("当前表中的有效数据不能重复提交到禁止交易信息表中!");
        }*/
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        long j = infoList.stream().filter(e -> "无此类情况".equals(sysDictBizService.getDictValue(e.getIsSituation(),dictList))).count();
        if (j > 0) {
            return ResultMsgUtil.failure("当前表中的无此类情况数据不能提交到禁止交易信息表中!");
        }
        //boolean b = this.updateState(ids, "有效");
        boolean b = true;
        if (b) { //配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况 表数据改为有效状态 并且修改成功 往 配偶，子女及其配偶表中添加数据。
            //获取干部的基本信息
           List<String> cardIds = infoList.stream().map(MechanismInfo::getCardId).collect(Collectors.toList());
           // List<GbBasicInfo> gbList = gbBasicInfoService.selectBatchByCardIds(cardIds);
            //获取干部组织的基本信息
            //获取干部组织的基本信息
            List<GbOrgInfo> gbOrgList = null;
            try {
                //gbOrgList = gbBasicInfoService.selectGbOrgInfoByCardIds(cardIds);
                String orgCode = submitDto.getOrgCode();
                gbOrgList = gbBasicInfoService.selectByOrgCodeAndCardIds(orgCode,cardIds);
            }catch (BusinessException e){
                return ResultMsgUtil.failure(e.getMsg());
            } catch (Exception e){
                e.printStackTrace();
                return ResultMsgUtil.failure("干部组织信息查询失败");
            }
            if(CollectionUtil.isEmpty(gbOrgList)){
                //this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("没有找到干部组织信息");
            }
           /* if(CollectionUtil.isNotEmpty(tempList)){
                infoList = infoList.stream().filter(e->tempList.contains(e.getId())).collect(Collectors.toList());
                ids = infoList.stream().map(MechanismInfo::getCardId).collect(Collectors.toList());
            }*/
            //向禁止交易信息表中添加数据 并进行验证 及其他逻辑处理
            banDealInfoService.deleteBanDealInfoByRefId(ids);
            ResultMsgUtil<Map<String, Object>> mapResult = banDealInfoService.insertBanDealInfoOfMechanismInfo(infoList,gbOrgList);
            //处理提交数据后
            Map<String, Object> data = mapResult.getData();
            String banDeal = data.get("banDeal").toString();
            List<String> submitIds = (List<String>)data.get("submitIds");
            List<KVVO> submitFailId = (List<KVVO>)data.get("submitFailId"); //无法提交的数据id
            StringJoiner sj = new StringJoiner(",");
            if(CollectionUtil.isNotEmpty(submitFailId)){//提交失败的数据
                this.updateBathTips(submitFailId);
                submitFailIdList = submitFailId.stream().map(KVVO::getId).collect(Collectors.toList());
            }
            if(CollectionUtil.isNotEmpty(submitIds)){ //提交成功的数据
                this.updateState(submitIds,SystemConstant.VALID_STATE);
                this.baseMapper.cleanBatchTips(submitIds);
            }
            if(!Boolean.valueOf(banDeal)){
                sj.add("数据库新增数据失败");
                resutStr = sj.toString();
                return ResultMsgUtil.failure(resutStr);
            }else{
                sj.add("提交数据成功");
                if(CollectionUtil.isNotEmpty(submitFailId)){
                   /* this.updateState(submitIds,SystemConstant.VALID_STATE);
                    int beferIndex = infoList.size();
                    int afterIndex = submitIds.size();
                    sj.add(",其中"+(beferIndex-afterIndex)+"数据提交失败");*/
                    sj.add("其中"+(submitFailId.size())+"数据提交失败");
                    resutStr = sj.toString();
                    return ResultMsgUtil.success(ResultCode.WARING.getCode(), resutStr,submitFailIdList);
                }
            }
            resutStr = sj.toString();
        }
        return ResultMsgUtil.success(resutStr,null);
    }

    @Override
    public void addFamilyInfo(MechanismInfo info) {
        String cardId = info.getCardId();
        String name = info.getName();
        String title = info.getTitle();
        if(StrUtil.isEmpty(cardId)||StrUtil.isEmpty(name)||StrUtil.isEmpty(title)){
            return;
        }
        long i = spouseBasicInfoService.selectCount(cardId, name, title);
        if (i > 0) { //i>0 说明当前数据重复了
            return;
        }
        List<SpouseBasicInfo> sbiList = new ArrayList<>();
        SpouseBasicInfo temp = new SpouseBasicInfo();
        temp.setCreateTime(DateUtil.date());
        temp.setUpdateTime(DateUtil.date());
        temp.setCadreName(info.getGbName());
        temp.setCadreCardId(cardId);
        temp.setName(name);
        temp.setTitle(title);
        temp.setCardName(info.getFamilyCardType());
        temp.setCardId(info.getFamilyCardId());
        temp.setCardName(info.getFamilyCardType());
        temp.setCardId(info.getFamilyCardId());
        temp.setRefId(info.getId());
        sbiList.add(temp);
        if (CollectionUtil.isNotEmpty(sbiList)) {
            //添加干部配偶，子女及其配偶数据
            try {
                spouseBasicInfoService.saveBatch(sbiList);
            } catch (Exception e) {
                e.printStackTrace();
                // this.updateState(ids, SystemConstant.SAVE_STATE)
            }
        }
    }

    private MechanismInfo  valid(MechanismInfo info){
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        if("无此类情况".equals(sysDictBizService.getDictValue(info.getIsSituation(),dictList))){
            info.setName(null);
            info.setTitle(null);
            info.setQualificationName(null);
            info.setQualificationCode(null);
            info.setOrganizationName(null);
            info.setCode(null);
            info.setOperatScope(null);
            info.setEstablishTime(null);
            info.setRegisterCountry(null);
            info.setRegisterProvince(null);
            info.setCity(null);
            info.setOperatAddr(null);
            info.setOrganizationType(null);
            info.setRegisterCapital(null);
            info.setOperatState(null);
            info.setShareholder(null);
            info.setPersonalCapital(null);
            info.setPersonalRatio(null);
            info.setJoinTime(null);
            info.setPractice(null);
            info.setPostName(null);
            info.setInductionTime(null);
            info.setIsRelation(null);
            info.setRemarks(null);
            info.setTbType(null);
            info.setYear(null);
        }else{
            //是否为股东（合伙人、所有人）
            if("否".equals(sysDictBizService.getDictValue(info.getShareholder(),dictList))){
                info.setPersonalCapital(null);
                info.setPersonalRatio(null);
                info.setJoinTime(null);
            }
            //是否在该机构中从业
            if("否".equals(sysDictBizService.getDictValue(info.getPractice(),dictList))){
                info.setPostName(null);
                info.setInductionTime(null);
            }
            //该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系
            /*if("否".equals(sysDictBizService.getDictValue(info.getIsRelation(),dictList))){
                info.setRemarks(null);
            }*/
        }
        return info;
    }
    @Override
    public void saveMechanismInfo(MechanismInfoDTO dto) {

        MechanismInfo info = new MechanismInfo();
        BeanUtil.copyProperties(dto,info,new String[]{"id"});
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
        addFamilyInfo(info);
    }

    private void checkArea(MechanismInfoDTO dto, MechanismInfo mechanismInfo) {
        //校验国家/省份/市是否合法
        if (dto.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
            //国家
            GlobalCityInfo country = globalCityInfoService.lambdaQuery().eq(GlobalCityInfo::getName,dto.getRegisterCountry()).last(SqlConstant.ONE_SQL).one();
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
                mechanismInfo.setRegisterProvince(info.getName())//省份
                        .setCity(city.getName());//地级市
            }
            mechanismInfo.setRegisterCountry(country.getName());//国家
        }
    }

    @Override
    public void editMechanismInfo(MechanismInfoDTO dto) {
        MechanismInfo info = new MechanismInfo();
        BeanUtil.copyProperties(dto, info);
        //校验国家/省份/市
        checkArea(dto, info);
        info.setState(SystemConstant.SAVE_STATE);
        info.setUpdateTime(DateUtil.date());
        info.setUpdaterId(dto.getServiceUserId());
        info = this.valid(info);
        this.updateById(info);
        //addFamilyInfo(info);
        this.editRefSpouseBasic(info);
    }
    private  void editRefSpouseBasic(MechanismInfo info){
        String id = info.getId();
        SpouseBasicInfo basicInfo = spouseBasicInfoService.selectByRefId(id);
        if(Objects.nonNull(basicInfo)){
            basicInfo.setUpdateTime(DateUtil.date());
            basicInfo.setCadreName(info.getGbName()); //干部姓名
            basicInfo.setCadreCardId(info.getCardId()); //干部身份证id
            basicInfo.setName(info.getName()); //家属姓名
            basicInfo.setTitle(info.getTitle());//家属关系
            basicInfo.setCardName(info.getFamilyCardType()); //家属证件类型
            basicInfo.setCardId(info.getFamilyCardId()); //家属证件号familyCardType
            basicInfo.setRefId(info.getId()); //关联数据id
            spouseBasicInfoService.updateById(basicInfo);
        }
    }

    @Override
    public void saveBatchInvestInfo(List<CommunityServiceDTO> list) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<MechanismInfo> mechanismInfoList = new ArrayList<>();
        List<String> cardIds = list.stream().distinct().map(t -> t.getCardId()).collect(Collectors.toList());
        List<MechanismInfo> infoList = this.lambdaQuery().in(MechanismInfo::getCardId, cardIds).list();
        if (infoList.isEmpty()) {
            list.stream().forEach(t -> {
                if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        checkArea(t);
                        MechanismInfo investInfo = new MechanismInfo();
                        BeanUtils.copyProperties(t, investInfo);
                        investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(new Date())
                                .setUpdateTime(new Date());
                        //字典替换
                        this.replaceDictId(investInfo,dictList);
                        mechanismInfoList.add(investInfo);
                    }
                } else {
                    MechanismInfo investInfo = new MechanismInfo();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
                    //字典替换
                    this.replaceDictId(investInfo,dictList);
                    mechanismInfoList.add(investInfo);
                }
            });
            if (!mechanismInfoList.isEmpty()) {
                this.saveBatch(mechanismInfoList);
            }
            return;
        }
        List<MechanismInfo> updateList = new ArrayList<>();
        Map<String, List<MechanismInfo>> infoMap = infoList.stream().collect(Collectors.groupingBy(MechanismInfo::getCardId));
        list.stream().forEach(t -> {
            List<MechanismInfo> infos = infoMap.containsKey(t.getCardId())?infoMap.get(t.getCardId()):null;
            if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                if (CollectionUtil.isNotEmpty(infos)) {//如果不为空，进行比较
                    //校验姓名和统一社会信用代码不能为空
                    if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        checkArea(t);
                        MechanismInfo info = new MechanismInfo();
                        BeanUtils.copyProperties(t, info);
                        info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setUpdateTime(DateUtil.date());
                        //判断该干部下的其他子项名称和代码是否相同
                        long nameIndex = infos.stream().filter(e->t.getName().equals(e.getName())).count();
                        long titleIndex = infos.stream().filter(e->sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())).count();
                        long codeIndex = infos.stream().filter(e->t.getCode().equals(e.getCode())).count();
                        this.replaceDictId(info,dictList);
                        if(nameIndex==0|titleIndex==0|codeIndex==0){ //一个都不重复
                            //如果不相同，新增，否则就是覆盖
                            info.setCreateTime(DateUtil.date());
                            mechanismInfoList.add(info);
                        }else{ //有重复数据了
                            MechanismInfo existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
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
                    //有此类情况
                   /* infos.stream().forEach(e -> {
                        //判断该干部下的其他子项名称和代码是否相同，不相同则添加数据库
                        if (StringUtils.isNotBlank(t.getName())&& StringUtils.isNotBlank(t.getTitle()) && StringUtils.isNotBlank(t.getCode())) {
                            //校验国家/省/市
                            checkArea(t);
                            MechanismInfo info = new MechanismInfo();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(new Date());
                            //字典替换
                            info = this.replaceDictId(info,dictList);
                            if (!t.getName().equals(e.getName()) ||!info.getTitle().equals(e.getTitle())||!t.getCode().equals(e.getCode())) {
                                info.setCreateTime(new Date());
                                mechanismInfoList.add(info);
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
                        MechanismInfo info = new MechanismInfo();
                        BeanUtils.copyProperties(t, info);
                        info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(new Date())
                                .setUpdateTime(new Date());
                        //字典替换
                        this.replaceDictId(info,dictList);
                        mechanismInfoList.add(info);
                    }
                }
            } else {
                //说明无此类情况
                MechanismInfo info = new MechanismInfo();
                BeanUtils.copyProperties(t, info);
                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setUpdateTime(new Date());
                //数据库中如果不存在数据
                if (CollectionUtil.isEmpty(infos)) {
                    info.setCreateTime(new Date());
                    mechanismInfoList.add(info);//可添加到数据库中
                } else {
                    info.setId(infos.get(0).getId());
                    updateList.add(info);
                }
            }

        });
        if (!mechanismInfoList.isEmpty()) {
            this.saveBatch(mechanismInfoList);
        }
        if (!updateList.isEmpty()) {
            this.updateBatchById(updateList);
        }
    }

    private String checkArea(CommunityServiceDTO dto) {
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
                if (StringUtils.isBlank(dto.getRegisterProvince()) ) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份或地级市信息不能为空");
                    return dto.getRegisterCountry() + ":" + "国家下的省份信息不能为空";
                }
                //省份及城市
                List<GlobalCityInfo> infoList = globalCityInfoService.lambdaQuery().in(GlobalCityInfo::getName, dto.getRegisterProvince(), dto.getCity()).list();

                if (infoList.isEmpty()) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份和地级市不存在");
                    return dto.getRegisterCountry() + ":" + "国家下的省份和地级市不存在";
                }
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
                if (i == 0) {
//                    throw new RuntimeException(dto.getRegisterProvince() + ":" + "省份下的地级市不匹配");
//                    return dto.getRegisterProvince() + ":" + "省份下的地级市不匹配";
                }
            }
            return null;
    }

    @Override
    public List<CommunityServiceDTO> exportCommunityServiceExcel(List<String> ids) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<CommunityServiceDTO> list = this.lambdaQuery().in(MechanismInfo::getId, ids).list().stream().map(t -> {
            CommunityServiceDTO dto = new CommunityServiceDTO();
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
        list = this.replaceDictValue(list,dictList);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchInvestInfo(List<CommunityServiceDTO> lists, BaseDTO baseDTO, ExportReturnVO exportReturnVO) {
        //过滤掉必填校验未通过的字段
        List<CommunityServiceDTO> list = lists.stream().filter(e -> StringUtils.isBlank(e.getMessage())).collect(Collectors.toList());
        list=checkParams(list,exportReturnVO);
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<MechanismInfo> mechanismInfoList = new ArrayList<>();
        List<String> cardIds = list.stream().distinct().map(t -> t.getCardId()).collect(Collectors.toList());
        List<MechanismInfo> infoList = this.lambdaQuery().in(MechanismInfo::getCardId, cardIds).list();
        Set<String> uniqueSet=new HashSet<>();
        if (infoList.isEmpty()) {
            list.stream().forEach(t -> {
                String uniqueCode = t.getCardId() + "," + t.getName() + "," + t.getTitle();
                if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        String returnMessage = checkArea(t);
                        if (StringUtils.isNotBlank(returnMessage)){
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),returnMessage));
                        }else {
                            MechanismInfo investInfo = new MechanismInfo();
                            BeanUtils.copyProperties(t, investInfo);
                            investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setCreateTime(new Date())
                                    .setUpdateTime(new Date());
                            String title = investInfo.getTitle();
                            //字典替换
                            String checkDict = this.replaceDictId(investInfo, dictList);
                            if (StringUtils.isNotBlank(checkDict)){
                                exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            }else {
                                if (uniqueSet.contains(uniqueCode)){
                                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复:干部身份证号"+t.getCardId()+"家人姓名"+t.getName()+"称谓"+title));
                                }else {
                                    uniqueSet.add(uniqueCode);
                                    investInfo.setCreateName(baseDTO.getServiceUserName());
                                    investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                                    investInfo.setOrgCode(baseDTO.getOrgCode());
                                    investInfo.setOrgName(baseDTO.getOrgName());
                                    mechanismInfoList.add(investInfo);
                                    exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                }
                            }

                        }
                    }
                } else {
                    MechanismInfo investInfo = new MechanismInfo();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
                    String title = investInfo.getTitle();
                    //字典替换
                    String checkDict = this.replaceDictId(investInfo, dictList);
                    if (StringUtils.isNotBlank(checkDict)){
                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                    }else {
                        if (uniqueSet.contains(uniqueCode)){
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复:干部身份证号"+t.getCardId()+"家人姓名"+t.getName()+"称谓"+title));
                        }else {
                            uniqueSet.add(uniqueCode);
                            investInfo.setCreateName(baseDTO.getServiceUserName());
                            investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                            investInfo.setOrgCode(baseDTO.getOrgCode());
                            investInfo.setOrgName(baseDTO.getOrgName());
                            mechanismInfoList.add(investInfo);
                            exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                        }
                    }

                }
            });
            if (!mechanismInfoList.isEmpty()) {
                this.saveBatch(mechanismInfoList);
                spouseBasicInfoService.addBatchSpouse(mechanismInfoList.stream().map(investInfo ->
                        new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(DateUtil.date()).setTenantId(baseDTO.getServiceLesseeId())
                                .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(DateUtil.date())
                                .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
                ).collect(Collectors.toList()));
            }
            return;
        }
        List<MechanismInfo> updateList = new ArrayList<>();
        uniqueSet.clear();
        Map<String, List<MechanismInfo>> infoMap = infoList.stream().collect(Collectors.groupingBy(MechanismInfo::getCardId));
        list.stream().forEach(t -> {
            List<MechanismInfo> infos = infoMap.containsKey(t.getCardId())?infoMap.get(t.getCardId()):null;
            String uniqueCode = t.getCardId() + "," + t.getName() + "," + t.getTitle();
            if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                if (CollectionUtil.isNotEmpty(infos)) {//如果不为空，进行比较
                    //校验姓名和统一社会信用代码不能为空
                    if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        String returnMessage = checkArea(t);
                        if (StringUtils.isNotBlank(returnMessage)){
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),returnMessage));
                        }else {
                            MechanismInfo info = new MechanismInfo();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(DateUtil.date());
                            //判断该干部下的其他子项名称和代码是否相同
                            long nameIndex = infos.stream().filter(e->t.getName().equals(e.getName())).count();
                            long titleIndex = infos.stream().filter(e->sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())).count();
                            long codeIndex = infos.stream().filter(e->t.getCode().equals(e.getCode())).count();
                            String title1 = info.getTitle();
                            String checkDict = this.replaceDictId(info,dictList);
                            if (StringUtils.isNotBlank(checkDict)){
                                exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            }else {
                                if (uniqueSet.contains(uniqueCode)){
                                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复:干部身份证号"+t.getCardId()+"家人姓名"+t.getName()+"称谓"+title1));
                                }else {
                                    if(nameIndex==0|titleIndex==0|codeIndex==0){ //一个都不重复
                                        //如果不相同，新增，否则就是覆盖
                                        info.setCreateTime(DateUtil.date());
                                        info.setCreateName(baseDTO.getServiceUserName());
                                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                                        info.setOrgCode(baseDTO.getOrgCode());
                                        info.setOrgName(baseDTO.getOrgName());
                                        uniqueSet.add(uniqueCode);
                                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                        mechanismInfoList.add(info);
                                    }else{ //有重复数据了
                                        MechanismInfo existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
                                                &&sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())
                                                &&t.getCode().equals(e.getCode())).findAny().orElse(null);
                                        String title = info.getTitle();
                                        if(Objects.nonNull(existInfo)){
                                            info.setId(existInfo.getId());
                                            exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                            updateList.add(info);
                                            uniqueSet.add(uniqueCode);
                                        }else if (mechanismInfoList.isEmpty()||mechanismInfoList.stream().filter(privateEquity1 -> t.getName().equals(privateEquity1.getName())&&t.getCode().equals(privateEquity1.getCode())&&title.equals(privateEquity1.getTitle())).count()==0){
                                            info.setCreateTime(DateUtil.date());
                                            info.setCreateName(baseDTO.getServiceUserName());
                                            info.setCreateAccount(baseDTO.getServiceUserAccount());
                                            exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                            mechanismInfoList.add(info);
                                            uniqueSet.add(uniqueCode);
                                        }else {
                                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复"));
                                        }
                                    }
                                }
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
                    //有此类情况
                   /* infos.stream().forEach(e -> {
                        //判断该干部下的其他子项名称和代码是否相同，不相同则添加数据库
                        if (StringUtils.isNotBlank(t.getName())&& StringUtils.isNotBlank(t.getTitle()) && StringUtils.isNotBlank(t.getCode())) {
                            //校验国家/省/市
                            checkArea(t);
                            MechanismInfo info = new MechanismInfo();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(new Date());
                            //字典替换
                            info = this.replaceDictId(info,dictList);
                            if (!t.getName().equals(e.getName()) ||!info.getTitle().equals(e.getTitle())||!t.getCode().equals(e.getCode())) {
                                info.setCreateTime(new Date());
                                mechanismInfoList.add(info);
                            } else {
                                info.setId(e.getId());
                                updateList.add(info);
                            }
                        }
                    });*/
                } else {
                    if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                        //校验国家/省/市
                        String returnMessage = checkArea(t);
                        if (StringUtils.isNotBlank(returnMessage)){
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),returnMessage));
                        }else {
                            //数据库为空，直接add
                            MechanismInfo info = new MechanismInfo();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setCreateTime(new Date())
                                    .setUpdateTime(new Date());
                            String title = info.getTitle();
                            //字典替换
                            String checkDict = this.replaceDictId(info,dictList);
                            if (StringUtils.isNotBlank(checkDict)){
                                exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            }else {
                                if (uniqueSet.contains(uniqueCode)){
                                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复:干部身份证号"+t.getCardId()+"家人姓名"+t.getName()+"称谓"+title));
                                }else {
                                    uniqueSet.add(uniqueCode);
                                    info.setCreateName(baseDTO.getServiceUserName());
                                    info.setCreateAccount(baseDTO.getServiceUserAccount());
                                    info.setOrgCode(baseDTO.getOrgCode());
                                    info.setOrgName(baseDTO.getOrgName());
                                    mechanismInfoList.add(info);
                                    exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                }

                            }

                        }

                    }
                }
            } else {
                //说明无此类情况
                MechanismInfo info = new MechanismInfo();
                BeanUtils.copyProperties(t, info);
                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setUpdateTime(new Date());
                //字典替换
                String checkDict = this.replaceDictId(info,dictList);
                if (StringUtils.isNotBlank(checkDict)){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                }else {
                    //数据库中如果不存在数据
                    if (CollectionUtil.isEmpty(infos)) {
                        info.setCreateTime(new Date());
                        info.setCreateName(baseDTO.getServiceUserName());
                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                        info.setOrgCode(baseDTO.getOrgCode());
                        info.setOrgName(baseDTO.getOrgName());
                        mechanismInfoList.add(info);//可添加到数据库中
                    } else {
                        info.setId(infos.get(0).getId());
                        updateList.add(info);
                    }
                }
            }
        });
        if (!mechanismInfoList.isEmpty()) {
            this.saveBatch(mechanismInfoList);
            spouseBasicInfoService.addBatchSpouse(mechanismInfoList.stream().map(investInfo ->
                    new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(DateUtil.date()).setTenantId(baseDTO.getServiceLesseeId())
                            .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(DateUtil.date())
                            .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
            ).collect(Collectors.toList()));
        }
        if (!updateList.isEmpty()) {
            this.updateBatchById(updateList);
            spouseBasicInfoService.addBatchSpouse(updateList.stream().map(investInfo ->
                    new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(DateUtil.date()).setTenantId(baseDTO.getServiceLesseeId())
                            .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(DateUtil.date())
                            .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
            ).collect(Collectors.toList()));
        }
    }

    @Override
    public List<CommunityServiceDTO> exportCommunityServiceExcel(CadreFamilyExportDto exportDto) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<CommunityServiceDTO> list = this.lambdaQuery().eq(StringUtils.isNotBlank(exportDto.getState()),MechanismInfo::getState, exportDto.getState())
                .like(StringUtils.isNotBlank(exportDto.getCompany()),MechanismInfo::getCompany,exportDto.getCompany())
                .like(StringUtils.isNotBlank(exportDto.getGb_name()),MechanismInfo::getGbName,exportDto.getGb_name())
                .apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.MECHANISM_INFO,exportDto.getOrgCode()))
                .orderByDesc(MechanismInfo::getUpdateTime).list().stream().map(t -> {
            CommunityServiceDTO dto = new CommunityServiceDTO();
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
        list = this.replaceDictValue(list,dictList);
        return list;
    }

    private List<CommunityServiceDTO> checkParams(List<CommunityServiceDTO> list, ExportReturnVO exportReturnVO) {
        List<String> isOrNotList = Arrays.asList(SystemConstant.WHETHER_YES, SystemConstant.WHETHER_NO);
        return list.stream().filter(e->{
            if (SystemConstant.IS_SITUATION_YES.equals(e.getIsSituation())){
//                if (StringUtils.isBlank(e.getName())||StringUtils.isBlank(e.getTitle())||StringUtils.isBlank(e.getQualificationName())
//                ||StringUtils.isBlank(e.getQualificationCode())||StringUtils.isBlank(e.getOrganizationName())||StringUtils.isBlank(e.getCode())||StringUtils.isBlank(e.getEstablishTime())
//                        ||StringUtils.isBlank(e.getRegisterCountry())||StringUtils.isBlank(e.getRegisterProvince())||StringUtils.isBlank(e.getOperatAddr())||StringUtils.isBlank(e.getOrganizationType())
//                        ||StringUtils.isBlank(e.getRegisterCapital())||StringUtils.isBlank(e.getOperatState())||StringUtils.isBlank(e.getOperatScope())||StringUtils.isBlank(e.getShareholder())||StringUtils.isBlank(e.getPractice())
//                        ||StringUtils.isBlank(e.getIsRelation())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容不能为空：姓名,称谓,执业资格名称,执业证号,开办或所在机构名称,统一社会信用代码/注册号,成立日期,注册地（国家）,注册地（省）,经营（服务）地,机构类型,注册资本（金）或资金数额（出资额）,经营状态,经营（服务）范围,是否为机构股东（合伙人、所有人等）,是否在该机构中从业,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系,填报类型,年度。"));
//                    return false;
//                }
                if (!isOrNotList.contains(e.getShareholder())||!isOrNotList.contains(e.getPractice())||!isOrNotList.contains(e.getIsRelation())){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容只能填是否：是否为机构股东（合伙人、所有人等）,是否在该机构中从业,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系。"));
                    return false;
                }
                if (SystemConstant.WHETHER_YES.equals(e.getShareholder())&&(StringUtils.isBlank(e.getPersonalCapital())||StringUtils.isBlank(e.getPersonalRatio()))){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"是为机构股东（合伙人、所有人等）时以下内容不能为空： 个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例。"));
                    return false;
                }
                if (SystemConstant.WHETHER_YES.equals(e.getPractice())&&(StringUtils.isBlank(e.getPostName()))){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"是在该机构中从业时以下内容不能为空： 所担任的职务名称。"));
                    return false;
                }
//                if (SystemConstant.WHETHER_YES.equals(e.getIsRelation())&&StringUtils.isBlank(e.getRemarks())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"该企业或其他市场主体与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系时以下内容不能为空：备注。"));
//                    return false;
//                }
                List<String> numbers = Stream.of(e.getRegisterCapital(), e.getPersonalCapital(), e.getPersonalRatio(), e.getYear()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (!NumberUtils.isAllNumeric(numbers)){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"以下内容必须为数：注册资本（金）或资金数额（出资额）,个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例,年度。"));
                    return false;
                }
                if (StringUtils.isNotBlank(e.getEstablishTime())&& CalendarUtil.greaterThanNow(e.getEstablishTime())){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"成立日期不能大于当前日期。"));
                    return false;
                }
                if (StringUtils.isNotBlank(e.getJoinTime())&& CalendarUtil.greaterThanNow(e.getJoinTime())){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"入股（合伙）时间不能大于当前日期。"));
                    return false;
                }


            }
            return true;
        }).collect(Collectors.toList());
    }

    private List<CommunityServiceDTO>  replaceDictValue(List<CommunityServiceDTO> list,List<SysDictBiz> dictList){
        list.parallelStream().forEach(t->{
            System.out.println("123--"+t.getTitle());
            //字典对应项
            String isSituation =sysDictBizService.getDictValue(t.getIsSituation(),dictList);
            String title =sysDictBizService.getDictValue(t.getTitle(),dictList);
            String organizationType =sysDictBizService.getDictValue(t.getOrganizationType(),dictList);
            String operatState =sysDictBizService.getDictValue(t.getOperatState(),dictList);
            String shareholder =sysDictBizService.getDictValue(t.getShareholder(),dictList);
            String practice =sysDictBizService.getDictValue(t.getPractice(),dictList);
            String isRelation =sysDictBizService.getDictValue(t.getIsRelation(),dictList);
            System.out.println("--"+title);
            t.setIsSituation(isSituation);
            t.setTitle(title);
            t.setOrganizationType(organizationType);
            t.setOperatState(operatState);
            t.setShareholder(shareholder);
            t.setPractice(practice);
            t.setIsRelation(isRelation);
        });
        return list;
    }

    private String  replaceDictId(MechanismInfo t,List<SysDictBiz> dictList){

        System.out.println("123--"+t.getTitle());
        String isSituation=null;
        String title=null;
        String organizationType=null;
        String operatState=null;
        String shareholder=null;
        String practice=null;
        String postType=null;

        String isRelation=null;
        //字典对应项
        if (StringUtils.isNotBlank(t.getIsSituation())){
            isSituation =sysDictBizService.getDictId(t.getIsSituation(),dictList);
            if (StringUtils.isBlank(isSituation)){
                return "有无此类情况字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(t.getTitle())){
            title =sysDictBizService.getDictId(t.getTitle(),dictList);
            if (StringUtils.isBlank(title)){
                return "称谓字典项不存在";
            }
        }

        if (StringUtils.isNotBlank(t.getOrganizationType())){
            organizationType =sysDictBizService.getDictId(t.getOrganizationType(),dictList);
            if (StringUtils.isBlank(organizationType)){
                return "机构类型字典项不存在";
            }
        }

        if (StringUtils.isNotBlank(t.getOperatState())){
            operatState =sysDictBizService.getDictId(t.getOperatState(),dictList);
            if (StringUtils.isBlank(operatState)){
                return "经营状态字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(t.getShareholder())){
            shareholder =sysDictBizService.getDictId(t.getShareholder(),dictList);
            if (StringUtils.isBlank(shareholder)){
                return "是否为机构股东（合伙人、所有人等）字典项不存在";
            }
        }

        if (StringUtils.isNotBlank(t.getPractice())){
            practice =sysDictBizService.getDictId(t.getPractice(),dictList);
            if (StringUtils.isBlank(practice)){
                return "是否在该机构中从业字典项不存在";
            }
        }

        if (StringUtils.isNotBlank(t.getIsRelation())){
            isRelation =sysDictBizService.getDictId(t.getIsRelation(),dictList);
            if (StringUtils.isBlank(isRelation)){
                return "是否与报告人所在单位（系统）直接发生过经济关系字典项不存在";
            }
        }

//        if (StringUtils.isNotBlank(t.getPostType())){
//            postType =sysDictBizService.getDictId(t.getPostType(),dictList);
//            if (StringUtils.isBlank(postType)){
//                return "干部类型字典项不存在";
//            }
//        }

        t.setIsSituation(isSituation);
        t.setTitle(title);
        t.setOrganizationType(organizationType);
        t.setOperatState(operatState);
        t.setShareholder(shareholder);
        t.setPractice(practice);
        t.setIsRelation(isRelation);
//        t.setPostType(postType);
        return null;
    }
}
