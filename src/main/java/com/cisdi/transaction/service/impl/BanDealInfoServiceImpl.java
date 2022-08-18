package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.BanDealInfoDTO;
import com.cisdi.transaction.domain.dto.SubmitDto;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.KVVO;
import com.cisdi.transaction.domain.vo.ProhibitTransactionExcelVO;
import com.cisdi.transaction.mapper.master.BanDealInfoMapper;
import com.cisdi.transaction.mapper.slave.PurchaseBanDealInfoMapper;
import com.cisdi.transaction.service.*;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 禁止交易信息
 *
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 14:27
 */
@Service
public class BanDealInfoServiceImpl extends ServiceImpl<BanDealInfoMapper, BanDealInfo> implements BanDealInfoService {

    @Autowired
    private BanDealInfoRecordService banDealInfoRecordService;

    @Autowired
    private PurchaseBanDealInfoSevice purchaseBanDealInfoSevice;

    @Autowired
    private SysDictBizService sysDictBizService;

    @Autowired
    private SpouseBasicInfoService spouseBasicInfoService;

    @Autowired
    private OrgService orgService;
    @Value("${wk.url}")
    private String url;

    @Override
    public boolean updateState(List<String> ids, String state) {
        List<BanDealInfo> list = ids.stream().map(e -> new BanDealInfo().setId(e).setState(state)).collect(Collectors.toList());
        boolean b = this.updateBatchById(list);
        return b;
    }

    public JSONObject getCompanyInfoByName(JSONObject jbParam) {
        System.out.println(jbParam.toString());
        String result = HttpUtil.post(url, jbParam.toString());
        JSONObject jb = JSON.parseObject(result);
        return jb;

    }

    @Override
    public void insertBanDealInfo(BanDealInfoDTO infoDto) {
        BanDealInfo info = new BanDealInfo();
        BeanUtil.copyProperties(infoDto, info, new String[]{"id"});
        //info.setState(SystemConstant.SAVE_STATE);
        info.setCreateTime(DateUtil.date());
        info.setUpdateTime(DateUtil.date());

        info.setTenantId(infoDto.getServiceLesseeId());
        info.setCreator(infoDto.getServiceUserName());
        info.setCreatorAccount(infoDto.getServiceUserAccount());
        info.setCreatorId(infoDto.getServiceUserId());
        if(StrUtil.isEmpty(infoDto.getManageCompany())){
            info.setManageCompany(infoDto.getServiceLesseeName());
        }
        if(StrUtil.isEmpty(infoDto.getManageCompanyCode())){
            info.setManageCompanyCode(infoDto.getServiceLesseeId());
        }
        //验证企业社会信用代码
        info = validCompanyCode(info);
        //验证
        info = validSupplierAndCodeAndBanPurchaseCode(info, SystemConstant.SAVE_STATE);
        this.save(info);

        //加入干部家属信息
        this.addFamilyInfo(info);
        //新增操作记录
        List<BanDealInfo> infoList = new ArrayList<>();
        infoList.add(info);
        banDealInfoRecordService.insertBanDealInfoRecord(infoList, SystemConstant.OPERATION_TYPE_ADD); //新增
    }

    @Override
    public void editBanDealInfo(BanDealInfoDTO infoDto) {
        BanDealInfo info = new BanDealInfo();
        BeanUtil.copyProperties(infoDto, info);
        info.setUpdateTime(DateUtil.date());
        info.setUpdaterId(infoDto.getServiceUserId());
        //验证企业社会信用代码
        info = validCompanyCode(info);
        //验证
        info = validSupplierAndCodeAndBanPurchaseCode(info, SystemConstant.SAVE_STATE); //修改的数据状态都改为新建
        //删除推送到采购平台的数据
        List<String> ids = new ArrayList<>();
        ids.add(infoDto.getId());
        this.updateById(info);
         //purchaseBanDealInfoSevice.deletePushDataForPurchase(ids);
        //加入干部家属信息
        this.addFamilyInfo(info);
        //新增操作记录
        List<BanDealInfo> infoList = new ArrayList<>();
        infoList.add(info);
        banDealInfoRecordService.insertBanDealInfoRecord(infoList, SystemConstant.OPERATION_TYPE_EDIT); //编辑
    }

    @Override
    public void addFamilyInfo(BanDealInfo info) {
        List<SpouseBasicInfo> sbiList = new ArrayList<>();
        String cardId = info.getCardId(); //身份证号
        String name = info.getFamilyName(); //家属姓名
        String title = info.getRelation(); //关系
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
        temp.setCadreName(info.getName());
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

    /**
     * 向禁止交易信息表中添加数据
     * 1.社会企业信用代码验证
     * 2.禁止交易采购单位逻辑
     * 3.经商类型详细描述
     * 4.验证供应商名称 信用代码  禁止交易采购单位代码是否都有。没有则置为无效
     *
     * @param infoList 来源与投资企业或担任高级职务情况表中的数据
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMsgUtil<Map<String,Object>>  insertBanDealInfoOfInvestInfo(List<InvestInfo> infoList, List<GbOrgInfo> gbOrgList) {
        //Map<String, List<GbBasicInfo>> gbBasicMap = gbList.parallelStream().collect(Collectors.groupingBy(GbBasicInfo::getCardId));
        Map<String, List<GbOrgInfo>> gbOrgMap = gbOrgList.stream().collect(Collectors.groupingBy(GbOrgInfo::getCardId));
        List<BanDealInfo> banDealInfoList = new ArrayList<>(); //保存禁止交易数据
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<String>  submitId = new ArrayList<>(); //提交成功的数据id
        List<KVVO>  submitFailId = new ArrayList<>(); //无法提交数据的数据id及失败信息
        for (InvestInfo info : infoList) {
            String gbCardId = info.getCardId();//干部的身份证
            List<GbOrgInfo> gbOrgInfoList = !gbOrgMap.containsKey(gbCardId) ? null : ((List<GbOrgInfo>) gbOrgMap.get(gbCardId));//是否在干部表中查询到干部数据
            if(CollectionUtil.isNotEmpty(gbOrgInfoList)){
                //禁止交易表中干部有几条数据 取决干部所在几个组织
                for (GbOrgInfo gbOrgInfo:gbOrgInfoList){

                    BanDealInfo bandealInfo = new BanDealInfo();
                    bandealInfo.setCardId(gbCardId);
                    bandealInfo.setName(gbOrgInfo.getName());
                    bandealInfo.setCompany(gbOrgInfo.getUnit());
                    bandealInfo.setPost(gbOrgInfo.getPost());
                    bandealInfo.setPostType(gbOrgInfo.getPostType());
                    bandealInfo.setBanPostType(gbOrgInfo.getPostType());//禁止职务类型

                    bandealInfo.setCreateTime(DateUtil.date());
                    bandealInfo.setUpdateTime(DateUtil.date());
                    bandealInfo.setFamilyName(info.getName());
                    bandealInfo.setRelation(info.getTitle());
                    bandealInfo.setEngageType(sysDictBizService.getDictId("投资企业或者担任高级职务",dictList));

                    //13-1表中的，显示X（个人认缴出资额或个人出资额（人民币万元））,Y（个人认缴出资比例或个人出资比例（%））,AA（是否担任高级职务），AB（所担任的高级职务名称）列内容
                    //----如果AA值为是，则拼接AB列，否则不拼接
                    String engageInfo = "个人认缴出资额或个人出资额(人民币万元):" +(StrUtil.isEmpty( info.getPersonalCapital())?"": info.getPersonalCapital()) + ",个人认缴出资比例或个人出资比例(%):" + (StrUtil.isEmpty(info.getPersonalRatio())?"":info.getPersonalRatio())
                            + ("是".equals(sysDictBizService.getDictValue(info.getSeniorPosition(),dictList)) ? ",担任高级职务名称:" + (StrUtil.isEmpty(info.getSeniorPositionName())?"":info.getSeniorPositionName()) : "");

                    bandealInfo.setEngageInfo(engageInfo);
                    bandealInfo.setOperatScope(info.getOperatScope());
                    bandealInfo.setSupplier(info.getEnterpriseName());
                    bandealInfo.setCode(info.getCode());
                    //验证必填字段
                    boolean b = this.validRequiredFeild(info.getId(),bandealInfo,submitFailId);
                    if(!b){
                        continue;
                    }
                    String company = gbOrgInfo.getUnit();
                    String department = gbOrgInfo.getDeparment();
                    if ((StrUtil.isEmpty(company)||StrUtil.isEmpty(department)) || !(company.contains("中国五矿集团有限公司")&&department.contains("专职董(监)事办公室"))) { //如果该干部在集团专职董监事,禁止交易采购单位名称手动录入
                        //根据干部禁业职务类型判断，
                        //当禁业职务类型为党组管理干部正职，则禁止交易采购单位为五矿集团及以下；
                        //当禁业职务类型为党组管理干部副职，则禁止交易采购单位为该干部所在单位及以下；
                        //当禁业职务类型为总部处长，则禁止交易采购单位为所在部门；
                        //当禁业职务类型为直管企业党委管理干部正职，则禁止交易采购单位为该干部所在单位及以下；
                        //当禁业职务类型为直管企业党委管理干部副职，则禁止交易采购单位为该干部所在单位及以下；
                        String banPostType = bandealInfo.getBanPostType();
                        String whether = SystemConstant.WHETHER_YES;
                        //String purchaseCode = Objects.isNull(gbOrgInfo) ? "" : gbOrgInfo.getUnitCode();//禁止交易采购单位代码
                        String purchaseCode = Objects.isNull(gbOrgInfo) ? "" : gbOrgInfo.getOrgCode();//禁止交易采购单位代码
                        String purchaseName = Objects.isNull(gbOrgInfo) ? "" : gbOrgInfo.getUnit();
                        ;//禁止交易采购单位名称
                        if ("党组管理干部正职".equals(sysDictBizService.getDictValue(banPostType,dictList))) {
                            purchaseCode = "60000001";
                            purchaseName = "中国五矿集团有限公司";
                        } else if ("总部处长".equals(sysDictBizService.getDictValue(banPostType,dictList))) {
                            whether = SystemConstant.WHETHER_NO;//是否继承关系
                            Org org = orgService.getOrgByUnitCodeAndDepartmentName(gbOrgInfo.getUnitCode(),gbOrgInfo.getDeparment());
                            purchaseCode = Objects.isNull(org) ? "" : org.getAsgorgancode();//禁止交易采购单位代码
                            purchaseName = Objects.isNull(org) ? "" : org.getAsgorganname();
                        }
                        bandealInfo.setIsExtends(whether);//是否继承关系
                        bandealInfo.setBanPurchaseCode(purchaseCode);//禁止交易采购单位代码
                        bandealInfo.setBanPurchaseName(purchaseName);//禁止交易采购单位名称
                    }
                    bandealInfo.setManageCompany(info.getOrgName());//管理单位名称
                    bandealInfo.setManageCompanyCode(info.getOrgCode()); //管理单位代码
                    bandealInfo.setCreator(info.getCreateName()); //创建人
                    bandealInfo.setCreatorAccount(info.getCreateAccount()); //创建人账号
                    bandealInfo.setRefId(info.getId());//关联数据id
                    //验证供应商名称 信用代码  禁止交易采购单位代码是否都有。没有则置为无效
                    bandealInfo = validSupplierAndCodeAndBanPurchaseCode(bandealInfo, SystemConstant.SAVE_STATE); //新建
                    banDealInfoList.add(bandealInfo);
                }
                submitId.add(info.getId());
            }else{
                KVVO vo = new KVVO();
                vo.setId(info.getId());
                vo.setName("未找到干部信息");
                submitFailId.add(vo);
            }
        }
        //社会企业信用代码验证
        List<BanDealInfo> newBanDealInfoList = this.validBatchCompanyCode(banDealInfoList);
        //在禁止企业交易信息表中添加数据
        boolean b = this.saveBatch(newBanDealInfoList);
        //新增操作记录
        boolean deal = banDealInfoRecordService.insertBanDealInfoRecord(newBanDealInfoList, SystemConstant.OPERATION_TYPE_ADD); //新增
        Map<String,Object> map = new HashMap();
        map.put("submitIds",submitId); //记录已提交的数据
        map.put("submitFailId",submitFailId); //记录无法提交的数据
        map.put("banDeal",b); //禁止交易数据是否提交成功
        map.put("banDealRecord",deal); //禁止交易记录数据是否提交成功
        return ResultMsgUtil.success(map);
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMsgUtil<Map<String,Object>> insertBanDealInfoOfPrivateEquity(List<PrivateEquity> infoList, List<GbOrgInfo> gbOrgList) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        Map<String, List<GbOrgInfo>> gbOrgMap = gbOrgList.stream().collect(Collectors.groupingBy(GbOrgInfo::getCardId));
        List<BanDealInfo> banDealInfoList = new ArrayList<>();
        List<String>  submitId = new ArrayList<>(); //提交的数据id
        List<KVVO>  submitFailId = new ArrayList<>(); //无法提交数据的数据id及失败信息
        for (PrivateEquity info : infoList) {
            String gbCardId = info.getCardId();//干部的身份证
            List<GbOrgInfo> gbOrgInfoList = !gbOrgMap.containsKey(gbCardId) ? null : ((List<GbOrgInfo>) gbOrgMap.get(gbCardId));//是否在干部表中查询到干部数据
            if(CollectionUtil.isNotEmpty(gbOrgInfoList)){
                //禁止交易表中干部有几条数据 取决干部所在几个组织
                for (GbOrgInfo gbOrgInfo:gbOrgInfoList){
                    BanDealInfo bandealInfo = new BanDealInfo();
                    bandealInfo.setCardId(gbCardId);
                    bandealInfo.setName(gbOrgInfo.getName());
                    bandealInfo.setCompany(gbOrgInfo.getUnit());
                    bandealInfo.setPost(gbOrgInfo.getPost());
                    bandealInfo.setPostType(gbOrgInfo.getPostType());
                    bandealInfo.setBanPostType(gbOrgInfo.getPostType());//禁止职务类型
                    bandealInfo.setCreateTime(DateUtil.date());
                    bandealInfo.setUpdateTime(DateUtil.date());
                    bandealInfo.setFamilyName(info.getName());
                    bandealInfo.setRelation(info.getTitle());
                    bandealInfo.setEngageType(sysDictBizService.getDictId("投资私募股权投资基金或者担任高级职务",dictList));
                    //13-3表中的，显示M（投资的私募股权投资基金产品名称），O（基金总实缴金额（人民币万元）），P（个人实缴金额（人民币万元））,Q（基金投向），X（认缴金额（人民币万元）），Y（认缴比例（%）），AA（是否担任该基金管理人高级职务），AB（所担任的高级职务名称）列内容
                    //------如果AA值为是，则拼接AB列，否则不拼接
                    String engageInfo = "投资的私募股权投资基金产品名称:" + (StrUtil.isEmpty(info.getPrivateequityName())?"":info.getPrivateequityName()) + ",基金总实缴金额（人民币万元）:" + (StrUtil.isEmpty(info.getMoney())?"":info.getMoney())
                            + ",个人实缴金额（人民币万元）:" +(StrUtil.isEmpty(info.getPersonalMoney())?"":info.getPersonalMoney())  + ",基金投向:" + (StrUtil.isEmpty(info.getInvestDirection())?"":info.getInvestDirection())
                            + ",认缴金额（人民币万元）:" + info.getSubscriptionMoney() + ",认缴比例（%）:" + info.getSubscriptionRatio()
                            + ("是".equals(sysDictBizService.getDictValue(info.getPractice(),dictList)) ? ",所担任的高级职务名称:" + (StrUtil.isEmpty(info.getPostName())?"":info.getPostName()) : "");

                    bandealInfo.setEngageInfo(engageInfo);
                    bandealInfo.setOperatScope(info.getManagerOperatScope());
                    bandealInfo.setSupplier(info.getManager());
                    bandealInfo.setCode(info.getRegistrationNumber());
                    //验证在数据库中必填字段是否有值。无值则不插入
                    boolean b = this.validRequiredFeild(info.getId(),bandealInfo,submitFailId);
                    if(!b){
                        continue;
                    }
                    String company = gbOrgInfo.getUnit();
                    String department = gbOrgInfo.getDeparment();
                    if ((StrUtil.isEmpty(company)||StrUtil.isEmpty(department)) || !(company.contains("中国五矿集团有限公司")&&department.contains("专职董(监)事办公室"))) { //如果该干部在集团专职董监事,禁止交易采购单位名称手动录入
                        String banPostType = bandealInfo.getBanPostType();
                        String whether = sysDictBizService.getDictId(SystemConstant.WHETHER_YES,dictList);
                        String purchaseCode = Objects.isNull(gbOrgInfo) ? "" : gbOrgInfo.getUnitCode();//禁止交易采购单位代码
                        String purchaseName = Objects.isNull(gbOrgInfo) ? "" : gbOrgInfo.getUnit();
                        ;//禁止交易采购单位名称
                        if ("党组管理干部正职".equals(sysDictBizService.getDictValue(banPostType,dictList))) {
                            purchaseCode = "60000001";
                            purchaseName = "中国五矿集团有限公司";
                        } else if ("总部处长".equals(sysDictBizService.getDictValue(banPostType,dictList))) {
                            whether = sysDictBizService.getDictId(SystemConstant.WHETHER_NO,dictList);//是否继承关系
                            Org org = orgService.getOrgByUnitCodeAndDepartmentName(gbOrgInfo.getUnitCode(),gbOrgInfo.getDeparment());
                            purchaseCode = Objects.isNull(org) ? "" : org.getAsgorgancode();//禁止交易采购单位代码
                            purchaseName = Objects.isNull(org) ? "" : org.getAsgorganname();
                        }
                        bandealInfo.setIsExtends(whether);//是否继承关系
                        bandealInfo.setBanPurchaseCode(purchaseCode);//禁止交易采购单位代码
                        bandealInfo.setBanPurchaseName(purchaseName);//禁止交易采购单位名称
                    }
                    bandealInfo.setManageCompany(info.getOrgName());//管理单位名称
                    bandealInfo.setManageCompanyCode(info.getOrgCode()); //管理单位代码
                    bandealInfo.setCreator(info.getCreateName()); //创建人
                    bandealInfo.setCreatorAccount(info.getCreateAccount()); //创建人账号
                    bandealInfo.setRefId(info.getId());//关联数据id
                    //验证供应商名称 信用代码  禁止交易采购单位代码是否都有。没有则置为无效
                    bandealInfo = validSupplierAndCodeAndBanPurchaseCode(bandealInfo, SystemConstant.SAVE_STATE); //新建
                    banDealInfoList.add(bandealInfo);
                }
                submitId.add(info.getId());
            }else{
                KVVO vo = new KVVO();
                vo.setId(info.getId());
                vo.setName("未找到干部信息");
                submitFailId.add(vo);
            }
        }
        //社会企业信用代码验证
        List<BanDealInfo> newBanDealInfoList = this.validBatchCompanyCode(banDealInfoList);
        //在禁止企业交易信息表中添加数据
        boolean b = this.saveBatch(banDealInfoList);
        //新增操作记录
        boolean deal = banDealInfoRecordService.insertBanDealInfoRecord(banDealInfoList, SystemConstant.OPERATION_TYPE_ADD); //新增

        Map<String,Object> map = new HashMap();
        map.put("submitIds",submitId); //记录能提交的数据
        map.put("submitFailId",submitFailId); //记录无法提交的数据
        map.put("banDeal",b); //禁止交易数据是否提交成功
        map.put("banDealRecord",deal); //禁止交易记录数据是否提交成功
        return ResultMsgUtil.success(map);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMsgUtil<Map<String,Object>> insertBanDealInfoOfMechanismInfo(List<MechanismInfo> infoList, List<GbOrgInfo> gbOrgList) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<String>  submitId = new ArrayList<>(); //提交失败的数据id
        List<KVVO>  submitFailId = new ArrayList<>(); //无法提交数据的数据id及失败信息
        Map<String, List<GbOrgInfo>> gbOrgMap = gbOrgList.stream().collect(Collectors.groupingBy(GbOrgInfo::getCardId));
        List<BanDealInfo> banDealInfoList = new ArrayList<>();
        for (MechanismInfo info : infoList) {
            String gbCardId = info.getCardId();//干部的身份证
            List<GbOrgInfo> gbOrgInfoList = !gbOrgMap.containsKey(gbCardId) ? null : ((List<GbOrgInfo>) gbOrgMap.get(gbCardId));//是否在干部表中查询到干部数据
            if(CollectionUtil.isNotEmpty(gbOrgInfoList)){
                //禁止交易表中干部有几条数据 取决干部所在几个组织
                for (GbOrgInfo gbOrgInfo:gbOrgInfoList){
                    BanDealInfo bandealInfo = new BanDealInfo();
                    bandealInfo.setCardId(gbCardId);
                    bandealInfo.setName(gbOrgInfo.getName());
                    bandealInfo.setCompany(gbOrgInfo.getUnit());
                    bandealInfo.setPost(gbOrgInfo.getPost());
                    bandealInfo.setPostType(gbOrgInfo.getPostType());
                    bandealInfo.setBanPostType(gbOrgInfo.getPostType());//禁止职务类型

                    bandealInfo.setCreateTime(DateUtil.date());
                    bandealInfo.setUpdateTime(DateUtil.date());

                    bandealInfo.setFamilyName(info.getName());
                    bandealInfo.setRelation(info.getTitle());
                    bandealInfo.setEngageType(sysDictBizService.getDictId("开办有偿社会中介和法律服务结构或从业",dictList));
                    //13-2表中的，显示Z（个人认缴出资额或个人出资额（人民币万元）），AA（个人认缴出资比例或个人出资比例（%）），AC（是否在该机构中从业），AD（所担任的职务名称）列内容
                    //------如果AC值为是，则拼接AD列，否则不拼接
                    String engageInfo = "个人认缴出资额或个人出资额（人民币万元）:" +(StrUtil.isEmpty( info.getPersonalCapital())?"": info.getPersonalCapital()) + ",个人认缴出资比例或个人出资比例（%）:" + (StrUtil.isEmpty(info.getPersonalRatio())?"":info.getPersonalRatio())
                            + ("是".equals(sysDictBizService.getDictValue(info.getPractice(),dictList)) ? ",所担任的职务名称:" + (StrUtil.isEmpty(info.getPostName())?"":info.getPostName()) : "");

                    bandealInfo.setEngageInfo(engageInfo);
                    bandealInfo.setOperatScope(info.getOperatScope());
                    bandealInfo.setSupplier(info.getOrganizationName());
                    bandealInfo.setCode(info.getCode());
                    //验证在数据库中必填字段是否有值。无值则不插入
                    boolean b = this.validRequiredFeild(info.getId(),bandealInfo,submitFailId);
                    if(!b){
                        continue;
                    }
                    String company = gbOrgInfo.getUnit();
                    String department = gbOrgInfo.getDeparment();
                    if ((StrUtil.isEmpty(company)||StrUtil.isEmpty(department)) || !(company.contains("中国五矿集团有限公司")&&department.contains("专职董(监)事办公室"))) { //如果该干部在集团专职董监事,禁止交易采购单位名称手动录入
                        String banPostType = bandealInfo.getBanPostType();
                        String whether = SystemConstant.WHETHER_YES;
                        String purchaseCode = Objects.isNull(gbOrgInfo) ? "" : gbOrgInfo.getUnitCode();//禁止交易采购单位代码
                        String purchaseName = Objects.isNull(gbOrgInfo) ? "" : gbOrgInfo.getUnit();
                        ;//禁止交易采购单位名称
                        if ("党组管理干部正职".equals(sysDictBizService.getDictValue(banPostType,dictList))) {
                            purchaseCode = "60000001";
                            purchaseName = "中国五矿集团有限公司";
                        } else if ("总部处长".equals(sysDictBizService.getDictValue(banPostType,dictList))) {
                            whether = SystemConstant.WHETHER_NO;//是否继承关系
                            Org org = orgService.getOrgByUnitCodeAndDepartmentName(gbOrgInfo.getUnitCode(),gbOrgInfo.getDeparment());
                            purchaseCode = Objects.isNull(org) ? "" : org.getAsgorgancode();//禁止交易采购单位代码
                            purchaseName = Objects.isNull(org) ? "" : org.getAsgorganname();//禁止交易采购单位名称

                        }
                        bandealInfo.setIsExtends(whether);//是否继承关系
                        bandealInfo.setBanPurchaseCode(purchaseCode);//禁止交易采购单位代码
                        bandealInfo.setBanPurchaseName(purchaseName);//禁止交易采购单位名称
                    }
                    bandealInfo.setManageCompany(info.getOrgName());//管理单位名称
                    bandealInfo.setManageCompanyCode(info.getOrgCode()); //管理单位代码
                    bandealInfo.setCreator(info.getCreateName()); //创建人
                    bandealInfo.setCreatorAccount(info.getCreateAccount()); //创建人账号
                    bandealInfo.setRefId(info.getId());//关联数据id
                    //验证供应商名称 信用代码  禁止交易采购单位代码是否都有。没有则置为无效
                    bandealInfo = validSupplierAndCodeAndBanPurchaseCode(bandealInfo, SystemConstant.SAVE_STATE); //新建
                    banDealInfoList.add(bandealInfo);
                }
                submitId.add(info.getId());
            }else{
                KVVO vo = new KVVO();
                vo.setId(info.getId());
                vo.setName("未找到干部信息");
                submitFailId.add(vo);
            }

        }
        //社会企业信用代码验证
        List<BanDealInfo> newBanDealInfoList = this.validBatchCompanyCode(banDealInfoList);

        //在禁止企业交易信息表中添加数据
        boolean b = this.saveBatch(newBanDealInfoList);
        //新增操作记录
        boolean deal = banDealInfoRecordService.insertBanDealInfoRecord(newBanDealInfoList, SystemConstant.OPERATION_TYPE_ADD); //新增
        Map<String,Object> map = new HashMap();
        map.put("submitIds",submitId); //记录不能提交的数据
        map.put("submitFailId",submitFailId); //记录无法提交的数据
        map.put("banDeal",b); //禁止交易数据是否提交成功
        map.put("banDealRecord",deal); //禁止交易记录数据是否提交成功
        return ResultMsgUtil.success(map);
    }

    @Transactional
    @Override
    public ResultMsgUtil<Object>  submitBanDealInfo(List<String> ids) {
        boolean b = false;
        List<BanDealInfo> infoList = this.lambdaQuery().in(BanDealInfo::getId, ids).list();
        if (CollectionUtil.isNotEmpty(infoList)) {
            long count = infoList.stream().filter(e -> SystemConstant.VALID_STATE.equals(e.getState())).count();
            if (count > 0) {
                return ResultMsgUtil.failure("当前表中的有效数据不能重复!");
            }
            //验证社会统一信用代码 不符合则在数据校验提示列中显示
            infoList = this.validBatchCompanyCode(infoList);
            //验证证供应商名称 信用代码  禁止交易采购单位代码是否都有。没有则置为无效，否则设置为有效
            infoList = validBatchSupplierAndCodeAndBanPurchaseCode(infoList, SystemConstant.VALID_STATE); //有效
            //修改数据
            b = this.updateBatchById(infoList);

        }
        if (b) {
            //推送有效数据给采购平台
           //purchaseBanDealInfoSevice.pushDatchDataForPurchase(infoList);
        }
        return ResultMsgUtil.success("提交成功");
    }

    @Override
    public List<BanDealInfo> validBatchCompanyCode(List<BanDealInfo> infoList) {
        try {
            List<String> companyList = infoList.stream().map(BanDealInfo::getCompany).collect(Collectors.toList());
            JSONObject jbParam = new JSONObject();
            jbParam.put("name", companyList);
            //调用企业画像接口
            JSONObject resultOb = this.getCompanyInfoByName(jbParam);
            boolean status = resultOb.getBoolean("status");
            boolean b = resultOb.containsKey("data");
            Map<String, String> map = new HashMap<>();
            if (status && b) {
                JSONArray data = resultOb.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    String name = obj.getString("name");
                    String creditNo = obj.getString("creditNo");
                    map.put(name, creditNo);
                }
            }
            if (map != null && map.size() > 0) {
                //List<String> ids = new ArrayList<>();
                infoList.stream().forEach(e -> {
                    String company = e.getCompany();
                    boolean ck = map.containsKey(company);
                    if (ck) {
                        String code = e.getCode();
                        String creditNo = map.get(company).toString();
                        if (!creditNo.equals(code)) {//查询出来的信用代码和填写的不一致
                            String tips = e.getCheckTips();
                            if (StrUtil.isBlank(tips) || !tips.contains("企业名称和统一社会信用代码/注册号不匹配")) {
                                StringJoiner sj = new StringJoiner(",");
                                sj.add(StrUtil.isEmpty(e.getCheckTips())?"":e.getCheckTips());
                                sj.add("企业名称和统一社会信用代码/注册号不匹配");
                                e.setCheckTips(sj.toString());
                            }

                            // ids.add(e.getId());
                        } else { //查询出来的信用代码和填写的一致
                            String tips = e.getCheckTips();
                            if (StrUtil.isNotBlank(tips) && tips.contains("企业名称和统一社会信用代码/注册号不匹配")) {
                                tips = tips.replace("企业名称和统一社会信用代码/注册号不匹配", "");
                                e.setCheckTips(tips);
                            }
                        }
                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return infoList;
    }

    @Override
    public BanDealInfo validCompanyCode(BanDealInfo banDealInfo) {
        try {
            JSONObject jbParam = new JSONObject();
            String company = banDealInfo.getCompany();
            jbParam.put("name", company);
            //调用企业画像接口
            JSONObject resultOb = this.getCompanyInfoByName(jbParam);
            boolean status = resultOb.getBoolean("status");
            boolean b = resultOb.containsKey("data");
            Map<String, String> map = new HashMap<>();
            if (status && b) {
                JSONArray data = resultOb.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    String name = obj.getString("name");
                    String creditNo = obj.getString("creditNo");
                    map.put(name, creditNo);
                }
            }
            if (map != null && map.size() > 0) {
                //List<String> ids = new ArrayList<>();
                String companyName = banDealInfo.getCompany();
                boolean ck = map.containsKey(companyName);
                if (ck) {
                    String code = banDealInfo.getCode();
                    String creditNo = map.get(companyName).toString();
                    if (!creditNo.equals(code)) {//查询出来的信用代码和填写的不一致
                        String tips = banDealInfo.getCheckTips();
                        if (StrUtil.isNotBlank(tips) && !tips.contains("企业名称和统一社会信用代码/注册号不匹配")) {
                            StringJoiner sj = new StringJoiner(",");
                            sj.add(StrUtil.isEmpty(banDealInfo.getCheckTips())?"":banDealInfo.getCheckTips());
                            sj.add("企业名称和统一社会信用代码/注册号不匹配");
                            banDealInfo.setCheckTips(sj.toString());
                        }

                        // ids.add(e.getId());
                    } else { //查询出来的信用代码和填写的一致
                        String tips = banDealInfo.getCheckTips();
                        if (StrUtil.isNotBlank(tips) && tips.contains("企业名称和统一社会信用代码/注册号不匹配")) {
                            tips = tips.replace("企业名称和统一社会信用代码/注册号不匹配", "");
                            banDealInfo.setCheckTips(tips);
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return banDealInfo;
    }


    @Override
    public List<BanDealInfo> validBatchSupplierAndCodeAndBanPurchaseCode(List<BanDealInfo> infoList, String state) {
        infoList.stream().forEach(e -> {
            String supplier = e.getSupplier();//供应商名称
            String code = e.getCode();//信用代码
            String banPurchaseCode = e.getBanPurchaseCode();
            if (StrUtil.isNotEmpty(supplier) && StrUtil.isNotEmpty(code) && StrUtil.isNotEmpty(banPurchaseCode)) {
                e.setState(state);
            } else {
                e.setState(SystemConstant.INVALID_STATE); //无效
            }
        });
        return infoList;
    }

    @Override
    public BanDealInfo validSupplierAndCodeAndBanPurchaseCode(BanDealInfo banDealInfo, String state) {
        String supplier = banDealInfo.getSupplier();//供应商名称
        String code = banDealInfo.getCode();//信用代码
        String banPurchaseCode = banDealInfo.getBanPurchaseCode();
        if (StrUtil.isNotEmpty(supplier) && StrUtil.isNotEmpty(code) && StrUtil.isNotEmpty(banPurchaseCode)) {
            banDealInfo.setState(state);
        } else {
            banDealInfo.setState(SystemConstant.INVALID_STATE); //无效
        }
        return banDealInfo;
    }

    @Override
    public boolean deleteBanDealInfo(List<String> ids) {
        //新增日志记录
        List<BanDealInfo> infoList = this.lambdaQuery().in(BanDealInfo::getId, ids).list();
        banDealInfoRecordService.insertBanDealInfoRecord(infoList, SystemConstant.OPERATION_TYPE_REMOVE);
        return this.removeByIds(ids);
    }

    @Transactional
    @Override
    public boolean deleteBanDealInfoByRefId(List<String> ids) {
        QueryWrapper<BanDealInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("ref_id", ids);
        int delete = this.baseMapper.delete(queryWrapper);
        return delete > 0 ? true : false;
    }

    @Override
    public int countByCardIdAndNameAndCode(String cardId, String name, String code) {
        Integer count = this.lambdaQuery().eq(BanDealInfo::getFamilyName, name).eq(BanDealInfo::getCardId, cardId).eq(BanDealInfo::getCode, code).count();
        return Objects.isNull(count) ? 0 : count.intValue();
    }

    @Override
    public List<ProhibitTransactionExcelVO> export(List<String> ids) {
        List<SysDictBiz> dictList = sysDictBizService.selectList();
        List<ProhibitTransactionExcelVO> list =  this.baseMapper.selectBatchIds(ids).stream().map(t -> {
            ProhibitTransactionExcelVO vo = new ProhibitTransactionExcelVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).collect(Collectors.toList());
        list = this.replaceDictValue(list,dictList);
        return list;
    }

    private List<ProhibitTransactionExcelVO> replaceDictValue(List<ProhibitTransactionExcelVO>  list,List<SysDictBiz> dictList){
        list.parallelStream().forEach(t->{
            String postType = sysDictBizService.getDictValue(t.getPostType(),dictList);
            String banPostType = sysDictBizService.getDictValue(t.getBanPostType(),dictList);
            String relation = sysDictBizService.getDictValue(t.getRelation(),dictList);
            String engageType = sysDictBizService.getDictValue(t.getEngageType(),dictList);
            String isExtends = sysDictBizService.getDictValue(t.getIsExtends(),dictList);
            String state = sysDictBizService.getDictValue(t.getState(),dictList);

            t.setPostType(postType);
            t.setBanPostType(banPostType);
            t.setRelation(relation);
            t.setEngageType(engageType);
            t.setIsExtends(isExtends);
            t.setState(state);

        });
        return list;
    }

    /**
     * 验证数据库中必填字段是否有值。验证通过返回true,否则返回flse
     * @param infoId 提交数据id
     * @param bandealInfo 验证数据实体
     * @param submitFailId 存储验证提示的集合
     * @return
     */
    private  boolean  validRequiredFeild(String infoId,BanDealInfo bandealInfo,List<KVVO>  submitFailId){
        String cardId = bandealInfo.getCardId(); //身份证
        if(StrUtil.isEmpty(cardId)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("干部的身份证为空");
            submitFailId.add(vo);
            return false;
        }
        String name = bandealInfo.getName(); //姓名
        if(StrUtil.isEmpty(name)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("干部姓名为空");
            submitFailId.add(vo);
            return false;
        }
        String company = bandealInfo.getCompany(); //单位
        if(StrUtil.isEmpty(company)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("干部的单位为空");
            submitFailId.add(vo);
            return false;
        }
        String post = bandealInfo.getPost(); //职务
        if(StrUtil.isEmpty(post)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("干部的职务为空");
            submitFailId.add(vo);
            return false;
        }
        String postType = bandealInfo.getPostType(); //职务类型
        if(StrUtil.isEmpty(postType)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("干部的职务类型为空");
            submitFailId.add(vo);
            return false;
        }
        String familyName = bandealInfo.getFamilyName(); //家人姓名
        if(StrUtil.isEmpty(familyName)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("家人姓名为空");
            submitFailId.add(vo);
            return false;
        }
        String engageType = bandealInfo.getEngageType(); //经商类型
        if(StrUtil.isEmpty(engageType)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("经商类型为空");
            submitFailId.add(vo);
            return false;
        }
        String engageInfo = bandealInfo.getEngageInfo(); //经商类型详细描述
        if(StrUtil.isEmpty(engageInfo)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("经商类型详细描述为空");
            submitFailId.add(vo);
            return false;
        }
        String supplier = bandealInfo.getSupplier(); //供应商名称
        if(StrUtil.isEmpty(supplier)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("供应商名称为空");
            submitFailId.add(vo);
            return false;
        }
        String code = bandealInfo.getCode(); //社会信用代码
        if(StrUtil.isEmpty(code)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("统一社会信用代码为空");
            submitFailId.add(vo);
            return false;
        }
        /*String state = bandealInfo.getState(); //状态
        if(StrUtil.isEmpty(state)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("状态为空");
            submitFailId.add(vo);
            return false;
        }*/
        String banPostType = bandealInfo.getBanPostType(); //禁业职务类型
        if(StrUtil.isEmpty(banPostType)){
            KVVO vo = new KVVO();
            vo.setId(infoId);
            vo.setName("禁业职务类型为空");
            submitFailId.add(vo);
            return false;
        }
        return true;
    }
}
