package com.cisdi.transaction.mapper.three;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cisdi.transaction.domain.model.GbBasicInfo;
import com.cisdi.transaction.domain.model.GbBasicInfoThree;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/7 22:59
 */
public interface GbBasicInfoThreeMapper extends BaseMapper<GbBasicInfoThree> {

    @Select(" select name,card_id,unit,department,post, post_type 'old_post_type'," +
            "case " +
            "when post_type='党组管干部班子正职' then '党组管干部正职'" +
            "when post_type='党组管干部班子正职（一把手）' then '党组管干部正职'" +
            "when post_type='党组管干部班子副职' then '党组管干部副职'" +
            "when post_type='党组管干部其他' then '党组管干部其他'" +
            "when post_type like '%总部处长%' then '总部处长'" +
            "when post_type='党委管干部班子正职（一把手）' then '党委管干部正职'" +
            "when post_type='党委管干部班子副职' then '党委管干部副职'" +
            "when post_type='党委管干部班子正职' then '党委管干部正职'" +
            "when post_type='党委管干部其他' then '党委管干部其他'" +
            "else null end 'post_type'" +
            " from (" +
            "SELECT " +
            "  a1.A0101 'name', " +
            " a1.a0184 'card_id', " +
            " case " +
            " when locate('公司',a2.ZDYXB0101)>0 then left( a2.ZDYXB0101,locate('公司', a2.ZDYXB0101)+1)  end as 'unit'," +
            "  case " +
            " when locate('公司',a2.ZDYXB0101)>0 then substring( a2.ZDYXB0101,locate('公司', a2.ZDYXB0101)+2) else a2.ZDYXB0101 end as 'department'," +
            " ( IFNULL(( SELECT zb8.DMCPT FROM ZB08 zb8 WHERE zb8.DMCOD = a2.A0215B ),A0217)) 'post', " +
            " CONCAT(( SELECT hr3.DMCPT FROM hr03 hr3 WHERE hr3.DMCOD = a1.ZDYXA0110 ),( SELECT HR973.DMCPT FROM HR973 WHERE HR973.INPFRQ  = a1.A0219 )) 'post_type' " +
            "FROM " +
            " ( SELECT A00, A0101, a0184, ZDYXA0110,A0219 FROM A01) a1 " +
            " LEFT JOIN a02 a2 ON a1.A00 = a2.A00  AND a2.A0255 <> 0 " +
            " ) a where a.post_type!='集团领导班子正职（一把手）'and a.post_type!='集团领导班子正职' and a.post_type!='集团领导班子副职' and a.post_type!='集团领导其他'")
    public List<GbBasicInfoThree> selectGbBasicInfo();

    @Select("SELECT\n" +
            "  a1.A0101 'name',\n" +
            " a1.a0184 'card_id',\n" +
            " ( a2.ZDYXB0101 ) 'unit',\n" +
            " ( SELECT zb8.DMCPT FROM ZB08 zb8 WHERE zb8.DMCOD = a2.A0215B ) 'post',\n" +
            " CONCAT(( SELECT hr3.DMCPT FROM hr03 hr3 WHERE hr3.DMCOD = a1.ZDYXA0110 ),( SELECT HR973.DMCPT FROM HR973 WHERE HR973.INPFRQ  = a1.A0219 )) 'post_type'\n" +
            "FROM\n" +
            " ( SELECT A00, A0101, a0184, ZDYXA0110,A0219 FROM A01) a1\n" +
            " LEFT JOIN a02 a2 ON a1.A00 = a2.A00 AND a2.a0255 <> 0 ")
    public List<GbBasicInfoThree> selectOldGbBasicInfo();


}
