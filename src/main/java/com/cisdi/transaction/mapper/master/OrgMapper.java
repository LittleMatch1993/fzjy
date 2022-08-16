package com.cisdi.transaction.mapper.master;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cisdi.transaction.domain.model.Org;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.util.List;

/** 组织机构
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/1 18:06
 */
public interface OrgMapper extends BaseMapper<Org> {
//    public  int removeAll();
//
//    public List<Org> insertBatch(List<Org> orgs);
//
//    public Org selectById(String id);

    @Select("<script>(select asgpathname from 69654103_org where asgorgancode=#{code}) b where\n" +
            "         a.asgpathname = concat(b.asgpathname,\"-\",#{name})</script>")
    public List<Org> getOrgByUnitCodeAndDepartmentName(@Param("code")String code,String name);
}
