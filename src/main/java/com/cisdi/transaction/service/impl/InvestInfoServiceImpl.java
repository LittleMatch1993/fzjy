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
import com.cisdi.transaction.service.*;
import com.cisdi.transaction.util.ThreadLocalUtils;
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

    @Autowired
    private OrgService orgService;

    @Autowired
    private SpouseEnterpriseService spouseEnterpriseService;

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
    public int countByNameAndCardIdAndCode(String name, String cardId, String code) {
        Integer count = this.lambdaQuery().eq(InvestInfo::getName, name).eq(InvestInfo::getCardId, cardId).eq(InvestInfo::getCode, code).count();
        return Objects.isNull(count) ? 0 : count.intValue();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMsgUtil<Object> submitInvestInfo(SubmitDto subDto) {
        List<String> submitFailIdList = null;
        String resutStr = "提交成功";
        List<String> ids = subDto.getIds();
        List<InvestInfo> infoList = this.lambdaQuery().in(InvestInfo::getId, ids).list();
        if (CollectionUtil.isEmpty(infoList)) {
            return ResultMsgUtil.failure("数据不存在了");
        }
        /*long count = infoList.stream().filter(e -> SystemConstant.VALID_STATE.equals(e.getState())).count();
        if (count > 0) {
            return ResultMsgUtil.failure("当前表中的有效数据不能重复提交到禁止交易信息表中!");
        }*/
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        long j = infoList.stream().filter(e -> "无此类情况".equals(sysDictBizService.getDictValue(e.getIsSituation(),dictList))).count();
        if (j > 0) {
            return ResultMsgUtil.failure("当前表中的无此类情况数据不能提交到禁止交易信息表中!");
        }
        //boolean b = this.updateState(ids, SystemConstant.VALID_STATE);
        boolean b = true;

        if (b) { //投资企业或担任高级职务情况 表数据改为有效状态 并且修改成功 往 配偶，子女及其配偶表中添加数据。
            //获取干部的基本信息
            List<String> cardIds = infoList.stream().map(InvestInfo::getCardId).collect(Collectors.toList());
           // List<GbBasicInfo> gbList = gbBasicInfoService.selectBatchByCardIds(cardIds);
            //获取干部组织的基本信息
            List<GbOrgInfo> gbOrgList = null;
            try {
                //gbOrgList = gbBasicInfoService.selectGbOrgInfoByCardIds(cardIds);
                String orgCode = subDto.getOrgCode();
                gbOrgList = gbBasicInfoService.selectByOrgCodeAndCardIds(orgCode,cardIds);
            }catch (BusinessException e){
                return ResultMsgUtil.failure(e.getMsg());
            } catch (Exception e){
                e.printStackTrace();
                log.error("家属投资企业查询干部组织信息异常",e);
                return ResultMsgUtil.failure("干部组织信息查询失败");
            }
            if(CollectionUtil.isEmpty(gbOrgList)){
                //this.updateState(ids, SystemConstant.SAVE_STATE);

                return ResultMsgUtil.failure("没有找到干部组织信息",ids);
            }
            /*if(CollectionUtil.isNotEmpty(tempList)){
                infoList = infoList.stream().filter(e->tempList.contains(e.getId())).collect(Collectors.toList());
                ids = infoList.stream().map(InvestInfo::getCardId).collect(Collectors.toList());
            }*/
            banDealInfoService.deleteOnlyBanDealInfoByRefId(ids);
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
                submitFailIdList = submitFailId.stream().map(KVVO::getId).collect(Collectors.toList());
            }
            if(CollectionUtil.isNotEmpty(submitIds)){
                this.updateState(submitIds,SystemConstant.VALID_STATE);
                this.baseMapper.cleanBatchTips(submitIds);
            }
            if(!Boolean.valueOf(banDeal)){
                sj.add("数据库新增数据失败");
                return ResultMsgUtil.failure(sj.toString());
            }else{
                sj.add("提交数据成功");
                if(CollectionUtil.isNotEmpty(submitFailId)){
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
        info.setCreateName(dto.getServicePersonName());
        String orgCode = dto.getOrgCode();
        if(StrUtil.isNotEmpty(orgCode)&&orgCode.split(",").length>1){
            info.setOrgCode("70000003");
            info.setOrgName("五矿有色金属股份有限公司");
        }else{
            info.setOrgCode(orgCode);
            info.setOrgName(dto.getServiceLesseeName());
        }

        info = this.valid(info);
        //新增
        this.save(info);
        addFamilyInfo(info);
    }

    @Override
    public void addFamilyInfo(InvestInfo info) {
        List<SpouseBasicInfo> sbiList = new ArrayList<>();
        String cardId = info.getCardId();
        String infoId = info.getId();
        String name = info.getName();
        String title = info.getTitle();
        if(StrUtil.isEmpty(cardId)||StrUtil.isEmpty(name)||StrUtil.isEmpty(title)){
            return;
        }
        List<SpouseBasicInfo>  spouseList = spouseBasicInfoService.selectSpouseInfo(cardId, name, title);
        if (CollectionUtil.isNotEmpty(spouseList)){ //说明当前数据重复了
            //查看中间表是否有关联数据，如果没有就添加
            //正常情况下 spouseList 只有一个值，如果多个值之前程序bug导致的。
            SpouseBasicInfo spouseBasicInfo = spouseList.get(0);
            String sid = spouseBasicInfo.getId();
            List<SpouseEnterprise> enterprisesList = spouseEnterpriseService.selectBySpouseIdAndEnterpriseIdAndType(sid, infoId, "1");
            if(CollectionUtil.isEmpty(enterprisesList)){
                spouseEnterpriseService.insertSpouseEnterprise(sid, infoId, "1");
            }
            //则修改家属信息
            SpouseBasicInfo spouseBasic = new SpouseBasicInfo();
            spouseBasic.setId(sid);
            spouseBasic.setUpdateTime(DateUtil.date());
            spouseBasic.setName(name);
            spouseBasic.setTitle(title);
            spouseBasic.setCardName(info.getFamilyCardType());
            spouseBasic.setCardId(info.getFamilyCardId());
            spouseBasicInfoService.updateById(spouseBasic);
            return;
        }
        SpouseBasicInfo temp = new SpouseBasicInfo();
        temp.setCreateTime(DateUtil.date());
        temp.setUpdateTime(DateUtil.date());
        temp.setCadreName(info.getGbName());
        temp.setCadreCardId(cardId);
        temp.setName(name);
        temp.setTitle(title);
        temp.setCardName(info.getFamilyCardType());
        temp.setCardId(info.getFamilyCardId());
        temp.setRefId(info.getId());
        sbiList.add(temp);
        if (CollectionUtil.isNotEmpty(sbiList)) {
            //添加干部配偶，子女及其配偶数据
            try {
                spouseBasicInfoService.saveBatch(sbiList);
                //关联中间表添加数据
                spouseEnterpriseService.insertSpouseEnterprise(temp.getId(), infoId, "1");
            } catch (Exception e) {
                e.printStackTrace();
                // this.updateState(ids, SystemConstant.SAVE_STATE)
            }
        }

    }


    @Override
    public void overrideInvestInfo(String id ,InvestInfoDTO dto) {
        InvestInfo info = new InvestInfo();
        BeanUtil.copyProperties(dto,info);
        info.setId(id);
        //校验国家/省份/市
        checkArea(dto, info);
        info.setState(SystemConstant.SAVE_STATE);
        //info.setCreateTime(DateUtil.date());
        info.setUpdateTime(DateUtil.date());
        info.setTenantId(dto.getServiceLesseeId());
        info.setCreatorId(dto.getServiceUserId());

        info.setCreateAccount(dto.getServiceUserAccount());
        info.setCreateName(dto.getServicePersonName());
        info.setOrgCode(dto.getOrgCode());
        info.setOrgName(dto.getOrgName());
        info = this.valid(info);
        this.updateById(info);
        addFamilyInfo(info);

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
        addFamilyInfo(info);
        this.editRefSpouseBasic(info);
    }

    private  void editRefSpouseBasic(InvestInfo info){
        String id = info.getId();
        SpouseBasicInfo basicInfo = spouseBasicInfoService.selectByRefId(id);
        if(Objects.nonNull(basicInfo)){
            basicInfo.setUpdateTime(DateUtil.date());
            basicInfo.setCadreName(info.getGbName()); //干部姓名
            basicInfo.setCadreCardId(info.getCardId()); //干部身份证id
            basicInfo.setName(info.getName()); //家属姓名
            basicInfo.setTitle(info.getTitle());//家属关系
            basicInfo.setCardName(info.getFamilyCardType());
            basicInfo.setCardId(info.getFamilyCardId());
            basicInfo.setRefId(info.getId()); //关联数据id
            spouseBasicInfoService.updateById(basicInfo);
        }
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
                if (StringUtils.isBlank(dto.getRegisterProvince())) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份或地级市信息不能为空");
                    return dto.getRegisterCountry() + ":" + "国家下的省份信息不能为空";
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
//                    return dto.getRegisterProvince() + ":" + "省份下的地级市不匹配";
                }
            }
            return null;

    }

    private String checkImportArea(InvestmentDTO dto) {
        //校验国家/省份/市是否合法
        //国家
        if (StringUtils.isBlank(dto.getRegisterCountry())) {
//                throw new RuntimeException("在有此类情况下，注册地国家信息不能为空");
            return "在有此类情况下，注册地国家信息不能为空;";
        }
        GlobalCityInfo country = globalCityInfoService.lambdaQuery().eq(GlobalCityInfo::getName, dto.getRegisterCountry()).last(SqlConstant.ONE_SQL).one();
        if (country == null) {
//                throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家不存在");
            return dto.getRegisterCountry() + ":" + "国家不存在;";
        }
        //如果是中国下的，校验省份和市
        if (country.getAreaCode().equals(SystemConstant.CHINA_AREA_CODE)) {
            if (StringUtils.isBlank(dto.getRegisterProvince())) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份或地级市信息不能为空");
                return dto.getRegisterCountry() + ":" + "国家下的省份信息不能为空;";
            }
            //省份
            GlobalCityInfo province = globalCityInfoService.lambdaQuery().eq(GlobalCityInfo::getName, dto.getRegisterProvince()).eq(GlobalCityInfo::getParentId,country.getCountryId()).last(SqlConstant.ONE_SQL).one();

            if (Objects.isNull(province)) {
//                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份和地级市不存在");
                return dto.getRegisterCountry() + ":" + "国家下的省份不存在;";
            }
            //Map<String, GlobalCityInfo> infoMap = infoList.stream().collect(Collectors.toMap(GlobalCityInfo::getParentId, Function.identity()));
            //Map<String, List<GlobalCityInfo>> collect = infoList.stream().collect(Collectors.groupingBy(GlobalCityInfo::getParentId));
            //GlobalCityInfo info = infoMap.get(country.getCountryId());//省份
            //市
            if (StringUtils.isNotBlank(dto.getCity())){
                GlobalCityInfo city = globalCityInfoService.lambdaQuery().eq(GlobalCityInfo::getName, dto.getCity()).isNull(GlobalCityInfo::getCountryId).last(SqlConstant.ONE_SQL).one();
                if (Objects.isNull(city)){
                    return dto.getRegisterProvince() + ":" + "省份下的市不存在;";
                }
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
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchInvestmentInfo(List<InvestmentDTO> list, BaseDTO baseDTO, ExportReturnVO exportReturnVO) {
        final String orgCode;
        final String orgName;
        if (baseDTO.getOrgCode().contains(",")){
            orgCode="70000003";
            orgName="五矿有色金属股份有限公司";
        }else {
            orgCode=baseDTO.getOrgCode();
            orgName=baseDTO.getOrgName();
        }
        //过滤掉必填校验未通过的字段
//        List<InvestmentDTO> list = lists.stream().filter(e -> StringUtils.isBlank(e.getMessage())).collect(Collectors.toList());
//        List<InvestmentDTO> list = lists;
        //部分参数校验
        checkParams(list,exportReturnVO,baseDTO.getOrgCode());
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        //获取所有字典信息
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        //新增的内容
        List<InvestInfo> investInfoList = new ArrayList<>();
        //筛选出没有错误内容的干部身份证号
        List<String> cardIds = list.stream().filter(e->StringUtils.isBlank(e.getMessage())).map(InvestmentDTO::getCardId).distinct().collect(Collectors.toList());
        //通过干部身份证号查询已经存在的投资企业或担任高级职务情况
        List<InvestInfo> infoList = CollectionUtils.isEmpty(cardIds)?Lists.newArrayList():this.lambdaQuery().in(InvestInfo::getCardId, cardIds).list();

        //去重集合：通过干部身份证号、家人姓名、称谓、统一社会信用代码/注册号
        Set<String> uniqueSet=new HashSet<>();
        Date date=new Date();
        if (infoList.isEmpty()) {
            list.forEach(t -> {
                //失败信息
                String message = t.getMessage();
                //国家省市校验
                String returnMessage = checkImportArea(t);
                InvestInfo investInfo = new InvestInfo();
                BeanUtils.copyProperties(t, investInfo);
                investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setCreateTime(new Date())
                        .setUpdateTime(new Date());
                String title = investInfo.getTitle();
                String checkDict = this.repalceDictId(investInfo,dictList);
                String uniqueCode = t.getCardId() + "," + t.getName() + "," + title+","+t.getCode();
                //去重校验提示信息
                String uniqueCheckMessage = "数据重复:干部身份证号" + t.getCardId() + ",家人姓名" + t.getName() + ",称谓" + title + ",统一社会信用代码/注册号" + t.getCode() + ";";
                //失败信息
                String failMessage=null;
                //是否需要添加失败信息
                boolean isAddFailMessage=false;
                //如果上面校验后无失败信息，则做字典、国家省市校验和替换，且做去重校验
                if (StringUtils.isBlank(message)){
                    if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                        if (StringUtils.isNotBlank(t.getName()) && StringUtils.isNotBlank(t.getCode())) {
                            //校验国家/省/市
                            if (StringUtils.isBlank(returnMessage)){
                                if (StringUtils.isBlank(checkDict)){
                                    if (uniqueSet.contains(uniqueCode)){
                                        failMessage=uniqueCheckMessage;
                                        isAddFailMessage = true;
                                    }else {
                                        uniqueSet.add(uniqueCode);
                                        investInfo.setCreateName(baseDTO.getServicePersonName());
                                        investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                                        investInfo.setOrgCode(baseDTO.getOrgCode());
                                        investInfo.setOrgName(baseDTO.getOrgName());
                                        long time = date.getTime() + 1000;
                                        investInfo.setCreateTime(DateUtil.date(time));
                                        investInfo.setUpdateTime(DateUtil.date(time));
                                        date.setTime(time);
                                        investInfoList.add(investInfo);
                                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                    }

                                }else {
                                    failMessage=uniqueSet.contains(uniqueCode)?(checkDict+uniqueCheckMessage):checkDict;
                                    isAddFailMessage = true;
                                }
                            }else {
                                failMessage=returnMessage+(StringUtils.isBlank(checkDict)?"":checkDict)+(uniqueSet.contains(uniqueCode)? uniqueCheckMessage :"");
                                isAddFailMessage = true;
                            }
                        }
                    } else {
//                        InvestInfo investInfo = new InvestInfo();
//                        BeanUtils.copyProperties(t, investInfo);
//                        investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
//                                .setCreateTime(new Date())
//                                .setUpdateTime(new Date());
//                        String title = investInfo.getTitle();
//                        String checkDict = this.repalceDictId(investInfo,dictList);
//                        if (StringUtils.isBlank(checkDict)){
//                            if (uniqueSet.contains(uniqueCode)){
//                                exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                                exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复:干部身份证号"+t.getCardId()+",家人姓名"+t.getName()+",称谓"+title+",统一社会信用代码/注册号"+t.getCode()));
//                            }else {
//                                uniqueSet.add(uniqueCode);
//                                investInfo.setCreateName(baseDTO.getServicePersonName());
//                                investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
//                                investInfo.setOrgCode(baseDTO.getOrgCode());
//                                investInfo.setOrgName(baseDTO.getOrgName());
//                                long time = date.getTime() + 1000;
//                                investInfo.setCreateTime(DateUtil.date(time));
//                                investInfo.setUpdateTime(DateUtil.date(time));
//                                date.setTime(time);
//                                investInfoList.add(investInfo);
//                                exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
//                            }
//                        }else {
//                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
//                        }

                    }
                    //新增失败内容
                    if (isAddFailMessage){
                        exportReturnVO.addFailContent(t.getColumnNumber(),failMessage);
                    }
                }else {
                    //已经存在失败信息的列做字典、国家省市、唯一校验失败信息添加
                    exportReturnVO.getFailMessage().stream().filter(exportReturnMessageVO -> exportReturnMessageVO.getColumn().equals(t.getColumnNumber())).forEach(exportReturnMessageVO -> {
                        exportReturnMessageVO.setMessage(exportReturnMessageVO.getMessage()+(returnMessage==null?"":returnMessage)+(StringUtils.isBlank(checkDict)?"":checkDict)+(uniqueSet.contains(uniqueCode)? uniqueCheckMessage :""));
                    });
                }
            });
            this.saveData(baseDTO, orgCode, orgName, investInfoList);
            return;
        }
        //修改的内容
        List<InvestInfo> updateList = new ArrayList<>();
        Map<String, List<InvestInfo>> infoMap = infoList.stream().collect(Collectors.groupingBy(InvestInfo::getCardId));
        list.forEach(t -> {
            //List<InvestInfo> infos = infoMap.get(t.getCardId());
            String message = t.getMessage();
            //校验国家/省/市
            String returnMessage = checkImportArea(t);
            String checkDict="";
            if (StringUtils.isBlank(message)) {
                List<InvestInfo> infos = infoMap.getOrDefault(t.getCardId(), null);
                String uniqueCode = t.getCardId() + "," + t.getName() + "," + t.getTitle()+","+t.getCode();
                InvestInfo info = new InvestInfo();
                BeanUtils.copyProperties(t, info);
                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setUpdateTime(DateUtil.date());
                //判断该干部下的其他子项名称和代码是否相同
                long nameIndex = infos.stream().filter(e->t.getName().equals(e.getName())).count();
                long titleIndex = infos.stream().filter(e->sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())).count();
                long codeIndex = infos.stream().filter(e->t.getCode().equals(e.getCode())).count();
                String title1 = info.getTitle();
                checkDict = this.repalceDictId(info,dictList);
                String uniqueCheckMessage = "数据重复:干部身份证号" + t.getCardId() + ",家人姓名" + t.getName() + ",称谓" + title1 + ",统一社会信用代码/注册号" + t.getCode() + ";";
                String failMessage=null;
                boolean isAddFailMessage=false;
                if (CollectionUtil.isNotEmpty(infos)) {//如果不为空，进行比较
                    //校验姓名和统一社会信用代码不能为空
                    if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())  && StringUtils.isNotBlank(t.getCode())) {
                        if (StringUtils.isBlank(returnMessage)){
                            if (StringUtils.isBlank(checkDict)){
                                if(nameIndex==0||titleIndex==0||codeIndex==0){ //一个都不重复
                                    if (uniqueSet.contains(uniqueCode)){
                                        isAddFailMessage=true;
                                        failMessage=uniqueCheckMessage;
                                    }else {
                                        //如果不相同，新增，否则就是覆盖
                                        uniqueSet.add(uniqueCode);
                                        long time = date.getTime() + 1000;
                                        info.setCreateTime(DateUtil.date(time));
                                        info.setUpdateTime(DateUtil.date(time));
                                        date.setTime(time);
                                        info.setCreateName(baseDTO.getServicePersonName());
                                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                                        info.setOrgCode(baseDTO.getOrgCode());
                                        info.setOrgName(baseDTO.getOrgName());
                                        investInfoList.add(info);
                                        exportReturnVO.addSuccessNumber();
                                    }
                                }else{ //有重复数据了
                                    InvestInfo existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
                                            &&sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())
                                            &&t.getCode().equals(e.getCode())).findAny().orElse(null);
                                    String title = info.getTitle();
                                    if(Objects.nonNull(existInfo)){
                                        if (uniqueSet.contains(uniqueCode)){
                                            isAddFailMessage=true;
                                            failMessage=uniqueCheckMessage;
                                        }else {
                                            uniqueSet.add(uniqueCode);
                                            info.setId(existInfo.getId());
                                            long time = date.getTime() + 1000;
                                            info.setCreateTime(DateUtil.date(time));
                                            info.setUpdateTime(DateUtil.date(time));
                                            date.setTime(time);
                                            updateList.add(info);
                                            exportReturnVO.addSuccessNumber();
                                        }

                                    }else if (investInfoList.isEmpty()||investInfoList.stream().filter(privateEquity1 -> t.getName().equals(privateEquity1.getName())&&t.getCode().equals(privateEquity1.getCode())&&title.equals(privateEquity1.getTitle())).count()==0){
                                        if (uniqueSet.contains(uniqueCode)){
                                            isAddFailMessage=true;
                                            failMessage=uniqueCheckMessage;
                                        }else {
                                            uniqueSet.add(uniqueCode);
                                            long time = date.getTime() + 1000;
                                            info.setCreateTime(DateUtil.date(time));
                                            info.setUpdateTime(DateUtil.date(time));
                                            date.setTime(time);
                                            info.setCreateName(baseDTO.getServicePersonName());
                                            info.setCreateAccount(baseDTO.getServiceUserAccount());
                                            info.setOrgCode(baseDTO.getOrgCode());
                                            info.setOrgName(baseDTO.getOrgName());
                                            investInfoList.add(info);
                                            exportReturnVO.addSuccessNumber();
                                        }

                                    }else {
                                        isAddFailMessage=true;
                                        failMessage="数据重复;";
                                    }
                                }
                            }else {
                                isAddFailMessage=true;
                                failMessage=checkDict+(uniqueSet.contains(uniqueCode)? uniqueCheckMessage :"");
                            }

                        }else {
                            isAddFailMessage=true;
                            failMessage=returnMessage+(StringUtils.isBlank(checkDict)?"":checkDict)+(uniqueSet.contains(uniqueCode)? uniqueCheckMessage :"");
                        }

                        /*if (!t.getName().equals(e.getName()) &&!info.getTitle().equals(e.getTitle())&& !t.getCode().equals(e.getCode())) {
                            //如果不相同，新增，否则就是覆盖
                            info.setCreateTime(DateUtil.date());
                            investInfoList.add(info);
                        } else {
                            info.setId(e.getId());
                            updateList.add(info);
                        }*/
                    }else {
                        final String dictCode=checkDict;
                        exportReturnVO.getFailMessage().stream().filter(exportReturnMessageVO -> exportReturnMessageVO.getColumn().equals(t.getColumnNumber()))
                                .forEach(exportReturnMessageVO -> {
                                    exportReturnMessageVO.setMessage(exportReturnMessageVO.getMessage()+(returnMessage==null?"":returnMessage)+(StringUtils.isBlank(dictCode)?"":dictCode)+(uniqueSet.contains(uniqueCode)? uniqueCheckMessage :""));
                                });
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
                        if (StringUtils.isNotBlank(returnMessage)){
                            isAddFailMessage=true;
                            failMessage=returnMessage+(StringUtils.isBlank(checkDict)?"":checkDict)+(uniqueSet.contains(uniqueCode)? uniqueCheckMessage :"");
                        }else {
                            //数据库为空，直接add
                            if (StringUtils.isBlank(checkDict)){
                                if (uniqueSet.contains(uniqueCode)){
                                    isAddFailMessage=true;
                                    failMessage=uniqueCheckMessage;
                                }else {
                                    uniqueSet.add(uniqueCode);
                                    info.setState(SystemConstant.SAVE_STATE);
                                    long time = date.getTime() + 1000;
                                    info.setCreateTime(DateUtil.date(time));
                                    info.setUpdateTime(DateUtil.date(time));
                                    date.setTime(time);
                                    info.setCreateName(baseDTO.getServicePersonName());
                                    info.setCreateAccount(baseDTO.getServiceUserAccount());
                                    info.setOrgCode(baseDTO.getOrgCode());
                                    info.setOrgName(baseDTO.getOrgName());
                                    investInfoList.add(info);
                                    exportReturnVO.addSuccessNumber();
                                }
                            }else {
                                isAddFailMessage=true;
                                failMessage=checkDict+(uniqueSet.contains(uniqueCode)? uniqueCheckMessage :"");
                            }
                        }
                    }
                }
                if (isAddFailMessage){
                    exportReturnVO.addFailContent(t.getColumnNumber(),failMessage);
                }
            } else {
//                //说明无此类情况
//                InvestInfo info = new InvestInfo();
//                BeanUtils.copyProperties(t, info);
//                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
//                        .setUpdateTime(DateUtil.date());
//                String checkDict = this.repalceDictId(info,dictList);
//                if (StringUtils.isBlank(checkDict)){
//                    //数据库中如果不存在数据
//                    if (CollectionUtil.isEmpty(infos)) {
//                        long time = date.getTime() + 1000;
//                        info.setCreateTime(DateUtil.date(time));
//                        info.setUpdateTime(DateUtil.date(time));
//                        date.setTime(time);
//                        info.setCreateName(baseDTO.getServicePersonName());
//                        info.setCreateAccount(baseDTO.getServiceUserAccount());
//                        info.setOrgCode(baseDTO.getOrgCode());
//                        info.setOrgName(baseDTO.getOrgName());
//                        investInfoList.add(info);
//                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
//                    } else {
//                        //覆盖
//                        info.setId(infos.get(0).getId());
//                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
//                        long time = date.getTime() + 1000;
//                        info.setCreateTime(DateUtil.date(time));
//                        info.setUpdateTime(DateUtil.date(time));
//                        date.setTime(time);
//                        updateList.add(info);
//                    }
//                }else {
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
//                }
                final String dictCode=checkDict;
                exportReturnVO.getFailMessage().stream().filter(exportReturnMessageVO -> exportReturnMessageVO.getColumn().equals(t.getColumnNumber())).forEach(exportReturnMessageVO -> {
                    exportReturnMessageVO.setMessage(exportReturnMessageVO.getMessage()+(returnMessage==null?"":returnMessage)+(StringUtils.isBlank(dictCode)?"":dictCode));
                });
            }

        });
        //保存数据
        this.saveData(baseDTO, orgCode, orgName, investInfoList);
        //修改数据
        this.updateData(baseDTO, orgCode, orgName, updateList);
    }

    /**
     * 修改数据
     * @param baseDTO
     * @param orgCode
     * @param orgName
     * @param updateList
     */
    private void updateData(BaseDTO baseDTO, String orgCode, String orgName, List<InvestInfo> updateList) {
        if (!updateList.isEmpty()) {
            //初始化组织机构信息
            updateList.forEach(mechanismInfo -> {
                mechanismInfo.setOrgCode(orgCode);
                mechanismInfo.setOrgName(orgName);
            });
            this.updateBatchById(updateList);
            //保存家人信息
            spouseBasicInfoService.addBatchSpouse(updateList.stream().map(investInfo ->
                    new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(investInfo.getCreateTime()).setTenantId(baseDTO.getServiceLesseeId())
                            .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(investInfo.getUpdateTime())
                            .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
            ).collect(Collectors.toList()), SystemConstant.INVESTMENT);
        }
    }

    /**
     * 保存数据
     * @param baseDTO
     * @param orgCode
     * @param orgName
     * @param investInfoList
     */
    private void saveData(BaseDTO baseDTO, String orgCode, String orgName, List<InvestInfo> investInfoList) {
        if (!investInfoList.isEmpty()) {
            //初始化组织机构信息
            investInfoList.forEach(mechanismInfo -> {
                mechanismInfo.setOrgCode(orgCode);
                mechanismInfo.setOrgName(orgName);
            });
            this.saveBatch(investInfoList);
            //保存家人信息
            spouseBasicInfoService.addBatchSpouse(investInfoList.stream().map(investInfo ->
                    new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(investInfo.getCreateTime()).setTenantId(baseDTO.getServiceLesseeId())
                            .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(investInfo.getUpdateTime())
                            .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
            ).collect(Collectors.toList()), SystemConstant.INVESTMENT);
        }
    }

    @Override
    public List<InvestmentDTO> exportInvestmentExcel(CadreFamilyExportDto exportDto) {

        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<InvestmentDTO> list = this.list(new QueryWrapper<InvestInfo>()
                .orderBy(StringUtils.isNotBlank(exportDto.getColumnName())&&Objects.nonNull(exportDto.getIsAsc()),exportDto.getIsAsc(),exportDto.getColumnName())
                .orderByDesc(StringUtils.isBlank(exportDto.getColumnName())||Objects.isNull(exportDto.getIsAsc()),"create_time")
                .lambda()
                .eq(StringUtils.isNotBlank(exportDto.getState()),InvestInfo::getState, exportDto.getState())
                .like(StringUtils.isNotBlank(exportDto.getCompany()),InvestInfo::getCompany,exportDto.getCompany())
                .like(StringUtils.isNotBlank(exportDto.getGb_name()),InvestInfo::getGbName,exportDto.getGb_name())
                .in(CollectionUtil.isNotEmpty(exportDto.getCreate_account()),InvestInfo::getCreateAccount,exportDto.getCreate_account())
                .apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.INVEST_INFO,exportDto.getOrgCode()))
        ).stream().map(t -> {
            InvestmentDTO dto = new InvestmentDTO();
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
        list = this.repalceDictValue(list,dictList);
        return list;
    }

    @Override
    public List<KVVO> getCreateInfoForInvest(String orgCode) {
        if(StrUtil.isEmpty(orgCode)){
            return new ArrayList<>();
        }
        List<String> orgCodeList = Arrays.stream(orgCode.split(",")).distinct().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if(orgCodeList.size()>1){
            orgCode = "70000003";//系统中只有pizd整个账号会传多个orgCode.他的主组织编码是70000003
        }
        Org org = orgService.selectByOrgancode(orgCode);
        List<InvestInfo> list = null;
        String asglevel = org.getAsglevel();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")) { //看所有
            QueryWrapper<InvestInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("DISTINCT  create_account","create_name");
            list = this.baseMapper.selectList(queryWrapper);
        }else{
            String asgpathnamecode = org.getAsgpathnamecode();
            List<String > cardIds = orgService.getCardIdsByAsgpathnamecode(asgpathnamecode);
            if(CollectionUtil.isEmpty(cardIds)){
                return new ArrayList<>();
            }
            cardIds.add("-9999qq");//
            QueryWrapper<InvestInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("DISTINCT  create_account","create_name").in( "card_id",cardIds);
            list = this.baseMapper.selectList(queryWrapper);
        }
        List<KVVO> resultList = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(list)){
            List<AuthUser> authUsers = gbBasicInfoService.selectAuthUser();
            if(CollectionUtil.isEmpty(authUsers)){
                return new ArrayList<>();
            }
            for (InvestInfo info : list) {
                if(Objects.isNull(info)){
                    continue;
                }
                String account = info.getCreateAccount();
                String userName= info.getCreateName();
               // String orgName = info.getOrgName();
                if(StrUtil.isEmpty(account)){
                    continue;
                }
                AuthUser authUser = authUsers.stream().filter(e -> account.equals(e.getUserName())).findAny().orElse(null);
                if(Objects.isNull(authUser)){
                    continue;
                }
                KVVO kvvo = new KVVO();
                kvvo.setId(account);
                kvvo.setName(userName+"-"+authUser.getUnit());
                resultList.add(kvvo);
            }
        }

        return resultList;
    }

    private void checkParams(List<InvestmentDTO> list, ExportReturnVO exportReturnVO,String orgCode) {
        List<String> isOrNotList = Arrays.asList(SystemConstant.WHETHER_YES, SystemConstant.WHETHER_NO);
        List<String> cardIds = Optional.ofNullable(gbBasicInfoService.selectNoAuthCardIds(orgCode)).orElse(Lists.newArrayList());
        Map<Integer, String> columnMessageMap = exportReturnVO.getFailMessage().stream().collect(Collectors.toMap(ExportReturnMessageVO::getColumn, ExportReturnMessageVO::getMessage));
        list.forEach(e->{
            boolean haveColumn = columnMessageMap.containsKey(e.getColumnNumber());
            StringBuilder message=new StringBuilder(haveColumn?columnMessageMap.get(e.getColumnNumber()):"");
            if (SystemConstant.IS_SITUATION_YES.equals(e.getIsSituation())){
//                if (StringUtils.isBlank(e.getName())||StringUtils.isBlank(e.getTitle())||StringUtils.isBlank(e.getEnterpriseName())||StringUtils.isBlank(e.getEnterpriseType())||StringUtils.isBlank(e.getCode())||StringUtils.isBlank(e.getEstablishTime())||StringUtils.isBlank(e.getRegisterCountry())||StringUtils.isBlank(e.getRegisterProvince())
//                ||StringUtils.isBlank(e.getOperatAddr())||StringUtils.isBlank(e.getRegisterCapital())||StringUtils.isBlank(e.getEnterpriseState())||StringUtils.isBlank(e.getOperatScope())||StringUtils.isBlank(e.getShareholder())||StringUtils.isBlank(e.getSeniorPosition())||StringUtils.isBlank(e.getIsRelation())||StringUtils.isBlank(e.getTbType())||StringUtils.isBlank(e.getYear())
//                ){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容不能为空：姓名,称谓,企业或其他市场主体名称,企业或其他市场主体类型,统一社会信用代码/注册号,成立日期,注册地（国家）,注册地（省）,注册资本或资金数额,企业状态,经营范围,是否为股东（合伙人、所有人）,是否担任高级职务,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系,填报类型,年度。"));
//                    return false;
//                }
                if ((!message.toString().contains("是否为股东（合伙人、所有人）")&&!isOrNotList.contains(e.getShareholder()))||(!message.toString().contains("是否担任高级职务")&&!isOrNotList.contains(e.getSeniorPosition()))||(StringUtils.isNotBlank(e.getIsRelation())&&!isOrNotList.contains(e.getIsRelation()))){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容只能填是否：是否为股东（合伙人、所有人）,是否担任高级职务,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系。"));
//                    return false;
                    message.append("有此类情况时以下内容只能填是否：是否为股东（合伙人、所有人）,是否担任高级职务,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系;");
                }
                if (SystemConstant.WHETHER_YES.equals(e.getShareholder())&&(StringUtils.isBlank(e.getPersonalCapital())||StringUtils.isBlank(e.getPersonalRatio()))){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"是为机构股东（合伙人、所有人等）时以下内容不能为空： 个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例。"));
//                    return false;
                    message.append("是为机构股东（合伙人、所有人等）时以下内容不能为空： 个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例;");
                }
                if (SystemConstant.WHETHER_YES.equals(e.getSeniorPosition())&&(StringUtils.isBlank(e.getSeniorPositionName())||StringUtils.isBlank(e.getSeniorPositionStartTime()))){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"担任高级职务时以下内容不能为空： 所担任的高级职务名称,担任高级职务的开始时间。"));
//                    return false;
                    message.append("担任高级职务时以下内容不能为空： 所担任的高级职务名称,担任高级职务的开始时间;");
                }
//                if (SystemConstant.WHETHER_YES.equals(e.getIsRelation())&&StringUtils.isBlank(e.getRemarks())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系时以下内容不能为空：备注。"));
//                    return false;
//                }
                List<String> numbers = Stream.of(e.getRegisterCapital(), e.getPersonalCapital(), e.getPersonalRatio(), e.getYear()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (!NumberUtils.isAllNumeric(numbers)){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"以下内容必须为正数：注册资本（金）或资金数额（出资额）,个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例,年度。"));
//                    return false;
                    message.append("以下内容必须为正数：注册资本（金）或资金数额（出资额）,个人认缴出资额或个人出资额,个人认缴出资比例或个人出资比例,年度;");
                }else {
                    String year = e.getYear();
                    if (StringUtils.isNotBlank(year)&&year.contains(".")){
//                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"年份不能为小数。"));
//                        return false;
                        message.append("年份不能为小数;");
                    }
                    if (StringUtils.isNotBlank(year)&&!year.contains(".")&&Integer.parseInt(year)>Calendar.getInstance().get(Calendar.YEAR)){
//                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"年份不能大于当前年份。"));
//                        return false;
                        message.append("年份不能大于当前年份;");
                    }
                }
                if (StringUtils.isNotBlank(e.getEstablishTime())&&!message.toString().contains("成立日期")&& CalendarUtil.greaterThanNow(e.getEstablishTime())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"成立日期不能大于当前日期。"));
//                    return false;
                    message.append("成立日期不能大于当前日期;");
                }
                if (StringUtils.isNotBlank(e.getInvestTime())&&!message.toString().contains("投资时间")&& CalendarUtil.greaterThanNow(e.getInvestTime())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"投资时间不能大于当前日期。"));
//                    return false;
                    message.append("投资时间不能大于当前日期;");
                }
                if (StringUtils.isNotBlank(e.getSeniorPositionStartTime())){
                    if (!message.toString().contains("担任高级职务的开始时间")&&CalendarUtil.greaterThanNow(e.getSeniorPositionStartTime())){
//                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"担任高级职务的开始时间不能大于当前日期。"));
//                        return false;
                        message.append("担任高级职务的开始时间不能大于当前日期;");
                    }
                    if (StringUtils.isNotBlank(e.getSeniorPositionEndTime())&&!message.toString().contains("担任高级职务的开始时间")&&!message.toString().contains("担任高级职务的结束时间")&&CalendarUtil.compare(e.getSeniorPositionStartTime(),e.getSeniorPositionEndTime())){
//                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"担任高级职务的结束时间不能大于开始时间。"));
//                        return false;
                        message.append("担任高级职务的结束时间不能大于开始时间;");
                    }
                }else if (StringUtils.isNotBlank(e.getSeniorPositionEndTime())&&!message.toString().contains("担任高级职务的结束时间")&&CalendarUtil.greaterThanNow(e.getSeniorPositionEndTime())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"担任高级职务的结束时间不能大于当前时间。"));
                    message.append("担任高级职务的结束时间不能大于当前时间;");
                }
                if (cardIds.contains(e.getCardId())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"没有当前干部权限:"+e.getCardId()+"。"));
                    message.append("没有当前干部权限:").append(e.getCardId()).append(";");
                }
                int columnNumber = e.getColumnNumber();
                if (haveColumn){
                    exportReturnVO.getFailMessage().stream().filter(exportReturnMessageVO -> exportReturnMessageVO.getColumn().equals(columnNumber)).forEach(exportReturnMessageVO -> exportReturnMessageVO.setMessage(message.toString()));
                }else if (message.length()>0){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(columnNumber, message.toString()));
                }
            }
            e.setMessage(message.toString());
        });
    }

    private List<InvestmentDTO>  repalceDictValue(List<InvestmentDTO> list,List<SysDictBiz> dictList){
        list.parallelStream().forEach(dto->{
            //字典对应项
            String isSituation = sysDictBizService.getDictValue(dto.getIsSituation(), dictList);
            String title = sysDictBizService.getDictValue(dto.getTitle(), dictList);
            String enterpriseState = sysDictBizService.getDictValue(dto.getEnterpriseState(), dictList,"1552585398045290496");
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
       // String postType=null;
        StringBuilder returnMessage=new StringBuilder();
        if (StringUtils.isNotBlank(dto.getIsSituation())){
            isSituation=sysDictBizService.getDictId(dto.getIsSituation(),dictList);
            if (StringUtils.isBlank(isSituation)){
//                return "有无此类情况字典项不存在";
                returnMessage.append("有无此类情况字典项不存在;");
            }
        }
        if (StringUtils.isNotBlank(dto.getTitle())){
            title=sysDictBizService.getDictId(dto.getTitle(),dictList);
            if (StringUtils.isBlank(title)){
//                return "称谓字典项不存在";
                returnMessage.append("称谓字典项不存在;");
            }
        }
        if (StringUtils.isNotBlank(dto.getEnterpriseState())){
            enterpriseState=sysDictBizService.getEnterpriseStateDictId(dto.getEnterpriseState(),dictList);
            if (StringUtils.isBlank(enterpriseState)){
//                return "企业状态字典项不存在";
                returnMessage.append("企业状态字典项不存在;");
            }
        }
        if (StringUtils.isNotBlank(dto.getEnterpriseType())){
            enterpriseType=sysDictBizService.getDictId(dto.getEnterpriseType(),dictList);
            if (StringUtils.isBlank(enterpriseType)){
//                return "企业或其他市场主体类型字典项不存在";
                returnMessage.append("企业或其他市场主体类型字典项不存在;");
            }
        }
        if (StringUtils.isNotBlank(dto.getShareholder())){
            shareholder=sysDictBizService.getDictId(dto.getShareholder(),dictList);
            if (StringUtils.isBlank(shareholder)){
//                return "是否为股东（合伙人、所有人）字典项不存在";
                returnMessage.append("是否为股东（合伙人、所有人）字典项不存在;");
            }
        }
        if (StringUtils.isNotBlank(dto.getSeniorPosition())){
            seniorPosition=sysDictBizService.getDictId(dto.getSeniorPosition(),dictList);
            if (StringUtils.isBlank(seniorPosition)){
//                return "是否担任高级职务字典项不存在";
                returnMessage.append("是否担任高级职务字典项不存在;");
            }
        }
        if (StringUtils.isNotBlank(dto.getIsRelation())){
            isRelation=sysDictBizService.getDictId(dto.getIsRelation(),dictList);
            if (StringUtils.isBlank(isRelation)){
//                return "该企业或其他市场主体是否与报告人所在单位字典项不存在";
                returnMessage.append("该企业或其他市场主体是否与报告人所在单位字典项不存在;");
            }
        }
//        if (StringUtils.isNotBlank(dto.getPostType())){
//            postType =sysDictBizService.getDictId(dto.getPostType(),dictList);
//            if (StringUtils.isBlank(postType)){
//                return "干部类型字典项不存在";
//            }
//        }
        dto.setIsSituation(isSituation);
        dto.setTitle(title);
        dto.setEnterpriseState(enterpriseState);
        dto.setEnterpriseType(enterpriseType);
        dto.setShareholder(shareholder);
        dto.setSeniorPosition(seniorPosition);
        dto.setIsRelation(isRelation);
//        dto.setPostType(postType);
        return StringUtils.isBlank(returnMessage)?null:returnMessage.toString();
    }
}