package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.utils.HttpUtils;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.domain.dto.InstitutionalFrameworkDTO;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.model.Org;
import com.cisdi.transaction.domain.model.SysDictBiz;
import com.cisdi.transaction.domain.vo.InstitutionalFrameworkExcelVO;
import com.cisdi.transaction.mapper.master.OrgMapper;
import com.cisdi.transaction.service.OrgService;
import com.cisdi.transaction.service.SysDictBizService;
import com.cisdi.transaction.util.ThreadLocalUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.BindException;
import java.util.ArrayList;
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
    public void syncDa() {
        JSONObject obj = new JSONObject();

        //换成配置文件
        obj.put("app_id", "sphy");
        obj.put("table_name", "");
        obj.put("stringDate", "18000101");
        obj.put("condition", "1=1");
        obj.put("secretKey", "50857140b5b84ddeeef5f62709b32fac");
        String result = HttpUtils.sendPostOfAuth(url, obj);
        JSONObject jb = JSONObject.parseObject(result);
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
            this.remove(null);
            List<SysDictBiz> dictList = sysDictBizService.selectList();
            orgs = this.repalceDictId(orgs,dictList);
            this.saveBatch(orgs);
        }
        System.out.println("service执行组织同步定时任务完成");
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
    public List<Org> selectByName(String name,String orgCode) {
        List<Org> list = new ArrayList<>();
        //Object orgCode = ThreadLocalUtils.get("orgCode");
      /*  if(StrUtil.isEmpty(orgCode)){
            return list;
        }
        Org org = this.selectByOrgancode(orgCode.toString());
        if(Objects.isNull(org)){
            return list;
        }
        String pathnamecode = org.getAsgpathnamecode();
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();

        if(StrUtil.isNotEmpty(name)){
            queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode);
            queryWrapper.lambda().like(Org::getAsgorganname, name);
        }else{
            queryWrapper.lambda().likeRight(Org::getAsgpathnamecode,pathnamecode).last(SqlConstant.ONE_SQL_YB);
        }*/
        QueryWrapper<Org> queryWrapper = new QueryWrapper<>();

        if(StrUtil.isNotEmpty(name)){
            queryWrapper.lambda().like(Org::getAsgorganname, name);
        }else{
            queryWrapper.lambda().likeRight(Org::getAsgorganname, name).last(SqlConstant.ONE_SQL_YB);
        }
        return this.baseMapper.selectList(queryWrapper);
    }

    private List<Org>  repalceDictId(List<Org> list, List<SysDictBiz> dictList){
        list.parallelStream().forEach(dto->{
            //字典对应项
            String asgleadfg = sysDictBizService.getDictId(dto.getAsgleadfg(), dictList);
            String asglead = sysDictBizService.getDictId(dto.getAsglead(), dictList);
            dto.setAsglead(asgleadfg);
            dto.setAsgleadfg(asgleadfg);

        });
        return list;
    }


//    @Override
//    public Org selectById(String id) {
//        return orgMapper.selectById(id);
//    }
}
