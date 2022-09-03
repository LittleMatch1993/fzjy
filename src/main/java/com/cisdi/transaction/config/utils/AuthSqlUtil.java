package com.cisdi.transaction.config.utils;


import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.service.OrgService;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
//        return getAuthSqlByTableNameAndOneOrgCode(tableName,orgCode);
        return getAuthSqlByTableNameAndMoreOrgCode(tableName,orgCode);
    }

    private static String getAuthSqlByTableNameAndOneOrgCode(String tableName,String orgCode){
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

    private static String getAuthSqlByTableNameAndMoreOrgCode(String tableName,String orgCodes){
        List<String> orgCodeList = Arrays.stream(orgCodes.split(",")).distinct().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        List<Org> orgs = staticOrgService.selectByOrgancodes(orgCodeList);
        String conditionSql=" 1=1 ";
        if (orgs.stream().map(Org::getAsglevel).collect(Collectors.toList()).contains("0")){
            return conditionSql;
        }
        //return "'"+this.lambdaQuery().likeRight(Org::getAsgpathnamecode, codePath).list().
        // stream().distinct().map(Org::getAsgorganname).collect(Collectors.joining("','"))+"'";

        List<String> orgNameList= Lists.newArrayList();
        Set<String> codePathList=new HashSet<>();
        List<String> cardIdList=Lists.newArrayList();
        for (Org org : orgs) {
            String codePath = org.getAsgpathnamecode();
            codePathList.add(codePath);
            List<String> orgNames=staticOrgService.getOrgNameListByCodePath(codePath);
            if (!CollectionUtils.isEmpty(orgNames)){
                orgNameList.addAll(orgNames);
            }
            List<String> cardIds=staticOrgService.getCardIdsByAsgpathnamecode(codePath).stream().distinct().collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(cardIds)){
                cardIdList.addAll(cardIds);
            }
        }
        String orgNames="'"+orgNameList.stream().distinct().collect(Collectors.joining("','"))+"'";
        String cardIds="'"+cardIdList.stream().distinct().collect(Collectors.joining("','"))+"'";

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
                StringBuilder sqlBuilder=new StringBuilder(" ( ");
                for (String codePath : codePathList) {
                    sqlBuilder.append(" asgpathnamecode LIKE concat('").append(codePath).append("','%') or ");
                }
                conditionSql=sqlBuilder.substring(0,sqlBuilder.length()-3)+" )";
                break;
            case ModelConstant.SPOUSE_BASIC_INFO:
                conditionSql = " cadre_card_id IN (" + cardIds + ")";
                break;
        }

        return conditionSql;
    }

    public static String getAuthSqlForPathnamecodeRegexp(List<String> pathnamecodeList){
        String conditionSql = "asgpathnamecode regexp";
        String pathnamecodeStr = pathnamecodeList.stream().collect(Collectors.joining("|"));
        conditionSql = conditionSql.concat(" '^(").concat(pathnamecodeStr).concat(")'");
        return conditionSql;
    }

}
