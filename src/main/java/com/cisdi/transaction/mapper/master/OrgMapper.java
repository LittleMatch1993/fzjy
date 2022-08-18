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

    @Select("<script> SELECT \n" +
            "                DISTINCT t3.card_id \n" +
            "        FROM \n" +
            "                ( \n" +
            "                        SELECT \n" +
            "                                t1.*, \n" +
            "                                t2.sort \n" +
            "                        FROM \n" +
            "                                69654103_gb_basic_info t1 \n" +
            "                                        LEFT JOIN sys_dict_biz t2 ON t1.post_type = t2.id \n" +
            "                        ) t3 \n" +
            "                        INNER JOIN ( \n" +
            "                        SELECT \n" +
            "                                gbi.card_id, \n" +
            "                                min( sdb.sort ) sort \n" +
            "                        FROM \n" +
            "                                69654103_gb_basic_info gbi \n" +
            "                                        LEFT JOIN sys_dict_biz sdb ON gbi.post_type = sdb.id \n" +
            "                        GROUP BY \n" +
            "                                gbi.card_id \n" +
            "                        HAVING \n" +
            "                                sort &gt;= 4 \n" +
            "                        ) t4 ON t3.card_id = t4.card_id \n" +
            "                        AND t3.sort = t4.sort \n" +
            "                        INNER JOIN ( SELECT DISTINCT asgorganname FROM 69654103_org WHERE asgpathnamecode LIKE concat(#{codePath},'%') ) t5 ON t5.asgorganname = t3.unit </script>")
    List<String> getCardIdsByAsgpathnamecode(@Param("codePath") String codePath);
}
