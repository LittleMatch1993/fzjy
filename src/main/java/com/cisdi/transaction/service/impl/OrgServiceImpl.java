package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
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
import com.cisdi.transaction.domain.dto.CadreFamilyExportDto;
import com.cisdi.transaction.domain.dto.InstitutionalFrameworkDTO;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.domain.model.SysDictBiz;
import com.cisdi.transaction.domain.vo.InstitutionalFrameworkExcelVO;
import com.cisdi.transaction.mapper.master.OrgMapper;
import com.cisdi.transaction.service.OrgService;
import com.cisdi.transaction.service.SysDictBizService;
import com.cisdi.transaction.util.ThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.BindException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
        //Todo 什么方式检测唯一性
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
        queryWrapper.lambda().eq(Org::getAsgDate, asgDate);
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
        //换成配置文件
        obj.put("app_id", "sphy");
        obj.put("table_name", "");
        if(StrUtil.isEmpty(stringDate)){
            stringDate = "18000101";
        }
        obj.put("stringDate", "18000101");
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
                for (int i = 0; i < data.size(); i++) {
                    JSONObject josnObj = data.getJSONObject(i);
                    Org org = JSONObject.parseObject(josnObj.toString(), Org.class);
                    String asgId = josnObj.getString("asgId");
                    org.setId(asgId);
                    org.setCreateTime(DateUtil.date());
                    org.setUpdateTime(DateUtil.date());
                    orgs.add(org);
                }

            }
            if (CollectionUtil.isNotEmpty(orgs)) {
                List<SysDictBiz> dictList = sysDictBizService.selectList();
                orgs = this.repalceDictId(orgs,dictList);
                this.remove(null);
                this.saveBatch(orgs);
                System.out.println("service执行组织同步定时任务成功完成");
                /*boolean removeB = false;
                if("18000101".equals(stringDate)){//全部更新
                    removeB = this.remove(null);
                }else{//增量更新
                    removeB = this.removeByAsgDate(stringDate);
                }
                if(removeB){

                }*/
            }
            System.out.println("service执行组织同步定时任务完成");
        }else{
            System.out.println("没有成功返回数据");
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
    public List<Org> selectChildOrgByPathnamecode(String pathnamecode) {
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode);
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<Org> selectByName(List<String> names,String orgCode) {
        List<Org> list = new ArrayList<>();
        //Object orgCode = ThreadLocalUtils.get("orgCode");

        if(StrUtil.isEmpty(orgCode)){
            return list;
        }
        Org org = this.selectByOrgancode(orgCode.toString());
        if(Objects.isNull(org)){
            return list;
        }
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        String asglevel = org.getAsglevel();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){ //看所有
            if(CollectionUtil.isNotEmpty(names)){
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i);
                    if(i==names.size()-1){
                        queryWrapper.lambda().like(Org::getAsgorganname, name+"%公司");
                    }else{
                        queryWrapper.lambda().like(Org::getAsgorganname, name+"%公司").or();
                    }
                }
            }else{
                queryWrapper.lambda().likeLeft(Org::getAsgorganname, "公司").last(SqlConstant.ONE_SQL_YB);
            }
        }else{
            String pathnamecode = org.getAsgpathnamecode();
            if(CollectionUtil.isNotEmpty(names)){
                //queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode);
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i);
                    if(i==names.size()-1){
                        if(name.endsWith("公司")||name.endsWith("公")){
                            queryWrapper.lambda().like(Org::getAsgorganname, name);
                        }else{
                            queryWrapper.lambda().like(Org::getAsgorganname, name+"%公司");
                        }

                    }else{
                        if(name.endsWith("公司")||name.endsWith("公")){
                            queryWrapper.lambda().like(Org::getAsgorganname, name).or();
                        }else{
                            queryWrapper.lambda().like(Org::getAsgorganname, name+"%公司").or();
                        }
                        //queryWrapper.lambda().like(Org::getAsgorganname, name+"%公司").or();
                    }
                }
                queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode);
               // queryWrapper.lambda().like(Org::getAsgorganname, name);
            }else{
                queryWrapper.lambda().likeLeft(Org::getAsgorganname, "公司").likeRight(Org::getAsgpathnamecode,pathnamecode).last(SqlConstant.ONE_SQL_YB);
            }
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
        Org org = this.selectByOrgancode(orgCode.toString());
        if(Objects.isNull(org)){
            return list;
        }
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();
        String asglevel = org.getAsglevel();
        if(StrUtil.isNotEmpty(asglevel)&&asglevel.equals("0")){ //看所有
            if(StrUtil.isNotEmpty(name)){
                if(name.endsWith("公司")||name.endsWith("公")){
                    queryWrapper.lambda().like(Org::getAsgorganname, name);
                }else{
                    queryWrapper.lambda().like(Org::getAsgorganname, name+"%公司");
                }

            }else{
                queryWrapper.lambda().likeLeft(Org::getAsgorganname, "公司").last(SqlConstant.ONE_SQL_YB);;
            }
        }else{
            String pathnamecode = org.getAsgpathnamecode();
            if(StrUtil.isNotEmpty(name)){
                queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode);
                if(name.endsWith("公司")||name.endsWith("公")){
                    queryWrapper.lambda().like(Org::getAsgorganname, name);
                }else{
                    queryWrapper.lambda().like(Org::getAsgorganname, name+"%公司");
                }
            }else{
                queryWrapper.lambda().likeLeft(Org::getAsgorganname, "公司");
                queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode).last(SqlConstant.ONE_SQL_YB);
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
            s1 = "0".equals(s1)?"否":"是";
            s2 = "0".equals(s2)?"否":"是";
            //字典对应项
            String asgleadfg = sysDictBizService.getDictId(s1, dictList);
            String asglead = sysDictBizService.getDictId(s2, dictList);
            dto.setAsglead(asglead);
            dto.setAsgleadfg(asgleadfg);

        });
        return list;
    }

    @Override
    public List<InstitutionalFrameworkExcelVO> export(CadreFamilyExportDto dto){
        return this.lambdaQuery().like(StringUtils.isNotBlank(dto.getAsgorganname()),Org::getAsgorganname,dto.getAsgorganname())
                .like(StringUtils.isNotBlank(dto.getAsgorgancode()),Org::getAsgorgancode,dto.getAsgorgancode())
                .eq(StringUtils.isNotBlank(dto.getAsglead()),Org::getAsglead,dto.getAsglead())
                .eq(StringUtils.isNotBlank(dto.getAsgleadfg()),Org::getAsgleadfg,dto.getAsgleadfg())
                .apply(AuthSqlUtil.getAuthSqlByTableNameAndOrgCode(ModelConstant.ORG,dto.getOrgCode()))
                .orderByDesc(Org::getUpdateTime)
                .list().stream().map(t -> {
            InstitutionalFrameworkExcelVO vo = new InstitutionalFrameworkExcelVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).collect(Collectors.toList());
    }


//    @Override
//    public Org selectById(String id) {
//        return orgMapper.selectById(id);
//    }
}
