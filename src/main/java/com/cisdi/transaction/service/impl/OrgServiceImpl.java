package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.utils.AuthSqlUtil;
import com.cisdi.transaction.config.utils.HttpUtils;
import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.domain.OrgTree;
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.InstitutionalFrameworkDTO;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.model.GbOrgInfo;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.domain.model.SysDictBiz;
import com.cisdi.transaction.domain.vo.InstitutionalFrameworkExcelVO;
import com.cisdi.transaction.domain.vo.OrgConditionVO;
import com.cisdi.transaction.domain.vo.OrgVo;
import com.cisdi.transaction.domain.vo.SearchVO;
import com.cisdi.transaction.mapper.master.OrgMapper;
import com.cisdi.transaction.service.OrgService;
import com.cisdi.transaction.service.SysDictBizService;
import com.cisdi.transaction.util.ThreadLocalUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.net.BindException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/1 23:53
 */
@Service
public class OrgServiceImpl extends ServiceImpl<OrgMapper, Org> implements OrgService {

    @Value("${org.url}")
    private String url;

    @Autowired
    private SysDictBizService sysDictBizService;

    @Override
    public void saveInfo(InstitutionalFrameworkDTO dto) {
        //Todo ???????????????????????????
        // this.lambdaQuery().eq(Org::getId)
        Org org = new Org();
        BeanUtil.copyProperties(dto, org);
        org.setCreateTime(DateUtil.date());
        org.setUpdateTime(DateUtil.date());
        org.setTenantId(dto.getServiceLesseeId());
        org.setCreatorId(dto.getServiceUserId());
        this.save(org);

    }

    @Override
    public boolean removeByAsgDate(String asgDate) {
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Org::getAsgdate, asgDate);
        return this.remove(queryWrapper);
    }

    @Override
    public int countByAncodeAndPathNamecode(String anCode, String pathNamecode) {
        Integer count = this.lambdaQuery().eq(Org::getAsgorgancode, anCode).eq(Org::getAsgpathnamecode, pathNamecode).count();

        return Objects.isNull(count) ? 0 : count.intValue();
    }

    @Override
    public void editOrgInfo(InstitutionalFrameworkDTO dto) {
        Org org = new Org();
        BeanUtil.copyProperties(dto, org);
        org.setUpdateTime(DateUtil.date());
        org.setUpdaterId(dto.getServiceUserId());
        this.updateById(org);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void syncDa(String stringDate) {
        JSONObject obj = new JSONObject();
        //??????????????????
        obj.put("app_id", "sphy");
        obj.put("table_name", "");
        if(StrUtil.isEmpty(stringDate)){
            stringDate = "18000101";
        }
        obj.put("stringDate", stringDate);
        obj.put("condition", "1=1");
        obj.put("secretKey", "50857140b5b84ddeeef5f62709b32fac");
        String result = HttpUtils.sendPostOfAuth(url, obj);
        JSONObject jb = JSONObject.parseObject(result);
        boolean b1 = jb.containsKey("code");
        if(b1){
            String code = jb.getString("code");
            boolean b = jb.containsKey("data");
            List<Org> orgs = new ArrayList<>();
            if ("1".equals(code) && b) {

                JSONArray data = jb.getJSONArray("data");
                long dateLong = DateUtil.date().getTime();
                 //date.getTime()
                for (int i = 0; i < data.size(); i++) {
                    JSONObject josnObj = data.getJSONObject(i);
                    Org org = JSONObject.parseObject(josnObj.toString(), Org.class);
                    String asgId = josnObj.getString("asgId");
                    org.setId(asgId);
                    DateTime date = DateUtil.date(dateLong + (i*1000));
                    org.setCreateTime(date);
                    org.setUpdateTime(date);
                    orgs.add(org);
                }

            }
            if (CollectionUtil.isNotEmpty(orgs)) {
                List<SysDictBiz> dictList = sysDictBizService.selectList();
                orgs = this.repalceDictId(orgs,dictList);
                //this.remove(null);
                //this.saveBatch(orgs);
                boolean removeB = false;
                if("18000101".equals(stringDate)){//????????????
                    removeB = this.remove(null);
                    this.saveBatch(orgs);
                }else{//????????????
                    removeB = this.removeByAsgDate(stringDate);
                    this.saveBatch(orgs);
                }

                System.out.println("service??????????????????????????????????????????");

            }
            System.out.println("service????????????????????????????????????");
        }else{
            System.out.println("????????????????????????");
        }

    }

    @Override
    public List<InstitutionalFrameworkExcelVO> export(List<String> ids) {
        return this.baseMapper.selectBatchIds(ids).stream().map(t -> {
            InstitutionalFrameworkExcelVO vo = new InstitutionalFrameworkExcelVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Org selectByOrgancode(String orgCode) {

        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Org::getAsgorgancode,orgCode);
        Org org = this.getOne(queryWrapper);
        return org;
    }

    @Override
    public List<Org> selectByOrgancode(List<String> orgCodes) {
        if(CollectionUtil.isEmpty(orgCodes)){
            return null;
        }
        return  this.lambdaQuery().in(Org::getAsgorgancode,orgCodes).list();
    }

    @Override
    public String getHighestLevel(List<Org> orgs) {
        if(CollectionUtil.isEmpty(orgs)){
            return null;
        }
        //Asglevel ?????????????????????
        String  level  = orgs.stream().min(Comparator.comparing(Org::getAsglevel)).get().getAsglevel();
        return level;
    }

    @Override
    public List<Org> selectChildOrgByPathnamecode(String pathnamecode) {
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode);
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<Org> selectByName(SearchVO search) {
        List<Org> list = new ArrayList<>();
        //Object orgCode = ThreadLocalUtils.get("orgCode");
        String orgCode = search.getOrgCode();
        if(StrUtil.isEmpty(orgCode)){
            return list;
        }
        List<String> orgCodeList = Arrays.stream(orgCode.split(",")).distinct().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        List<Org> orgList = this.selectByOrgancode(orgCodeList);
        if(CollectionUtil.isEmpty(orgList)){
            return list;
        }
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        String asglevel = this.getHighestLevel(orgList);
        List<String> names = search.getKeyword();
        List<String> codes = search.getCodes();
        if(CollectionUtil.isNotEmpty(codes)){
            queryWrapper.orderByDesc(AuthSqlUtil.getAuthSqlForAsgorganCodeOrderBy(codes));
            queryWrapper.lambda().in(CollectionUtil.isNotEmpty(codes), Org::getAsgorgancode,codes).or();
        }
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){ //?????????
            if(CollectionUtil.isNotEmpty(names)){
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i);
                    if(i==names.size()-1){
                        queryWrapper.lambda().like(Org::getAsgorganname, name);
                    }else{
                        queryWrapper.lambda().like(Org::getAsgorganname, name).or();
                    }
                }
            }
            queryWrapper.lambda().last(SqlConstant.ONE_SQL_YB);
        }else{
            List<String> pathnamecodeList = orgList.stream().map(Org::getAsgpathnamecode).collect(Collectors.toList());
            if(CollectionUtil.isNotEmpty(names)){
                //queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode);
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i);
                    if(i==names.size()-1){
                        queryWrapper.lambda().like(Org::getAsgorganname, name);

                    }else{
                        queryWrapper.lambda().like(Org::getAsgorganname, name).or();

                    }
                }

               // queryWrapper.lambda().like(Org::getAsgorganname, name);
            }
            queryWrapper.lambda().apply(AuthSqlUtil.getAuthSqlForPathnamecodeRegexp(pathnamecodeList)).last(SqlConstant.ONE_SQL_YB);;
        }
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<Org> selectUnitByName(String name, String orgCode) {
        List<Org> list = new ArrayList<>();
        //Object orgCode = ThreadLocalUtils.get("orgCode");
        if(StrUtil.isEmpty(orgCode)){
            return list;
        }
        List<String> orgCodeList = Arrays.stream(orgCode.split(",")).distinct().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        List<Org> orgList = this.selectByOrgancode(orgCodeList);
       // Org org = this.selectByOrgancode(orgCode.toString());
        if(CollectionUtil.isEmpty(orgList)){
            return list;
        }
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
       // String asglevel = org.getAsglevel();
        String asglevel =  this.getHighestLevel(orgList);
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){ //?????????
            if(StrUtil.isNotEmpty(name)){
                if(name.endsWith("??????")||name.endsWith("???")){
                    queryWrapper.lambda().like(Org::getAsgorganname, name);
                }else{
                    queryWrapper.lambda().like(Org::getAsgorganname, name+"%??????");
                }

            }else{
                queryWrapper.lambda().likeLeft(Org::getAsgorganname, "??????").last(SqlConstant.ONE_SQL_YB);;
            }
        }else{
            List<String> pathnamecodeList = orgList.stream().map(Org::getAsgpathnamecode).collect(Collectors.toList());
            if(StrUtil.isNotEmpty(name)){
                if(name.endsWith("??????")||name.endsWith("???")){
                    queryWrapper.lambda().like(Org::getAsgorganname, name);
                }else{
                    queryWrapper.lambda().like(Org::getAsgorganname, name+"%??????");
                }
                queryWrapper.lambda().apply(AuthSqlUtil.getAuthSqlForPathnamecodeRegexp(pathnamecodeList));
            }else{
                queryWrapper.lambda().likeLeft(Org::getAsgorganname, "??????");
                queryWrapper.lambda().apply(AuthSqlUtil.getAuthSqlForPathnamecodeRegexp(pathnamecodeList)).last(SqlConstant.ONE_SQL_YB);
            }
        }

        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public Org getOrgByUnitCodeAndDepartmentName(String unitCode, String departmentName) {
         List<Org> org = this.baseMapper.getOrgByUnitCodeAndDepartmentName(unitCode, departmentName);
         if(CollectionUtil.isEmpty(org)){
             return null;
         }
         return org.get(0);
    }

    @Override
    public List<String> getCardIdsByAsgpathnamecode(String asgpathnamecode) {
        if (StringUtils.isBlank(asgpathnamecode)){
            return null;
        }
        return this.baseMapper.getCardIdsByAsgpathnamecode(asgpathnamecode);
    }

    @Override
    public String getOrgNamesByCodePath(String codePath) {
        return "'"+this.lambdaQuery().likeRight(Org::getAsgpathnamecode, codePath).list().stream().distinct().map(Org::getAsgorganname).collect(Collectors.joining("','"))+"'";
    }

    private List<Org>  repalceDictId(List<Org> list, List<SysDictBiz> dictList){
        list.parallelStream().forEach(dto->{
            String s1 = dto.getAsgleadfg();
            String s2 = dto.getAsglead();
            s1 = "0".equals(s1)?"???":"???";
            s2 = "0".equals(s2)?"???":"???";
            //???????????????
            String asgleadfg = sysDictBizService.getDictId(s1, dictList);
            String asglead = sysDictBizService.getDictId(s2, dictList);
            dto.setAsglead(asglead);
            dto.setAsgleadfg(asgleadfg);

        });
        return list;
    }

    @Override
    public List<InstitutionalFrameworkExcelVO> export(CadreFamilyExportDto dto){

        return this.list(new QueryWrapper<Org>()
                .orderBy(StringUtils.isNotBlank(dto.getColumnName())&&Objects.nonNull(dto.getIsAsc()),dto.getIsAsc(),dto.getColumnName())
                .orderByDesc(StringUtils.isBlank(dto.getColumnName())||Objects.isNull(dto.getIsAsc()),"create_time")
                .lambda()
                .like(StringUtils.isNotBlank(dto.getAsgorganname()),Org::getAsgorganname,dto.getAsgorganname())
                .like(StringUtils.isNotBlank(dto.getAsgorgancode()),Org::getAsgorgancode,dto.getAsgorgancode())
                .eq(StringUtils.isNotBlank(dto.getAsglead()),Org::getAsglead,dto.getAsglead())
                .eq(StringUtils.isNotBlank(dto.getAsgleadfg()),Org::getAsgleadfg,dto.getAsgleadfg())
                .apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.ORG,dto.getOrgCode()))
        ).stream().map(t -> {
            InstitutionalFrameworkExcelVO vo = new InstitutionalFrameworkExcelVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<OrgVo> selectOrgByOrgCode(OrgConditionVO searchVO) {
//        return selectOrgByOneOrgCode(searchVO);
        return selectOrgByMoreOrgCode(searchVO);
    }

    /**
     * ????????????????????????
     * @param searchVO
     * @return
     */
    private List<OrgVo> selectOrgByOneOrgCode(OrgConditionVO searchVO){
        //?????????orgCode
        String searchOrgCode = searchVO.getSearchOrgCode();
        //??????????????????orgCode
        String orgCode = searchVO.getOrgCode();
        List<Org> currentUserOrg = this.lambdaQuery().eq(Org::getAsgorgancode, orgCode).last(SqlConstant.ONE_SQL).list();
        //?????????????????????
        Org org = currentUserOrg.get(0);
        if (StringUtils.isBlank(searchOrgCode)){
            //???????????????
            String firstLevel = org.getAsgpathnamecode().split("-")[1];
            Org returnOrg = this.lambdaQuery().eq(Org::getAsgorgancode, firstLevel).last(SqlConstant.ONE_SQL).list().get(0);
            return Collections.singletonList(new OrgVo(returnOrg.getAsgorgancode() + "-" + returnOrg.getAsgorganname(), returnOrg.getAsgorganname(), Boolean.TRUE));
        }else {
            List<Org> currentOrgs = this.lambdaQuery().eq(Org::getAsgorgancode, searchOrgCode).last(SqlConstant.ONE_SQL).list();
            if (CollectionUtils.isEmpty(currentOrgs)){
                return null;
            }
            //??????????????????
            Org currentOrg = currentOrgs.get(0);
            //???????????????????????????
            int i = Integer.parseInt(currentOrg.getAsglevel());
            //?????????????????????????????????
            int j = Integer.parseInt(org.getAsglevel());
            //???????????????????????????????????????????????????????????????????????????????????????????????????
            if (i < j){
                if (!org.getAsgpathnamecode().startsWith(currentOrg.getAsgpathnamecode())){
                    return null;
                }
                //???????????????????????????
                List<Org> nextOrgList = this.lambdaQuery().eq(Org::getAsgorgancode, org.getAsgpathnamecode().split("-")[i+2]).last(SqlConstant.ONE_SQL).list();
                Org nextOrg = nextOrgList.get(0);
                //????????????????????????????????????children
                if (j-i>1){
                    return Collections.singletonList(new OrgVo(nextOrg.getAsgorgancode() + "-" + nextOrg.getAsgorganname(), nextOrg.getAsgorganname(), Boolean.TRUE));
                }else {
                    //???????????????????????????????????????????????????????????????
                    List<Org> orgs = this.lambdaQuery().likeRight(Org::getAsgpathnamecode, org.getAsgpathnamecode() + "-").last(SqlConstant.ONE_SQL).list();
                    if (!CollectionUtils.isEmpty(orgs)){
                        return Collections.singletonList(new OrgVo(nextOrg.getAsgorgancode() + "-" + nextOrg.getAsgorganname(), nextOrg.getAsgorganname(), Boolean.TRUE));
                    }
                }
                return Collections.singletonList(new OrgVo(nextOrg.getAsgorgancode() + "-" + nextOrg.getAsgorganname(), nextOrg.getAsgorganname(), Boolean.FALSE));

            }else{
                if (!currentOrg.getAsgpathnamecode().startsWith(org.getAsgpathnamecode())){
                    return null;
                }
                //???????????????????????????
                List<Org> newOrgs = this.lambdaQuery().likeRight(Org::getAsgpathnamecode, currentOrg.getAsgpathnamecode()+"-").eq(Org::getAsglevel,(i+1)+"").list();
                if (CollectionUtils.isEmpty(newOrgs)){
                    return null;
                }else {
                    //?????????????????????
                    List<Org> nextOrgList = this.lambdaQuery().likeRight(Org::getAsgpathnamecode, currentOrg.getAsgpathnamecode()+"-").eq(Org::getAsglevel,(i+2)+"").list();
                    List<String> asgpathnamecodeList = nextOrgList.stream().map(Org::getAsgpathnamecode).collect(Collectors.toList());
                    //?????????????????????????????????????????????
                    Map<String, Boolean> asgpathnamecodeHaveChildrenMap = newOrgs.stream().collect(Collectors.toMap(Org::getAsgpathnamecode, org1 -> {
                        for (String asgpathnamecode : asgpathnamecodeList) {
                            if (asgpathnamecode.startsWith(org1.getAsgpathnamecode())) {
                                return true;
                            }
                        }
                        return false;
                    }));
                    return newOrgs.stream().map(org1 -> new OrgVo(org1.getAsgorgancode() + "-" + org1.getAsgorganname(), org1.getAsgorganname(),asgpathnamecodeHaveChildrenMap.get(org1.getAsgpathnamecode()) )).collect(Collectors.toList());
                }
            }
        }
    }


    /**
     * ????????????????????????
     * @param searchVO
     * @return
     */
    private List<OrgVo> selectOrgByMoreOrgCode(OrgConditionVO searchVO){
        //?????????orgCode
        String searchOrgCode = searchVO.getSearchOrgCode();
        //??????????????????orgCode
        List<String> orgCodeList = Arrays.stream(searchVO.getOrgCode().split(",")).distinct().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        //????????????orgCode??????????????????????????????
        if (StringUtils.isBlank(searchOrgCode)){
            Set<OrgVo> orgVos=new HashSet<>();
            orgCodeList.forEach(orgCode->{
                searchVO.setOrgCode(orgCode);
                orgVos.addAll(Optional.ofNullable(selectOrgByOneOrgCode(searchVO)).orElse(Lists.newArrayList()));
            });
            if (CollectionUtils.isEmpty(orgVos)){
                return null;
            }else {
                return Lists.newArrayList(orgVos);
            }
        }
        for (String orgCode : orgCodeList) {
            searchVO.setOrgCode(orgCode);
            List<OrgVo> orgVos = selectOrgByOneOrgCode(searchVO);
            if (!CollectionUtils.isEmpty(orgVos)){
                return orgVos;
            }
        }
        return null;
    }

    @Override
    public String selectAsgpathnamecodeByOrgCode(OrgConditionVO searchVO) {
        //???????????????orgCode
        String orgCode = searchVO.getOrgCode();
        //?????????orgCode
        String searchOrgCode = searchVO.getSearchOrgCode();

        if (StringUtils.isBlank(searchOrgCode)){
            return null;
        }
        //???????????????????????????
        Org org = this.lambdaQuery().eq(Org::getAsgorgancode, orgCode).last(SqlConstant.ONE_SQL).one();
        //???????????????????????????
        Org currentOrg = this.lambdaQuery().eq(Org::getAsgorgancode, searchOrgCode).last(SqlConstant.ONE_SQL).one();
        if (Objects.isNull(currentOrg)){
            return null;
        }
        if (org.getAsgpathnamecode().startsWith(currentOrg.getAsgpathnamecode())||currentOrg.getAsgpathnamecode().startsWith(org.getAsgpathnamecode())){
            return currentOrg.getAsgpathnamecode();
        }
        return null;
    }



    @Override
    public List<OrgTree> selectOrgTree(String orgCode) {
        if(StrUtil.isEmpty(orgCode)){
            return new ArrayList<>();
        }
         Org org = this.selectByOrgancode(orgCode);
         if(Objects.isNull(org)){
            return new ArrayList<>();
        }
         String level = org.getAsglevel();
         String asgpathnamecode = org.getAsgpathnamecode();
         if(StrUtil.isEmpty(level)){
             return new ArrayList<>();
         }
        List<Org> orgList = null;
         if("0".equals(level)){//????????????
             orgList = this.baseMapper.selectList(null);
         }else{
             orgList = this.selectChildOrgByPathnamecode(asgpathnamecode);
         }
        List<OrgTree> orgTreeList = new ArrayList<>();
        //???????????????
        OrgTree tree = new OrgTree();
        String name = org.getAsgorganname();
        tree.setId(orgCode);
        tree.setName(name);
        List<OrgTree> tempTreeList = new ArrayList<>();
        tempTreeList= this.getOrgTree(orgList,tempTreeList,asgpathnamecode,Integer.parseInt(level));
        tree.setChildSelect(tempTreeList);
        orgTreeList.add(tree);
        return orgTreeList;
    }

    @Override
    public List<Org> selectByAsgpathnamecode(String asgpathnamecode) {
        QueryWrapper<Org> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,asgpathnamecode);
        List<Org> list = this.baseMapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public List<Org> selectByOrgancodes(List<String> orgCodeList) {
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(Org::getAsgorgancode,orgCodeList);
        return this.list(queryWrapper);
    }

    @Override
    public String selectAsgpathnamecodeByMoreOrgCode(OrgConditionVO searchVO) {
        //???????????????orgCode
        List<String> orgCodeList = Arrays.stream(searchVO.getOrgCode().split(",")).distinct().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        //?????????orgCode
        String searchOrgCode = searchVO.getSearchOrgCode();

        if (StringUtils.isBlank(searchOrgCode)){
            return null;
        }
        //???????????????????????????
        List<Org> orgList = this.lambdaQuery().in(Org::getAsgorgancode, orgCodeList).list();
        //???????????????????????????
        Org currentOrg = this.lambdaQuery().eq(Org::getAsgorgancode, searchOrgCode).last(SqlConstant.ONE_SQL).one();
        if (Objects.isNull(currentOrg)){
            return null;
        }
        for (Org org : orgList) {
            if (org.getAsgpathnamecode().startsWith(currentOrg.getAsgpathnamecode())||currentOrg.getAsgpathnamecode().startsWith(org.getAsgpathnamecode())){
                return currentOrg.getAsgpathnamecode();
            }
        }
        return null;
    }

    @Override
    public List<String> getOrgNameListByCodePath(String codePath) {
        return this.lambdaQuery().likeRight(Org::getAsgpathnamecode, codePath).list().stream().distinct().map(Org::getAsgorganname).collect(Collectors.toList());
    }

    private List<OrgTree> getOrgTree(List<Org> orgList,List<OrgTree> orgTreeList,String asgpathnamecode,int level){

         List<Org> tempList = orgList.stream().filter(e -> e.getAsgpathnamecode().startsWith(asgpathnamecode) && Integer.parseInt(e.getAsglevel()) == level + 1).collect(Collectors.toList());
         if(CollectionUtil.isEmpty(tempList)){
             return new ArrayList<>();
         }
        for (Org org : tempList) {
            OrgTree tree = new OrgTree();
            String orgCode  = org.getAsgorgancode();
            String name = org.getAsgorganname();
            String pathcode = org.getAsgpathnamecode();
            String asglevel = org.getAsglevel();
            tree.setId(orgCode);
            tree.setName(name);
            List<OrgTree> tempTreeList = new ArrayList<>();
            List<OrgTree> orgTree = this.getOrgTree(orgList, tempTreeList, pathcode, Integer.parseInt(asglevel));
            if(CollectionUtil.isEmpty(orgTree)){
                tree.setChildSelect(null);
            }else{
                tree.setChildSelect(orgTree);
            }
            orgTreeList.add(tree);
        }


        return orgTreeList;
    }


//    @Override
//    public Org selectById(String id) {
//        return orgMapper.selectById(id);
//    }
}
