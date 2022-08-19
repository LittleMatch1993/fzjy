package com.cisdi.transaction.mapper.master;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.GbOrgInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/1 16:53
 */
public interface GbBasicInfoMapper extends BaseMapper<GbBasicInfo> {
    /**
     * 插入单条
     * @param gb
     * @return
     */
 //   public int insert(GbBasicInfo gb);

    /**
     * 批量插入
     * @param gbs
     * @return
     */
   // public int insertBatch(List<GbBasicInfo> gbs);


//    public List<GbBasicInfo> selectByName(String name);

    /**
     * 根据身份证查询干部的基本信息和组织信息
     * @param ids
     * @return
     */
    @Select("<script>select a.name,a.card_id,a.unit, c.asgorgancode \"unit_code\",  CONVERT(c.asglevel,SIGNED) 'asglevel',c.asgpathnamecode,\n" +
            "            department,\n" +
            "          \n" +
            "            post,post_type,a.allot_type from `69654103_gb_basic_info` a left join  \n" +
            "            (select  asgorgancode,asgorganname,asglevel,asgpathnamecode from `69654103_org`) c \n" +
            "                     \n" +
            "            on c.asgorganname =a.unit where a.card_id in\n" +
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>\n" +
            "  #{item}\n" +
            "</foreach>\n</script>")
    public List<GbOrgInfo> selectByCardIds(@Param("ids") List<String> ids);

    @Select("   SELECT \n" +
            "                DISTINCT t3.*,t5.asgorgancode 'unit_code' \n" +
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
            "                                sort >= 4 \n" +
            "                        ) t4 ON t3.card_id = t4.card_id \n" +
            "                        AND t3.sort = t4.sort \n" +
            "                        INNER JOIN ( SELECT DISTINCT asgorganname,asgorgancode FROM 69654103_org WHERE asgpathnamecode LIKE concat(#{pathNameCode},'%')) t5 ON t5.asgorganname = t3.unit")
    public  List<GbOrgInfo> selectByPathNameCode(String pathNameCode);


    /**
     * 按权限查询
     * @param cardIds 干部身份证id
     * @param pathNameCode 可见部门的部门编码链
     * @return
     */
    @Select("<script>   SELECT \n" +
            "                DISTINCT t3.*,t5.asgorgancode 'unit_code',CONVERT(t5.asglevel,SIGNED) 'asglevel',t5.asgpathnamecode \n" +
            "        FROM \n" +
            "                ( \n" +
            "                        SELECT \n" +
            "                                t1.*, \n" +
            "                                t2.sort \n" +
            "                        FROM \n" +
            "                                69654103_gb_basic_info t1 \n" +
            "                                        LEFT JOIN sys_dict_biz t2 ON t1.post_type = t2.id where t1.card_id in \n" +
            "<foreach collection='list' item='item'  open='(' separator=',' close=')'>\n" +
            "  #{item}\n" +
            "</foreach>\n"+
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
            "                                sort >= 4 \n" +
            "                        ) t4 ON t3.card_id = t4.card_id \n" +
            "                        AND t3.sort = t4.sort \n" +
            "                        INNER JOIN ( SELECT DISTINCT asgorganname,asgorgancode,asglevel,asgpathnamecode FROM 69654103_org WHERE asgpathnamecode LIKE concat(#{pathNameCode},'%')) t5 ON t5.asgorganname = t3.unit </script> ")
    public  List<GbOrgInfo> selectByPathNameCodeAndCardIds(@Param("list") List<String> cardIds,@Param("pathNameCode") String pathNameCode);

    /*@Select("   SELECT \n" +
            "                DISTINCT t3.*,t5.asgorgancode 'unit_code' \n" +
            "        FROM \n" +
            "                ( \n" +
            "                        SELECT \n" +
            "                                t1.*, \n" +
            "                                t2.sort \n" +
            "                        FROM \n" +
            "                                69654103_gb_basic_info t1 \n" +
            "                                        LEFT JOIN sys_dict_biz t2 ON t1.post_type = t2.id where t1.card_id in \n" +
            "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>\n" +
            "  #{item}\n" +
            "</foreach>\n"+
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
            "                                sort >= 4 \n" +
            "                        ) t4 ON t3.card_id = t4.card_id \n" +
            "                        AND t3.sort = t4.sort \n" +
            "                        INNER JOIN ( SELECT DISTINCT asgorganname,asgorgancode FROM 69654103_org ) t5 ON t5.asgorganname = t3.unit")
    public  List<GbOrgInfo> selectAllByCardIds(@Param("ids")List<String> cardIds);*/
    @Select("   SELECT \n" +
            "                DISTINCT t3.*,t5.asgorgancode 'unit_code' \n" +
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
            "                                sort >= 4 \n" +
            "                        ) t4 ON t3.card_id = t4.card_id \n" +
            "                        AND t3.sort = t4.sort \n" +
            "                        INNER JOIN ( SELECT DISTINCT asgorganname,asgorgancode FROM 69654103_org WHERE asgpathnamecode LIKE concat(#{pathNameCode},'%')) t5 ON t5.asgorganname = t3.unit")
    public  List<GbBasicInfo> selectGbBasicInfoByPathNameCodePage(String pathNameCode,Integer pageSize,Integer pageNum);

    @Select(" <script>  SELECT \n" +
            "                DISTINCT t3.* \n" +
            "        FROM \n" +
            "                ( \n" +
            "                        SELECT \n" +
            "                                t1.*, \n" +
            "                                t2.sort \n" +
            "                        FROM \n" +
            "                                69654103_gb_basic_info t1 \n" +
            "                                        LEFT JOIN sys_dict_biz t2 ON t1.post_type = t2.id <when test='name!=null and name!=\"\"'> and t1.name LIKE concat('%',#{name},'%')</when> \n" +
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
            "                                sort >= 4 \n" +
            "                        ) t4 ON t3.card_id = t4.card_id \n" +
            "                        AND t3.sort = t4.sort \n" +
            "                        INNER JOIN ( SELECT DISTINCT asgorganname FROM 69654103_org WHERE asgpathnamecode LIKE concat(#{pathNameCode},'%')) t5 ON t5.asgorganname = t3.unit </script>")
    public  List<GbOrgInfo>  selectByOrgCodeAndGbName(String name,String pathNameCode);
    @Select("  <script> SELECT " +
            "                DISTINCT t3.* " +
            "        FROM " +
            "                ( " +
            "                        SELECT " +
            "                                t1.*, " +
            "                                t2.sort " +
            "                        FROM  " +
            "                                69654103_gb_basic_info t1 " +
            "                                        LEFT JOIN sys_dict_biz t2 ON t1.post_type = t2.id <when test='name!=null and name!=\"\"'> and t1.name LIKE concat('%',#{name},'%')</when> " +
            "                        ) t3 " +
            "                        INNER JOIN ( " +
            "                        SELECT " +
            "                                gbi.card_id, " +
            "                                min( sdb.sort ) sort " +
            "                        FROM " +
            "                                69654103_gb_basic_info gbi " +
            "                                        LEFT JOIN sys_dict_biz sdb ON gbi.post_type = sdb.id \n" +
            "                        GROUP BY " +
            "                                gbi.card_id " +
            "                        HAVING " +
            "                                sort >= 4 " +
            "                        ) t4 ON t3.card_id = t4.card_id " +
            "                        AND t3.sort = t4.sort " +
            "                        INNER JOIN ( SELECT DISTINCT asgorganname FROM 69654103_org WHERE asgpathnamecode LIKE concat(#{pathNameCode},'%')) t5 ON t5.asgorganname = t3.unit limit 200" +
            " </script>")
    public  List<GbOrgInfo>  selectByOrgCodeAndGbNamePage(String name,String pathNameCode);
}
