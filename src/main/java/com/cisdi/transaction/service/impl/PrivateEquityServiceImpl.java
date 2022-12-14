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
import com.cisdi.transaction.mapper.master.PrivateEquityMapper;
import com.cisdi.transaction.service.*;
import com.cisdi.transaction.util.ThreadLocalUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.xml.transform.Result;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Autowired
    private OrgService orgService;

    @Autowired
    private SpouseEnterpriseService spouseEnterpriseService;

    @Override
    public boolean updateState(List<String> ids, String state) {
        UpdateWrapper<PrivateEquity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(PrivateEquity::getState,state).in(PrivateEquity::getId,ids);

        boolean b = this.update(updateWrapper);
        return b;
    }

    @Override
    public int countByNameAndCardIdAndCode(String name, String cardId, String code) {
        Integer count = this.lambdaQuery().eq(PrivateEquity::getName, name).eq(PrivateEquity::getCardId, cardId).eq(PrivateEquity::getCode, code).count();
        return Objects.isNull(count) ? 0 : count.intValue();
    }

    @Override
    public void addFamilyInfo(PrivateEquity info) {
        List<SpouseBasicInfo> sbiList = new ArrayList<>();
        String cardId = info.getCardId();
        String name = info.getName();
        String title = info.getTitle();
        String infoId = info.getId();
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
                spouseEnterpriseService.insertSpouseEnterprise(sid, infoId, "3");
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
            } catch (Exception e) {
                e.printStackTrace();
                // this.updateState(ids, SystemConstant.SAVE_STATE)
            }
        }
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void overrideInvestInfo(String id, PrivateEquityDTO dto) {
        PrivateEquity info = new PrivateEquity();
        BeanUtil.copyProperties(dto,info);
        info.setId(id);
        //校验国家/省份/市
        //checkArea(dto, info);
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
    public ResultMsgUtil<Object> submitPrivateEquity(SubmitDto subDto) {
        List<String> submitFailIdList = null;
        String resutStr = "提交成功";
        List<String> ids = subDto.getIds();
        List<PrivateEquity> infoList = this.lambdaQuery().in(PrivateEquity::getId, ids).list();
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


       // boolean b = this.updateState(ids, SystemConstant.VALID_STATE);
        boolean b = true;
        if (b) { //配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况 表数据改为有效状态 并且修改成功 往 配偶，子女及其配偶表中添加数据。
            //获取干部的基本信息
            List<String> cardIds = infoList.stream().map(PrivateEquity::getCardId).collect(Collectors.toList());
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
                log.error("投资私募股权查询干部组织信息异常",e);
                //this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("干部组织信息查询失败");
            }
            if(CollectionUtil.isEmpty(gbOrgList)){
                //this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("没有找到干部组织信息",ids);
            }
            /*if(CollectionUtil.isNotEmpty(tempList)){
                infoList = infoList.stream().filter(e->tempList.contains(e.getId())).collect(Collectors.toList());
                ids = infoList.stream().map(PrivateEquity::getCardId).collect(Collectors.toList());
            }*/
            //向禁止交易信息表中添加数据 并进行验证 及其他逻辑处理
            banDealInfoService.deleteOnlyBanDealInfoByRefId(ids);
            ResultMsgUtil<Map<String, Object>> mapResult = banDealInfoService.insertBanDealInfoOfPrivateEquity(infoList, gbOrgList);
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
                    /*int beferIndex = infoList.size();
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
    private PrivateEquity  valid(PrivateEquity info){
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        if("无此类情况".equals(sysDictBizService.getDictValue(info.getIsSituation(),dictList))){
            info.setName(null);
            info.setTitle(null);
            info.setCode(null);
            info.setPrivateequityName(null);
            info.setMoney(null);
            info.setPersonalMoney(null);
            info.setInvestDirection(null);
            info.setContractTime(null);
            info.setContractExpireTime(null);
            info.setManager(null);
            info.setRegistrationNumber(null);
            info.setController(null);
            info.setShareholder(null);
            info.setSubscriptionMoney(null);
            info.setSubscriptionRatio(null);
            info.setSubscriptionTime(null);
            info.setPractice(null);
            info.setPostName(null);
            info.setInductionStartTime(null);
            info.setInductionEndTime(null);
            info.setManagerOperatScope(null);
            info.setIsRelation(null);
            info.setRemarks(null);
            info.setTbType(null);
            info.setYear(null);
        }else{
            //是否投资私募股权投资基金
            if("否".equals(sysDictBizService.getDictValue(info.getIsInvest(),dictList))){
                info.setPrivateequityName(null);
                info.setCode(null);
                info.setMoney(null);
                info.setPersonalMoney(null);
                info.setContractTime(null);
                info.setContractExpireTime(null);
                info.setInvestDirection(null);
            }
            //是否为股东（合伙人、所有人）
            if("否".equals(sysDictBizService.getDictValue(info.getShareholder(),dictList))){
                info.setSubscriptionMoney(null);
                info.setSubscriptionRatio(null);
                info.setSubscriptionTime(null);
            }
            //是否为该基金管理人的实际控制人

            //是否担任该基金管理人高级职务
            if("否".equals(sysDictBizService.getDictValue(info.getPractice(),dictList))){
                info.setPostName(null);
                info.setInductionStartTime(null);
                info.setInductionEndTime(null);
            }
            //是否与报告人所在单位（系统）直接发生过经济关系
            /*if("否".equals(sysDictBizService.getDictValue(info.getIsRelation(),dictList))){
                info.setRemarks(null);
            }*/
        }
        return info;
    }
    @Override
    public void savePrivateEquity(PrivateEquityDTO dto) {
        PrivateEquity equity = new PrivateEquity();
        BeanUtil.copyProperties(dto,equity,new String[]{"id"});
        equity.setState(SystemConstant.SAVE_STATE);
        equity.setCreateTime(DateUtil.date());
        equity.setUpdateTime(DateUtil.date());
        equity.setTenantId(dto.getServiceLesseeId());
        equity.setCreatorId(dto.getServiceUserId());
        equity.setCreateAccount(dto.getServiceUserAccount());
        equity.setCreateName(dto.getServicePersonName());
        String orgCode = dto.getOrgCode();
        if(StrUtil.isNotEmpty(orgCode)&&orgCode.split(",").length>1){
            equity.setOrgCode("70000003");
            equity.setOrgName("五矿有色金属股份有限公司");
        }else{
            equity.setOrgCode(orgCode);
            equity.setOrgName(dto.getServiceLesseeName());
        }
        equity = this.valid(equity);
        //新增
        this.save(equity);
        addFamilyInfo(equity);
    }

    @Override
    public void editPrivateEquity(PrivateEquityDTO dto) {
        PrivateEquity equity = new PrivateEquity();
        BeanUtil.copyProperties(dto, equity);
        equity.setState(SystemConstant.SAVE_STATE);
        equity.setUpdateTime(DateUtil.date());
        equity.setUpdaterId(dto.getServiceUserId());
        equity = this.valid(equity);
        this.updateById(equity);
        addFamilyInfo(equity);
        this.editRefSpouseBasic(equity);
    }
    private  void editRefSpouseBasic(PrivateEquity info){
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
                        this.replaceDictId(investInfo,dictList);
                        privateEquity.add(investInfo);
                    }
                } else {
                    PrivateEquity investInfo = new PrivateEquity();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
                    //字典替换
                    this.replaceDictId(investInfo,dictList);
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
                        this.replaceDictId(info,dictList);
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
                        this.replaceDictId(info,dictList);
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
                this.replaceDictId(info,dictList);
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

    private void checkParams(List<EquityFundsDTO> list, ExportReturnVO exportReturnVO,String orgCode) {
        List<String> isOrNotList = Arrays.asList(SystemConstant.WHETHER_YES, SystemConstant.WHETHER_NO);
        List<String> cardIds = Optional.ofNullable(gbBasicInfoService.selectNoAuthCardIds(orgCode)).orElse(Lists.newArrayList());
        Map<Integer, String> columnMessageMap = exportReturnVO.getFailMessage().stream().collect(Collectors.toMap(ExportReturnMessageVO::getColumn, ExportReturnMessageVO::getMessage));
        list.forEach(e->{
            boolean haveColumn = columnMessageMap.containsKey(e.getColumnNumber());
            StringBuilder message=new StringBuilder(haveColumn?columnMessageMap.get(e.getColumnNumber()):"");
            if (SystemConstant.IS_SITUATION_YES.equals(e.getIsSituation())){
//                if (StringUtils.isBlank(e.getName())||StringUtils.isBlank(e.getTitle())||StringUtils.isBlank(e.getPrivateequityName())||StringUtils.isBlank(e.getCode())||StringUtils.isBlank(e.getMoney())||StringUtils.isBlank(e.getPersonalMoney())||StringUtils.isBlank(e.getContractTime()
//                )||StringUtils.isBlank(e.getContractExpireTime())||StringUtils.isBlank(e.getInvestDirection())||StringUtils.isBlank(e.getManager())||StringUtils.isBlank(e.getRegistrationNumber())||StringUtils.isBlank(e.getShareholder())||StringUtils.isBlank(e.getController())||StringUtils.isBlank(e.getPractice())||StringUtils.isBlank(e.getIsRelation())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容不能为空：姓名,称谓,投资私募股权投资基金的情况,投资的私募股权投资基金产品名称,投资的私募股权投资基金产品编码,基金总实缴金额,个人实缴金额,基金合同签署日,基金合同约定的到期日,基金投向,投资私募股权投资基金管理人或者在其担任高级职务的情况,私募股权投资基金管理人名称,私募股权投资基金登记编号,是否为该基金管理人的股东（合伙人）,是否为该基金管理人的实际控制人,是否担任该基金管理人高级职务,是否与报告人所在单位（系统）直接发生过经济关系。"));
//                    return false;
//                }
                if ((!message.toString().contains("是否投资私募股权投资基金")&&!isOrNotList.contains(e.getIsInvest())) || (!message.toString().contains("是否为该基金管理人的股东（合伙人）")&&!isOrNotList.contains(e.getShareholder())) || (!message.toString().contains("是否为该基金管理人的实际控制人")&&!isOrNotList.contains(e.getController())) || (!message.toString().contains("是否担任该基金管理人高级职务")&&!isOrNotList.contains(e.getPractice())) || (!message.toString().contains("是否与报告人所在单位（系统）直接发生过经济关系")&&!isOrNotList.contains(e.getIsRelation()))) {
//                        exportReturnVO.setFailNumber(failNumber);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "有此类情况时以下内容只能填是否：是否投资私募股权投资基金,是否为该基金管理人的实际控制人,是否为机构股东（合伙人、所有人等）,是否在该机构中从业,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系。"));
                    message.append("有此类情况时以下内容只能填是否：是否投资私募股权投资基金,是否为该基金管理人的实际控制人,是否为机构股东（合伙人、所有人等）,是否在该机构中从业,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系;");
//                        return false;
                }
//                if (SystemConstant.WHETHER_YES.equals(e.getShareholder())&&(StringUtils.isBlank(e.getSubscriptionMoney()))||StringUtils.isBlank(e.getSubscriptionRatio())||StringUtils.isBlank(e.getSubscriptionTime())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"为机构股东（合伙人、所有人等）时以下内容不能为空： 认缴金额,认缴比例,认缴时间。"));
//                    return false;
//                }
//                if (SystemConstant.WHETHER_YES.equals(e.getPractice())&&(StringUtils.isBlank(e.getPostName())||StringUtils.isBlank(e.getInductionStartTime())||StringUtils.isBlank(e.getInductionEndTime())||StringUtils.isBlank(e.getManagerOperatScope()))){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"担任该基金管理人高级职务时以下内容不能为空： 所担任的高级职务名称,担任高级职务的开始结束时间,基金管理人的经营范围。"));
//                    return false;
//                }
//                if (SystemConstant.WHETHER_YES.equals(e.getIsRelation())&&StringUtils.isBlank(e.getRemarks())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"与报告人所在单位（系统）直接发生过经济关系时以下内容不能为空：备注。"));
//                    return false;
//                }
                if (SystemConstant.WHETHER_YES.equals(e.getIsInvest()) && (StringUtils.isBlank(e.getPrivateequityName()) || StringUtils.isBlank(e.getCode())
                        || StringUtils.isBlank(e.getMoney()) || StringUtils.isBlank(e.getPersonalMoney()) || StringUtils.isBlank(e.getInvestDirection())
                        || StringUtils.isBlank(e.getContractTime()) || StringUtils.isBlank(e.getContractExpireTime())
                )) {
//                        exportReturnVO.setFailNumber(failNumber);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "投资私募股权投资基金为是时以下内容不能为空：投资的私募股权投资基金产品名称,编码,基金总实缴金额,个人实缴金额,基金投向,基金合同签署日,基金合同约定的到期日。"));
//                        return false;
                    message.append("投资私募股权投资基金为是时以下内容不能为空：投资的私募股权投资基金产品名称,编码,基金总实缴金额,个人实缴金额,基金投向,基金合同签署日,基金合同约定的到期日;");
                }

                List<String> numbers = Stream.of(e.getMoney(), e.getPersonalMoney(), e.getSubscriptionMoney(), e.getSubscriptionRatio(), e.getYear()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (!NumberUtils.isAllNumeric(numbers)) {
//                        exportReturnVO.setFailNumber(failNumber);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "以下内容必须为正数：基金总实缴金额,个人实缴金额,认缴金额,认缴比例,年度。"));
//                        return false;
                    message.append("以下内容必须为正数：基金总实缴金额,个人实缴金额,认缴金额,认缴比例,年度;");
                } else {
                    String year = e.getYear();
                    if (StringUtils.isNotBlank(year) && year.contains(".")) {
//                            exportReturnVO.setFailNumber(failNumber);
//                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "年份不能为小数。"));
//                            return false;
                        message.append("年份不能为小数;");
                    }
                    if (StringUtils.isNotBlank(year)&&!year.contains(".") && Integer.parseInt(year) > Calendar.getInstance().get(Calendar.YEAR)) {
//                            exportReturnVO.setFailNumber(failNumber);
//                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "年份不能大于当前年份。"));
//                            return false;
                        message.append("年份不能大于当前年份;");
                    }
                }
                if (StringUtils.isNotBlank(e.getContractTime())&&!message.toString().contains("基金合同签署日") && CalendarUtil.greaterThanNow(e.getContractTime())) {
//                        exportReturnVO.setFailNumber(failNumber);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "基金合同签署日不能大于当前日期。"));
//                        return false;
                    message.append("基金合同签署日不能大于当前日期;");
                }
                if (StringUtils.isNotBlank(e.getSubscriptionTime())&&!message.toString().contains("认缴时间") && CalendarUtil.greaterThanNow(e.getSubscriptionTime())) {
//                        exportReturnVO.setFailNumber(failNumber);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "认缴时间不能大于当前日期。"));
//                        return false;
                    message.append("认缴时间不能大于当前日期;");
                }
                if (StringUtils.isNotBlank(e.getInductionStartTime())) {
                    if (!message.toString().contains("担任高级职务的开始时间")&&CalendarUtil.greaterThanNow(e.getInductionStartTime())) {
//                            exportReturnVO.setFailNumber(failNumber);
//                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "担任高级职务的开始时间不能大于当前日期。"));
//                            return false;
                        message.append("担任高级职务的开始时间不能大于当前日期;");
                    }
                    if (StringUtils.isNotBlank(e.getInductionEndTime())&& !message.toString().contains("担任高级职务的开始时间")&&!message.toString().contains("担任高级职务的结束时间") && CalendarUtil.compare(e.getInductionStartTime(), e.getInductionEndTime())) {
//                            exportReturnVO.setFailNumber(failNumber);
//                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "担任高级职务的结束时间不能大于开始时间。"));
//                            return false;
                        message.append("担任高级职务的结束时间不能大于开始时间;");
                    }
                } else if (StringUtils.isNotBlank(e.getInductionEndTime())&& !message.toString().contains("担任高级职务的结束时间") && CalendarUtil.greaterThanNow(e.getInductionEndTime())) {
//                        exportReturnVO.setFailNumber(failNumber);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "担任高级职务的结束时间不能大于当前时间。"));
//                        return false;
                    message.append("担任高级职务的结束时间不能大于当前时间;");
                }
                if (cardIds.contains(e.getCardId())) {
//                        exportReturnVO.setFailNumber(failNumber);
//                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(), "没有当前干部权限:" + e.getCardId() + "。"));
//                        return false;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchInvestmentInfo(List<EquityFundsDTO> list, BaseDTO baseDTO, ExportReturnVO exportReturnVO) {
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
        checkParams(list,exportReturnVO,baseDTO.getOrgCode());
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        //获取所有字典信息
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        //新增的内容
        List<PrivateEquity> privateEquity = new ArrayList<>();
        //筛选出没有错误内容的干部身份证号
        List<String> cardIds = list.stream().distinct().filter(e->StringUtils.isBlank(e.getMessage())).map(EquityFundsDTO::getCardId).collect(Collectors.toList());
        //通过干部身份证号查询已经存在的配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况
        List<PrivateEquity> infoList = CollectionUtils.isEmpty(cardIds)?Lists.newArrayList():this.lambdaQuery().in(PrivateEquity::getCardId, cardIds).list();
        //去重集合：通过干部身份证号、家人姓名、称谓、登记编号
        Set<String> uniqueSet=new HashSet<>();
        Date date=new Date();
        if (infoList.isEmpty()) {
            list.forEach(t -> {
                String title = t.getTitle();
                String uniqueCheckMessage = "数据重复:干部身份证号" + t.getCardId() + ",家人姓名" + t.getName() + ",称谓" + title + ",登记编号" + t.getRegistrationNumber() + ";";
                //失败信息
                String failMessage=null;
                //是否需要添加失败信息
                boolean isAddFailMessage=false;
                //失败信息
                String message = t.getMessage();
                if (StringUtils.isBlank(message)){
                    String uniqueCode = t.getCardId() + "," + t.getName() + "," + t.getTitle()+","+t.getRegistrationNumber();
                    if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                        PrivateEquity investInfo = new PrivateEquity();
                        BeanUtils.copyProperties(t, investInfo);
                        investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(new Date())
                                .setUpdateTime(new Date());
                        //字典替换
                        String checkDict = this.replaceDictId(investInfo, dictList);
                        if (StringUtils.isBlank(checkDict)){
                            if (uniqueSet.contains(uniqueCode)){
                                isAddFailMessage=true;
                                failMessage=uniqueCheckMessage;
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
                                privateEquity.add(investInfo);
                                exportReturnVO.addSuccessNumber();
                            }

                        }else {
                            isAddFailMessage=true;
                            failMessage=uniqueSet.contains(uniqueCode)?(checkDict+uniqueCheckMessage):checkDict;
//                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),uniqueSet.contains(uniqueCode)?(checkDict+"数据重复:干部身份证号"+t.getCardId()+",家人姓名"+t.getName()+",称谓"+title+",登记编号"+t.getRegistrationNumber()+";"):checkDict));
//                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                        }
                    } else {
                        PrivateEquity investInfo = new PrivateEquity();
                        BeanUtils.copyProperties(t, investInfo);
                        investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                .setCreateTime(new Date())
                                .setUpdateTime(new Date());
                        //字典替换
                        String checkDict = this.replaceDictId(investInfo,dictList);
                        if (StringUtils.isBlank(checkDict)){
                            if (uniqueSet.contains(uniqueCode)){
                                isAddFailMessage=true;
                                failMessage=uniqueCheckMessage;
                            }else {
                                uniqueSet.add(uniqueCode);
                                investInfo.setCreateName(baseDTO.getServicePersonName());
                                investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                                investInfo.setOrgCode(baseDTO.getOrgCode());
                                investInfo.setOrgName(baseDTO.getOrgName());
                                long time = date.getTime() + 1000;
                                investInfo.setUpdateTime(DateUtil.date(time));
                                investInfo.setCreateTime(DateUtil.date(time));
                                date.setTime(time);
                                privateEquity.add(investInfo);
                                exportReturnVO.addSuccessNumber();
                            }

                        }else {
                            isAddFailMessage=true;
                            failMessage=uniqueSet.contains(uniqueCode)?(checkDict+uniqueCheckMessage):checkDict;
                        }
                    }
                }else {
                    PrivateEquity investInfo = new PrivateEquity();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
//                    String title = investInfo.getTitle();
                    //字典替换
                    String checkDict = this.replaceDictId(investInfo,dictList);
                    if (StringUtils.isNotBlank(checkDict)){
                        exportReturnVO.getFailMessage().stream().filter(exportReturnMessageVO -> exportReturnMessageVO.getColumn().equals(t.getColumnNumber())).forEach(exportReturnMessageVO -> exportReturnMessageVO.setMessage(message+Optional.ofNullable(checkDict).orElse("")));
                    }
                }
                if (isAddFailMessage){
                    exportReturnVO.addFailContent(t.getColumnNumber(),failMessage);
                }
            });
//            if (!privateEquity.isEmpty()) {
//                privateEquity.forEach(mechanismInfo -> {
//                    mechanismInfo.setOrgCode(orgCode);
//                    mechanismInfo.setOrgName(orgName);
//                });
//                this.saveBatch(privateEquity);
//                spouseBasicInfoService.addBatchSpouse(privateEquity.stream().map(investInfo ->
//                        new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(investInfo.getCreateTime()).setTenantId(baseDTO.getServiceLesseeId())
//                                .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(investInfo.getUpdateTime())
//                                .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
//                ).collect(Collectors.toList()),SystemConstant.EQUITYFUNDS);
//            }
            this.saveData(privateEquity,orgCode,orgName,baseDTO);
            return;
        }
        List<PrivateEquity> updateList = new ArrayList<>();
        Map<String, List<PrivateEquity>> infoMap = infoList.stream().collect(Collectors.groupingBy(PrivateEquity::getCardId));
        list.forEach(t -> {
            //List<PrivateEquity> infos = infoMap.get(t.getCardId());
            String uniqueCode = t.getCardId() + "," + t.getName() + "," + t.getTitle()+","+t.getRegistrationNumber();
            List<PrivateEquity> infos = infoMap.getOrDefault(t.getCardId(), null);
            String title1 = t.getTitle();
            String uniqueCheckMessage = "数据重复:干部身份证号" + t.getCardId() + ",家人姓名" + t.getName() + ",称谓" + title1 + ",登记编号" + t.getRegistrationNumber() + ";";
            //失败信息
            String failMessage=null;
            //是否需要添加失败信息
            boolean isAddFailMessage=false;
            if (t.getIsSituation().equals(SystemConstant.IS_SITUATION_YES)) {
                String message = t.getMessage();
                if (StringUtils.isBlank(message)){
                    if (CollectionUtil.isNotEmpty(infos)) {//如果不为空，进行比较
                        //有此类情况
                        //校验姓名和统一社会信用代码不能为空
                        if (StringUtils.isNotBlank(t.getName())&&StringUtils.isNotBlank(t.getTitle())) {
                            //校验国家/省/市
                            PrivateEquity info = new PrivateEquity();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setUpdateTime(DateUtil.date());
                            //判断该干部下的其他子项名称和代码是否相同
                            long nameIndex = infos.stream().filter(e->t.getName().equals(e.getName())).count();
                            long titleIndex = infos.stream().filter(e->sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())).count();
//                        long codeIndex = infos.stream().filter(e->t.getCode().equals(e.getCode())).count();
                            String checkDict = this.replaceDictId(info,dictList);
                            if (StringUtils.isBlank(checkDict)){
                                if (uniqueSet.contains(uniqueCode)){
//                                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(), uniqueCheckMessage));
                                    isAddFailMessage=true;
                                    failMessage=uniqueCheckMessage;
                                }else {
                                    if(nameIndex==0|titleIndex==0){ //一个都不重复
                                        long time = date.getTime() + 1000;
                                        //如果不相同，新增，否则就是覆盖
                                        info.setCreateTime(DateUtil.date(time));
                                        info.setUpdateTime(DateUtil.date(time));
                                        date.setTime(time);
                                        info.setCreateName(baseDTO.getServicePersonName());
                                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                                        info.setOrgCode(baseDTO.getOrgCode());
                                        info.setOrgName(baseDTO.getOrgName());
                                        privateEquity.add(info);
                                        exportReturnVO.addSuccessNumber();
                                        uniqueSet.add(uniqueCode);
                                    }else{ //有重复数据了
                                        PrivateEquity existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
                                                &&sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())
                                        ).findAny().orElse(null);
                                        String title = info.getTitle();
                                        if(Objects.nonNull(existInfo)){
                                            info.setId(existInfo.getId());
                                            long time = date.getTime() + 1000;
                                            info.setUpdateTime(DateUtil.date(time));
                                            info.setCreateTime(DateUtil.date(time));
                                            date.setTime(time);
                                            updateList.add(info);
                                            exportReturnVO.addSuccessNumber();
                                            uniqueSet.add(uniqueCode);
                                        }else if (privateEquity.isEmpty()||privateEquity.stream().filter(privateEquity1 -> t.getName().equals(privateEquity1.getName())&&t.getCode().equals(privateEquity1.getCode())&&title.equals(privateEquity1.getTitle())).count()==0){
                                            long time = date.getTime() + 1000;
                                            info.setCreateTime(DateUtil.date(time));
                                            info.setUpdateTime(DateUtil.date(time));
                                            date.setTime(time);
                                            info.setCreateName(baseDTO.getServicePersonName());
                                            info.setCreateAccount(baseDTO.getServiceUserAccount());
                                            info.setOrgCode(baseDTO.getOrgCode());
                                            info.setOrgName(baseDTO.getOrgName());
                                            privateEquity.add(info);
                                            exportReturnVO.addSuccessNumber();
                                            uniqueSet.add(uniqueCode);
                                        }else {
                                            isAddFailMessage=true;
                                            failMessage="数据重复;";
//                                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复;"));
//                                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                        }
                                    }
                                }

                            }else {
                                isAddFailMessage=true;
                                failMessage=uniqueSet.contains(uniqueCode)?(checkDict+uniqueCheckMessage):checkDict;
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
                        if (StringUtils.isNotBlank(t.getName())) {
                            //数据库为空，直接add
                            PrivateEquity info = new PrivateEquity();
                            BeanUtils.copyProperties(t, info);
                            info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                                    .setCreateTime(new Date())
                                    .setUpdateTime(new Date());
                            //字典替换
                            String checkDict = this.replaceDictId(info,dictList);
                            if (StringUtils.isBlank(checkDict)){
                                if (uniqueSet.contains(uniqueCode)){
                                    isAddFailMessage=true;
                                    failMessage=uniqueCheckMessage;
                                }else {
                                    uniqueSet.add(uniqueCode);
                                    info.setCreateName(baseDTO.getServicePersonName());
                                    info.setCreateAccount(baseDTO.getServiceUserAccount());
                                    long time = date.getTime() + 1000;
                                    info.setCreateTime(DateUtil.date(time));
                                    info.setUpdateTime(DateUtil.date(time));
                                    date.setTime(time);
                                    privateEquity.add(info);
                                    exportReturnVO.addSuccessNumber();
                                }

                            }else {
//                                exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),uniqueSet.contains(uniqueCode)?(checkDict+"数据重复:干部身份证号"+t.getCardId()+",家人姓名"+t.getName()+",称谓"+title1+",登记编号"+t.getRegistrationNumber()+";"):checkDict));
//                                exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                isAddFailMessage=true;
                                failMessage=uniqueSet.contains(uniqueCode)?(checkDict+uniqueCheckMessage):checkDict;
                            }

                        }
                    }
                }else {
                    PrivateEquity investInfo = new PrivateEquity();
                    BeanUtils.copyProperties(t, investInfo);
                    investInfo.setState(SystemConstant.SAVE_STATE)//默认类型新建
                            .setCreateTime(new Date())
                            .setUpdateTime(new Date());
//                    String title = investInfo.getTitle();
                    //字典替换
                    String checkDict = this.replaceDictId(investInfo,dictList);
                    if (StringUtils.isNotBlank(checkDict)){
                        exportReturnVO.getFailMessage().stream().filter(exportReturnMessageVO -> exportReturnMessageVO.getColumn().equals(t.getColumnNumber())).forEach(exportReturnMessageVO -> exportReturnMessageVO.setMessage(message+Optional.ofNullable(checkDict).orElse("")));
                    }
                }

            } else {
                //说明无此类情况
                PrivateEquity info = new PrivateEquity();
                BeanUtils.copyProperties(t, info);
                info.setState(SystemConstant.SAVE_STATE)//默认类型新建
                        .setUpdateTime(new Date());
                //字典替换
                String checkDict = this.replaceDictId(info,dictList);
                if (StringUtils.isBlank(checkDict)){
                    // 数据库中如果不存在数据
                    if (CollectionUtil.isEmpty(infos)) {
                        long time = date.getTime() + 1000;
                        info.setCreateTime(DateUtil.date(time));
                        info.setUpdateTime(DateUtil.date(time));
                        date.setTime(time);
                        info.setCreateName(baseDTO.getServicePersonName());
                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                        privateEquity.add(info);//可添加到数据库中
                    } else {
                        info.setId(infos.get(0).getId());
                        long time = date.getTime() + 1000;
                        info.setCreateTime(DateUtil.date(time));
                        info.setUpdateTime(DateUtil.date(time));
                        date.setTime(time);
                        updateList.add(info);
                    }
                    exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                }else {
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                }

            }
            if (isAddFailMessage){
                exportReturnVO.addFailContent(t.getColumnNumber(),failMessage);
            }
        });
        this.saveData(privateEquity,orgCode,orgName,baseDTO);
        if (!updateList.isEmpty()) {
            updateList.forEach(mechanismInfo -> {
                mechanismInfo.setOrgCode(orgCode);
                mechanismInfo.setOrgName(orgName);
            });
            this.updateBatchById(updateList);
            spouseBasicInfoService.addBatchSpouse(updateList.stream().map(investInfo ->
                    new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(investInfo.getCreateTime()).setTenantId(baseDTO.getServiceLesseeId())
                            .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(investInfo.getUpdateTime())
                            .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
            ).collect(Collectors.toList()),SystemConstant.EQUITYFUNDS);
        }
    }

    private void saveData(List<PrivateEquity> privateEquity,String orgCode,String orgName,BaseDTO baseDTO){
        if (!privateEquity.isEmpty()) {
            privateEquity.forEach(mechanismInfo -> {
                mechanismInfo.setOrgCode(orgCode);
                mechanismInfo.setOrgName(orgName);
            });
            this.saveBatch(privateEquity);
            spouseBasicInfoService.addBatchSpouse(privateEquity.stream().map(investInfo ->
                    new SpouseBasicInfo().setRefId(investInfo.getId()).setCreateTime(investInfo.getCreateTime()).setTenantId(baseDTO.getServiceLesseeId())
                            .setCreatorId(baseDTO.getServiceUserId()).setUpdaterId(baseDTO.getServiceUserId()).setUpdateTime(investInfo.getUpdateTime())
                            .setCadreCardId(investInfo.getCardId()).setName(investInfo.getName()).setTitle(investInfo.getTitle()).setCadreName(investInfo.getGbName())
            ).collect(Collectors.toList()),SystemConstant.EQUITYFUNDS);
        }
    }

    @Override
    public List<EquityFundsDTO> exportEquityFundsExcel(CadreFamilyExportDto exportDto) {

        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<EquityFundsDTO> list =  this.list(new QueryWrapper<PrivateEquity>()
                .orderBy(StringUtils.isNotBlank(exportDto.getColumnName())&&Objects.nonNull(exportDto.getIsAsc()),exportDto.getIsAsc(),exportDto.getColumnName())
                .orderByDesc(StringUtils.isBlank(exportDto.getColumnName())||Objects.isNull(exportDto.getIsAsc()),"create_time")
                .lambda()
                .eq(StringUtils.isNotBlank(exportDto.getState()),PrivateEquity::getState, exportDto.getState())
                .like(StringUtils.isNotBlank(exportDto.getCompany()),PrivateEquity::getCompany,exportDto.getCompany())
                .like(StringUtils.isNotBlank(exportDto.getGb_name()),PrivateEquity::getGbName,exportDto.getGb_name())
                .in(CollectionUtil.isNotEmpty(exportDto.getCreate_account()),PrivateEquity::getCreateAccount,exportDto.getCreate_account())
                .apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.PRIVATE_EQUITY,exportDto.getOrgCode()))
        ).stream().map(t -> {
            EquityFundsDTO dto = new EquityFundsDTO();
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
        list  = this.replaceDictValue(list,dictList);
        return list;
    }

    @Override
    public List<KVVO> getCreateInfoForPrivateEquity(String orgCode) {
        if(StrUtil.isEmpty(orgCode)){
            return new ArrayList<>();
        }
        List<String> orgCodeList = Arrays.stream(orgCode.split(",")).distinct().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if(orgCodeList.size()>1){
            orgCode = "70000003";//系统中只有pizd整个账号会传多个orgCode.他的主组织编码是70000003
        }
        Org org = orgService.selectByOrgancode(orgCode);
        List<PrivateEquity> list = null;
        String asglevel = org.getAsglevel();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")) { //看所有
            QueryWrapper<PrivateEquity> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("DISTINCT  create_account","create_name");
            list = this.baseMapper.selectList(queryWrapper);
        }else{
            String asgpathnamecode = org.getAsgpathnamecode();
            List<String > cardIds = orgService.getCardIdsByAsgpathnamecode(asgpathnamecode);
            if(CollectionUtil.isEmpty(cardIds)){
                return new ArrayList<>();
            }
            cardIds.add("-9999qq");//
            QueryWrapper<PrivateEquity> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("DISTINCT  create_account","create_name").in( "card_id",cardIds);
            list = this.baseMapper.selectList(queryWrapper);
        }
        List<KVVO> resultList = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(list)){
            List<AuthUser> authUsers = gbBasicInfoService.selectAuthUser();
            if(CollectionUtil.isEmpty(authUsers)){
                return new ArrayList<>();
            }
            for (PrivateEquity info : list) {
                if(Objects.isNull(info)){
                    continue;
                }
                String account = info.getCreateAccount();
                String userName= info.getCreateName();
                //String orgName = info.getOrgName();
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
    private String replaceDictId(PrivateEquity t,List<SysDictBiz> dictList){


        String isInvest=null;
        String isSituation=null;
        String title=null;
        String controller=null;
        String shareholder=null;
        String practice=null;
        String isRelation=null;
        //String postType=null;

        StringBuilder message=new StringBuilder();
        if (StringUtils.isNotBlank(t.getIsInvest())){
            isInvest =sysDictBizService.getDictId(t.getIsInvest(),dictList);
            if (StringUtils.isBlank(isInvest)){
                message.append("是否投资私募股权投资基金字典项不存在;") ;
            }
        }
        if (StringUtils.isNotBlank(t.getIsSituation())){
            isSituation =sysDictBizService.getDictId(t.getIsSituation(),dictList);
            if (StringUtils.isBlank(isSituation)){
//                return "有无此类情况字典项不存在";
                message.append("有无此类情况字典项不存在;") ;
            }
        }
        if (StringUtils.isNotBlank(t.getTitle())){
            title =sysDictBizService.getDictId(t.getTitle(),dictList);
            if (StringUtils.isBlank(title)){
//                return "称谓字典项不存在";
                message.append("称谓字典项不存在;") ;
            }
        }
        if (StringUtils.isNotBlank(t.getController())){
            controller =sysDictBizService.getDictId(t.getController(),dictList);
            if (StringUtils.isBlank(controller)){
//                return "是否为该基金管理人的实际控制人字典项不存在";
                message.append("是否为该基金管理人的实际控制人字典项不存在;") ;
            }
        }
        if (StringUtils.isNotBlank(t.getShareholder())){
            shareholder =sysDictBizService.getDictId(t.getShareholder(),dictList);
            if (StringUtils.isBlank(shareholder)){
//                return "是否为该基金管理人的股东（合伙人）字典项不存在";
                message.append("是否为该基金管理人的股东（合伙人）字典项不存在;") ;
            }
        }
        if (StringUtils.isNotBlank(t.getPractice())){
            practice =sysDictBizService.getDictId(t.getPractice(),dictList);
            if (StringUtils.isBlank(practice)){
                message.append("是否担任该基金管理人高级职字典项不存在;") ;
//                return "是否担任该基金管理人高级职字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(t.getIsRelation())){
            isRelation =sysDictBizService.getDictId(t.getIsRelation(),dictList);
            if (StringUtils.isBlank(isRelation)){
//                return "是否与报告人所在单位（系统）直接发生过经济关系字典项不存在";
                message.append("是否与报告人所在单位（系统）直接发生过经济关系字典项不存在;") ;
            }
        }
        if (StringUtils.isNotBlank(t.getIsRelation())){
            isRelation =sysDictBizService.getDictId(t.getIsRelation(),dictList);
            if (StringUtils.isBlank(isRelation)){
//                return "是否与报告人所在单位（系统）直接发生过经济关系字典项不存在";
                message.append("是否与报告人所在单位（系统）直接发生过经济关系字典项不存在;") ;
            }
        }
//        if (StringUtils.isNotBlank(t.getPostType())){
//            postType =sysDictBizService.getDictId(t.getPostType(),dictList);
//            if (StringUtils.isBlank(postType)){
//                return "干部类型字典项不存在";
//            }
//        }

        t.setIsInvest(isInvest);
        t.setIsSituation(isSituation);
        t.setTitle(title);
        t.setController(controller);
        t.setShareholder(shareholder);
        t.setPractice(practice);
        t.setIsRelation(isRelation);
//        t.setPostType(postType);
        return StringUtils.isNotBlank(message)?message.toString():null;
    }
}
