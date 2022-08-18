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
        if(StrUtil.isEmpty(cardId)||StrUtil.isEmpty(name)||StrUtil.isEmpty(title)){
            return;
        }
        long i = spouseBasicInfoService.selectCount(cardId, name, title);
        if (i > 0) { //i>0 说明当前数据重复了
            return;
        }
        SpouseBasicInfo temp = new SpouseBasicInfo();
        temp.setCreateTime(DateUtil.date());
        temp.setUpdateTime(DateUtil.date());
        temp.setCadreName(info.getGbName());
        temp.setCadreCardId(cardId);
        temp.setName(name);
        temp.setTitle(title);
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
        this.baseMapper.updateTips(tempList);
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
            }catch (Exception e){
                e.printStackTrace();
                //this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("干部组织信息查询失败");
            }
            if(CollectionUtil.isEmpty(gbOrgList)){
                //this.updateState(ids, SystemConstant.SAVE_STATE);
                return ResultMsgUtil.failure("没有找到干部组织信息");
            }
            /*if(CollectionUtil.isNotEmpty(tempList)){
                infoList = infoList.stream().filter(e->tempList.contains(e.getId())).collect(Collectors.toList());
                ids = infoList.stream().map(PrivateEquity::getCardId).collect(Collectors.toList());
            }*/
            //向禁止交易信息表中添加数据 并进行验证 及其他逻辑处理
            banDealInfoService.deleteBanDealInfoByRefId(ids);
            ResultMsgUtil<Map<String, Object>> mapResult = banDealInfoService.insertBanDealInfoOfPrivateEquity(infoList, gbOrgList);
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
                    sj.add(",其中"+(beferIndex-afterIndex)+"数据提交失败");
                }
            }
            resutStr = sj.toString();
        }
        return ResultMsgUtil.success(resutStr);
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
        equity.setCreateName(dto.getServiceUserName());
        equity.setOrgCode(dto.getOrgCode());
        equity.setOrgName(dto.getOrgName());
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

    private List<EquityFundsDTO> checkParams(List<EquityFundsDTO> list, ExportReturnVO exportReturnVO) {
        List<String> isOrNotList = Arrays.asList(SystemConstant.WHETHER_YES, SystemConstant.WHETHER_NO);
        return list.stream().filter(e->{
            if (SystemConstant.IS_SITUATION_YES.equals(e.getIsSituation())){
//                if (StringUtils.isBlank(e.getName())||StringUtils.isBlank(e.getTitle())||StringUtils.isBlank(e.getPrivateequityName())||StringUtils.isBlank(e.getCode())||StringUtils.isBlank(e.getMoney())||StringUtils.isBlank(e.getPersonalMoney())||StringUtils.isBlank(e.getContractTime()
//                )||StringUtils.isBlank(e.getContractExpireTime())||StringUtils.isBlank(e.getInvestDirection())||StringUtils.isBlank(e.getManager())||StringUtils.isBlank(e.getRegistrationNumber())||StringUtils.isBlank(e.getShareholder())||StringUtils.isBlank(e.getController())||StringUtils.isBlank(e.getPractice())||StringUtils.isBlank(e.getIsRelation())){
//                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容不能为空：姓名,称谓,投资私募股权投资基金的情况,投资的私募股权投资基金产品名称,投资的私募股权投资基金产品编码,基金总实缴金额,个人实缴金额,基金合同签署日,基金合同约定的到期日,基金投向,投资私募股权投资基金管理人或者在其担任高级职务的情况,私募股权投资基金管理人名称,私募股权投资基金登记编号,是否为该基金管理人的股东（合伙人）,是否为该基金管理人的实际控制人,是否担任该基金管理人高级职务,是否与报告人所在单位（系统）直接发生过经济关系。"));
//                    return false;
//                }
                if (!isOrNotList.contains(e.getShareholder())||!isOrNotList.contains(e.getController())||!isOrNotList.contains(e.getPractice())||!isOrNotList.contains(e.getIsRelation())){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"有此类情况时以下内容只能填是否：是否为该基金管理人的实际控制人,是否为机构股东（合伙人、所有人等）,是否在该机构中从业,该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系。"));
                    return false;
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
                List<String> numbers = Stream.of(e.getMoney(), e.getPersonalMoney(), e.getSubscriptionMoney(),e.getSubscriptionRatio(), e.getYear()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (!NumberUtils.isAllNumeric(numbers)){
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(e.getColumnNumber(),"以下内容必须为数：基金总实缴金额,个人实缴金额,认缴金额,认缴比例,年度。"));
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveBatchInvestmentInfo(List<EquityFundsDTO> lists, BaseDTO baseDTO, ExportReturnVO exportReturnVO) {
        //过滤掉必填校验未通过的字段
        List<EquityFundsDTO> list = lists.stream().filter(e -> StringUtils.isBlank(e.getMessage())).collect(Collectors.toList());
        list=checkParams(list,exportReturnVO);
        if (CollectionUtils.isEmpty(list)){
            return;
        }
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
                        String checkDict = this.replaceDictId(investInfo, dictList);
                        if (StringUtils.isBlank(checkDict)){
                            investInfo.setCreateName(baseDTO.getServiceUserName());
                            investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                            privateEquity.add(investInfo);
                            exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                        }else {
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                        }

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
                        investInfo.setCreateName(baseDTO.getServiceUserName());
                        investInfo.setCreateAccount(baseDTO.getServiceUserAccount());
                        privateEquity.add(investInfo);
                        exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                    }else {
                        exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                        exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                    }
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
                        String checkDict = this.replaceDictId(info,dictList);
                        if (StringUtils.isBlank(checkDict)){
                            if(nameIndex==0|titleIndex==0|codeIndex==0){ //一个都不重复
                                //如果不相同，新增，否则就是覆盖
                                info.setCreateTime(DateUtil.date());
                                info.setCreateName(baseDTO.getServiceUserName());
                                info.setCreateAccount(baseDTO.getServiceUserAccount());
                                privateEquity.add(info);
                                exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                            }else{ //有重复数据了
                                PrivateEquity existInfo = infos.stream().filter(e->t.getName().equals(e.getName())
                                        &&sysDictBizService.getDictId(t.getTitle(),dictList).equals(e.getTitle())
                                        &&t.getCode().equals(e.getCode())).findAny().orElse(null);
                                String title = info.getTitle();
                                if(Objects.nonNull(existInfo)){
                                    info.setId(existInfo.getId());
                                    updateList.add(info);
                                    exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                }else if (privateEquity.isEmpty()||privateEquity.stream().filter(privateEquity1 -> t.getName().equals(privateEquity1.getName())&&t.getCode().equals(privateEquity1.getCode())&&title.equals(privateEquity1.getTitle())).count()==0){
                                    info.setCreateTime(DateUtil.date());
                                    info.setCreateName(baseDTO.getServiceUserName());
                                    info.setCreateAccount(baseDTO.getServiceUserAccount());
                                    privateEquity.add(info);
                                    exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                                }else {
                                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),"数据重复"));
                                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                                }
                            }
                        }else {
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
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
                        String checkDict = this.replaceDictId(info,dictList);
                        if (StringUtils.isBlank(checkDict)){
                            info.setCreateName(baseDTO.getServiceUserName());
                            info.setCreateAccount(baseDTO.getServiceUserAccount());
                            privateEquity.add(info);
                            exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                        }else {
                            exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
                        }

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
                        info.setCreateTime(new Date());
                        info.setCreateName(baseDTO.getServiceUserName());
                        info.setCreateAccount(baseDTO.getServiceUserAccount());
                        privateEquity.add(info);//可添加到数据库中
                    } else {
                        info.setId(infos.get(0).getId());
                        updateList.add(info);
                    }
                    exportReturnVO.setSuccessNumber(exportReturnVO.getSuccessNumber()+1);
                }else {
                    exportReturnVO.getFailMessage().add(new ExportReturnMessageVO(t.getColumnNumber(),checkDict));
                    exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
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

        System.out.println("123--"+t.getTitle());

        String isSituation=null;
        String title=null;
        String controller=null;
        String shareholder=null;
        String practice=null;
        String isRelation=null;
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
        if (StringUtils.isNotBlank(t.getController())){
            controller =sysDictBizService.getDictId(t.getController(),dictList);
            if (StringUtils.isBlank(controller)){
                return "是否为该基金管理人的实际控制人字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(t.getShareholder())){
            shareholder =sysDictBizService.getDictId(t.getShareholder(),dictList);
            if (StringUtils.isBlank(shareholder)){
                return "是否为该基金管理人的股东（合伙人）字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(t.getPractice())){
            practice =sysDictBizService.getDictId(t.getPractice(),dictList);
            if (StringUtils.isBlank(practice)){
                return "是否担任该基金管理人高级职字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(t.getIsRelation())){
            isRelation =sysDictBizService.getDictId(t.getIsRelation(),dictList);
            if (StringUtils.isBlank(isRelation)){
                return "是否与报告人所在单位（系统）直接发生过经济关系字典项不存在";
            }
        }
        if (StringUtils.isNotBlank(t.getIsRelation())){
            isRelation =sysDictBizService.getDictId(t.getIsRelation(),dictList);
            if (StringUtils.isBlank(isRelation)){
                return "是否与报告人所在单位（系统）直接发生过经济关系字典项不存在";
            }
        }
        System.out.println("--"+title);
        t.setIsSituation(isSituation);
        t.setTitle(title);
        t.setController(controller);
        t.setShareholder(shareholder);
        t.setPractice(practice);
        t.setIsRelation(isRelation);
        return null;
    }
}
