package com.cisdi.transaction.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.domain.dto.BanDealInfoDTO;
import com.cisdi.transaction.domain.dto.SubmitDto;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.ProhibitTransactionExcelVO;

import java.util.List;
import java.util.Map;

/**禁止企业交易信息
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 14:27
 */
public interface BanDealInfoService extends IService<BanDealInfo> {
    boolean updateState(List<String> ids, String state);

    public JSONObject getCompanyInfoByName(JSONObject jb);

    public void insertBanDealInfo(BanDealInfoDTO infoDto);
    public void editBanDealInfo(BanDealInfoDTO infoDto);

    /**
     * 通过 投资企业或担任高级职务情况 向禁止交易表中添加数据
     * @param infoList 来源与投资企业或担任高级职务情况表中的数据
     * @param gbOrgList  干部组织基本信息
     */
    public ResultMsgUtil<Map<String,Object>>  insertBanDealInfoOfInvestInfo(List<InvestInfo> infoList, List<GbOrgInfo> gbOrgList);

    /**
     * 通过 投配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况 向禁止交易表中添加数据
     * @param infoList 来源与配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况的数据
     * @param gbOrgList   干部组织基本信息
     */
    public ResultMsgUtil<Map<String,Object>> insertBanDealInfoOfPrivateEquity(List<PrivateEquity> infoList, List<GbOrgInfo> gbOrgList);

    /**
     * 通过 配偶、子女及其配偶开办有偿社会中介和法律服务机构或者从业的情况  向禁止交易表中添加数据
     * @param infoList 来源与配偶、子女及其配偶投资私募股权投资基金或者担任高级职务的情况的数据
     * @param gbOrgList  干部组织基本信息
     */
    public ResultMsgUtil<Map<String,Object>>  insertBanDealInfoOfMechanismInfo(List<MechanismInfo> infoList, List<GbOrgInfo> gbOrgList);

    /**
     * 提交禁止交易信息
     * @param ids 提交数据id
     */
    public ResultMsgUtil<Object>  submitBanDealInfo(List<String> ids);

    /**
     * 批量验证企业社会信用代码是否正确,否则在数据提示列提示
     * @param infoList
     * @return
     */
    List<BanDealInfo> validBatchCompanyCode(List<BanDealInfo> infoList);

    /**
     * 验证企业社会信用代码是否正确,否则在数据提示列提示
     * @param banDealInfo
     * @return
     */
    BanDealInfo validCompanyCode(BanDealInfo banDealInfo);

    /**
     * 批量验证验证数据中至少要有供应商名称、统一社会信用代码、禁止交易采购企业代码才能置状态为有效，否则提示该行数据为无效状态
     * @param infoList
     * @param state 验证通过需要设置的状态
     * @return
     */
    List<BanDealInfo> validBatchSupplierAndCodeAndBanPurchaseCode(List<BanDealInfo> infoList,String state);

    /**
     * 验证验证数据中至少要有供应商名称、统一社会信用代码、禁止交易采购企业代码才能置状态为有效，否则提示该行数据为无效状态
     * @param banDealInfo
     * @param state 验证通过需要设置的状态
     * @return
     */
    BanDealInfo validSupplierAndCodeAndBanPurchaseCode(BanDealInfo banDealInfo,String state);

    /**
     * 根据id删除
     * @param ids
     * @return
     */
    boolean deleteBanDealInfo(List<String> ids);

    /**
     * 根据关联关系id删除数据
     * @param ids
     * @return
     */
    boolean deleteBanDealInfoByRefId(List<String> ids);

    /**
     * 根据条件查询禁止交易表中的数据条数
     * @param cardId 干部身份证号
     * @param name 家人姓名
     * @param code 供应商社会信用代码
     * @return
     */
    int countByCardIdAndNameAndCode(String cardId,String name,String code);

    /**
     * 导出
     * @param ids
     * @return
     */
    List<ProhibitTransactionExcelVO> export(List<String> ids);
}
