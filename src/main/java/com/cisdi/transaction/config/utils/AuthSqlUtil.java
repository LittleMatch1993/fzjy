package com.cisdi.transaction.config.utils;


import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.service.OrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.stream.Collectors;

/**
 * @Author: tgl
 * @Description: 权限sql工具
 * @Date: 2022/8/18 13:46
 */
@Component
public class AuthSqlUtil {
    @Autowired
    private OrgService orgService;

    private static OrgService staticOrgService;

    @PostConstruct
    public void initBeans(){
        staticOrgService=orgService;
    }

    /**
     *
     * @param tableName 表名
     * @param orgCode 组织机构编码
     * @return 条件sql
     */
    public static String getAuthSqlByTableNameAndOrgCode(String tableName,String orgCode){
        Org org = staticOrgService.selectByOrgancode(orgCode);
        //级别
        String asglevel = org.getAsglevel();
        int level = Integer.parseInt(asglevel);
        String codePath = org.getAsgpathnamecode();
        String orgNames=staticOrgService.getOrgNamesByCodePath(codePath);
        String cardIds="'"+staticOrgService.getCardIdsByAsgpathnamecode(codePath).stream().distinct().collect(Collectors.joining("','"))+"'";
        String conditionSql="1=1";
        if(level!=0){
            switch (tableName) {
                case ModelConstant.BAN_DEAL_INFO:
                case ModelConstant.BAN_DEAL_INFO_RECORD:
                case ModelConstant.ENTERPRISE_DEAL_INFO:
                    conditionSql = " company IN (" + orgNames + ") AND post_type IN (" + ModelConstant.STR_POST_TYPE + ")";
                    break;
                case ModelConstant.GB_BASIC_INFO:
                    conditionSql = " unit IN (" + orgNames + ") AND post_type IN (" + ModelConstant.STR_POST_TYPE + ")";
                    break;
                case ModelConstant.INVEST_INFO:
                case ModelConstant.MECHANISM_INFO:
                case ModelConstant.PRIVATE_EQUITY:
//                    conditionSql = " company IN (" + orgNames + ") AND card_id IN (" + cardIds + ")";
                    conditionSql = " card_id IN (" + cardIds + ")";
                    break;
                case ModelConstant.ORG:
                    conditionSql = " asgpathnamecode LIKE concat('" + codePath + "','%')";
                    break;
                case ModelConstant.SPOUSE_BASIC_INFO:
                    conditionSql = " cadre_card_id IN (" + cardIds + ")";
                    break;
            }
        }
        return conditionSql;

    }


    /**
     *
     * @param tableName 表名
     * @param orgCodes 多个组织机构编码
     * @return 条件sql
     */
//    public static String getAuthSqlByTableNameAndOrgCodes(String tableName,String orgCodes){
//
//        List<String> orgCodeList = Arrays.asList(orgCodes.split(","));
//        if (orgCodeList.size()==1){
//            return getAuthSqlByTableNameAndOrgCode(tableName,orgCodeList.get(0));
//        }
//
//        List<Org> orgs = staticOrgService.selectByOrgancodes(orgCodeList);
//        Map<String, List<Org>> asglevelOrgMap = orgs.stream().collect(Collectors.groupingBy(Org::getAsglevel));
//
//
//        //级别
//        String asglevel = org.getAsglevel();
//        int level = Integer.parseInt(asglevel);
//        String codePath = org.getAsgpathnamecode();
//        String orgNames=staticOrgService.getOrgNamesByCodePath(codePath);
//        String cardIds="'"+staticOrgService.getCardIdsByAsgpathnamecode(codePath).stream().distinct().collect(Collectors.joining("','"))+"'";
//        String conditionSql="1=1";
//        if(level!=0){
//            switch (tableName) {
//                case ModelConstant.BAN_DEAL_INFO:
//                case ModelConstant.BAN_DEAL_INFO_RECORD:
//                case ModelConstant.ENTERPRISE_DEAL_INFO:
//                    conditionSql = " company IN (" + orgNames + ") AND post_type IN (" + ModelConstant.STR_POST_TYPE + ")";
//                    break;
//                case ModelConstant.GB_BASIC_INFO:
//                    conditionSql = " unit IN (" + orgNames + ") AND post_type IN (" + ModelConstant.STR_POST_TYPE + ")";
//                    break;
//                case ModelConstant.INVEST_INFO:
//                case ModelConstant.MECHANISM_INFO:
//                case ModelConstant.PRIVATE_EQUITY:
//                    conditionSql = " company IN (" + orgNames + ") AND card_id IN (" + cardIds + ")";
//                    break;
//                case ModelConstant.ORG:
//                    conditionSql = " asgpathnamecode LIKE concat('" + codePath + "','%')";
//                    break;
//                case ModelConstant.SPOUSE_BASIC_INFO:
//                    conditionSql = " cadre_card_id IN (" + cardIds + ")";
//                    break;
//            }
//        }
//        return conditionSql;
//
//    }
}
