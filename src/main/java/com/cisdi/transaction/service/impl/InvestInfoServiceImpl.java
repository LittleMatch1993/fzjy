package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.InvestInfoDTO;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.dto.SubmitDto;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.mapper.master.InvestInfoMapper;
import com.cisdi.transaction.service.*;
import com.cisdi.transaction.util.ThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        List<InvestInfo> list = ids.stream().map(e -> new InvestInfo().setId(e).setState(state)).collect(Collectors.toList());
        boolean b = this.updateBatchById(list);
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
        //boolean b = this.updateState(ids, SystemConstant.VALID_STATE);
        boolean b = true;

        if (b) { //投资企业或担任高级职务情况 表数据改为有效状态 并且修改成功 往 配偶，子女及其配偶表中添加数据。
            // 配偶，子女及其配偶表中添加数据。如果 干部身份证号 姓名 称谓 重复则不添加
            List<SpouseBasicInfo> sbInfoList = spouseBasicInfoService.selectAll();//查询所有干部家属信息
            List<SpouseBasicInfo> sbiList = new ArrayList<>();
            for (InvestInfo info : infoList) {
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
                    this.updateState(ids, SystemConstant.SAVE_STATE);
                    return ResultMsgUtil.failure("添加家属信息失败");
                }
            }
            banDealInfoService.deleteBanDealInfoByRefId(ids);
            //获取干部的基本信息
            //List<String> cardIds = infoList.stream().map(InvestInfo::getCardId).collect(Collectors.toList());
           // List<GbBasicInfo> gbList = gbBasicInfoService.selectBatchByCardIds(cardIds);
            //获取干部组织的基本信息
            List<GbOrgInfo> gbOrgList = null;
            try {
                //gbOrgList = gbBasicInfoService.selectGbOrgInfoByCardIds(cardIds);
                String orgCode = subDto.getOrgCode();
                gbOrgList = gbBasicInfoService.selectByOrgode(orgCode);
            }catch (Exception e){
                e.printStackTrace();
                return ResultMsgUtil.failure("干部组织信息查询失败");
            }
            if(CollectionUtil.isEmpty(gbOrgList)){
                this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("没有找到干部组织信息");
            }

            //向禁止交易信息表中添加数据 并进行验证 及其他逻辑处理
            ResultMsgUtil<Map<String, Object>> mapResult = banDealInfoService.insertBanDealInfoOfInvestInfo(infoList, gbOrgList);
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
        info.setOrgCode(dto.getServiceLesseeId());
        info.setOrgName(dto.getServiceLesseeName());
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
        info.setOrgCode(dto.getServiceLesseeId());
        info.setOrgName(dto.getServiceLesseeName());
        this.save(info);

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
                        investInfo = this.repalceDictId(investInfo,dictList);
                        investInfoList.add(investInfo);
                    }
                } else {
                    InvestInfo investInfo = new InvestInfo();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
                    investInfo = this.repalceDictId(investInfo,dictList);
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
                        info = this.repalceDictId(info,dictList);
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
                info = this.repalceDictId(info,dictList);
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

    private void checkArea(InvestmentDTO dto) {
        //校验国家/省份/市是否合法
        //国家
            if (StringUtils.isBlank(dto.getRegisterCountry())) {
                throw new RuntimeException("在有此类情况下，注册地国家信息不能为空");
            }
            GlobalCityInfo country = globalCityInfoService.lambdaQuery().eq(GlobalCityInfo::getName, dto.getRegisterCountry()).last(SqlConstant.ONE_SQL).one();
            if (country == null) {
                throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家不存在");
            }
            //如果是中国下的，校验省份和市
            if (country.getAreaCode().equals(SystemConstant.CHINA_AREA_CODE)) {
                if (StringUtils.isBlank(dto.getRegisterProvince()) || StringUtils.isBlank(dto.getCity())) {
                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份或地级市信息不能为空");
                }
                //省份及城市
                List<GlobalCityInfo> infoList = globalCityInfoService.lambdaQuery().in(GlobalCityInfo::getName, dto.getRegisterProvince(), dto.getCity()).list();

                if (infoList.isEmpty()) {
                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份和地级市不存在");
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
                    throw new RuntimeException(dto.getRegisterCountry() + ":" + "国家下的省份不匹配");
                }
                //
                String provinceId = province.getCountryId(); //省份地区号
                //查询省份下的城市
                List<GlobalCityInfo> cityList = infoList.stream().filter(e -> e.getParentId().equals(provinceId)).collect(Collectors.toList());
                //查询当前城市列表中满足条件的数据
                long i = cityList.stream().filter(e -> e.getName().equals(dto.getCity())).count();
                //GlobalCityInfo city = infoMap.get(info.getCountryId());//地级市
                if (i == 0) {
                    throw new RuntimeException(dto.getRegisterProvince() + ":" + "省份下的地级市不匹配");
                }
            }


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

    private InvestInfo repalceDictId(InvestInfo dto,List<SysDictBiz> dictList){
        //字典对应项
        String isSituation = sysDictBizService.getDictId(dto.getIsSituation(),dictList);
        String title = sysDictBizService.getDictId(dto.getTitle(),dictList);
        String enterpriseState = sysDictBizService.getDictId(dto.getEnterpriseState(),dictList);
        String enterpriseType = sysDictBizService.getDictId(dto.getEnterpriseType(),dictList);
        String shareholder = sysDictBizService.getDictId(dto.getShareholder(),dictList);
        String seniorPosition = sysDictBizService.getDictId(dto.getSeniorPosition(),dictList);
        String isRelation = sysDictBizService.getDictId(dto.getIsRelation(),dictList);

        dto.setIsSituation(isSituation);
        dto.setTitle(title);
        dto.setEnterpriseState(enterpriseState);
        dto.setEnterpriseType(enterpriseType);
        dto.setShareholder(shareholder);
        dto.setSeniorPosition(seniorPosition);
        dto.setIsRelation(isRelation);
        return dto;
    }
}