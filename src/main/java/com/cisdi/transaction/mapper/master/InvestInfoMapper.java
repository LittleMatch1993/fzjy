package com.cisdi.transaction.mapper.master;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cisdi.transaction.domain.model.InvestInfo;
import com.cisdi.transaction.domain.vo.KVVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/3 16:32
 */
public interface InvestInfoMapper extends BaseMapper<InvestInfo> {


    @Select("<script>  update 69654103_invest_info " +
            "    set  tips=" +
            "    <foreach collection=\"list\" item=\"item\" index=\"index\" " +
            "        separator=\" \" open=\"CASE id\" close=\"end\">" +
            "        when #{item.id} then #{item.name}" +
            "    </foreach>" +
            "    where id in" +
            "    <foreach collection=\"list\" index=\"index\" item=\"item\" " +
            "        separator=\",\" open=\"(\" close=\")\">" +
            "        #{item.id}" +
            "    </foreach></script>")
    public int updateTips(List<KVVO> kvList);


    @Select("<script>    <foreach collection=\"list\" item=\"item\" index=\"index\" open=\"\" close=\"\" separator=\";\">\n" +
            "            update 69654103_invest_info\n" +
            "               set     tips = #{item.name,jdbcType=VARCHAR}\n" +
            "            where id = #{item.id,jdbcType=VARCHAR}\n" +
            "     </foreach></script>")
    public void  updateBatchTips(@Param("list") List<KVVO> kvList);

    @Select("<script>" +
            "update 69654103_invest_info set tips = null " +
            "where id in " +
            "<foreach collection=\"list\" index=\"index\" item=\"item\"  separator=\",\" open=\"(\" close=\")\">" +
            "#{item}" +
            "</foreach></script>")
    public void  cleanBatchTips(@Param("list") List<String> idsList);
}
