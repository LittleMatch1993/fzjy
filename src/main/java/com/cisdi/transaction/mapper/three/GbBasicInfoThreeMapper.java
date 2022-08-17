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

    @Select("select name,card_id,unit,department,post,\n" +
            "case \n" +
            "when post_type='党委管干部班子正职（一把手）' then '党委管干部正职'\n" +
            "when post_type='党组管干部正职' then '党委管干部正职'\n" +
            "when post_type='集团总部处长班子正职' then '总部处长'\n" +
            "when post_type='党委管干部副职' then '党委管干部副职'\n" +
            "when post_type='党委管干部班子正职' then '党委管干部正职'\n" +
            "when post_type='党组管干部班子正职（一把手）' then '党委管干部正职'\n" +
            "when post_type='党委管干部正职' then '党委管干部正职'\n" +
            "when post_type='党组管干部副职' then '党组管干部副职'\n" +
            "when post_type='党组管干部班子正职' then '党组管干部正职'\n" +
            "else null end 'post_type'\n" +
            " from (\n" +
            "SELECT \n" +
            "  a1.A0101 \"name\", \n" +
            " a1.a0184 \"card_id\", \n" +
            " case \n" +
            " \twhen locate(\"公司\",a2.ZDYXB0101)>0 then left( a2.ZDYXB0101,locate(\"公司\", a2.ZDYXB0101)+1)  end as \"unit\",\n" +
            "  case \n" +
            " \twhen locate(\"公司\",a2.ZDYXB0101)>0 then substring( a2.ZDYXB0101,locate(\"公司\", a2.ZDYXB0101)+2) else a2.ZDYXB0101 end as \"department\",\n" +
            "\t  \n" +
            " ( SELECT zb8.DMCPT FROM ZB08 zb8 WHERE zb8.DMCOD = a2.A0215B ) \"post\", \n" +
            " CONCAT(( SELECT hr3.DMCPT FROM hr03 hr3 WHERE hr3.DMCOD = a1.ZDYXA0110 ),( SELECT z42.DMCPT FROM zb42 z42 WHERE z42.DMCOD = a2.A0219 )) \"post_type\"\n" +
            "FROM \n" +
            " ( SELECT A00, A0101, a0184, ZDYXA0110 FROM A01) a1 \n" +
            " LEFT JOIN a02 a2 ON a1.A00 = a2.A00  \n" +
            " ) a where a.post_type!='集团领导班子正职(一把手)'or a.post_type!='集团领导正职' or a.post_type!='集团领导副职'")
    public List<GbBasicInfoThree> selectGbBasicInfo();
}
