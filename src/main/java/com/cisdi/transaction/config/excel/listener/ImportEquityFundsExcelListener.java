package com.cisdi.transaction.config.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.util.CollectionUtils;
import com.cisdi.transaction.config.excel.ExcelImportValid;
import com.cisdi.transaction.config.excel.ExceptionCustom;
import com.cisdi.transaction.domain.dto.EquityFundsDTO;
import com.cisdi.transaction.service.PrivateEquityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/6 13:02
 */
@Slf4j
public class ImportEquityFundsExcelListener extends AnalysisEventListener<EquityFundsDTO> {

    /**
     * 每隔 1000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 1000;

    // 存款的list对象
    List<EquityFundsDTO> list = new ArrayList<EquityFundsDTO>();

    private PrivateEquityService privateEquityService;

    public ImportEquityFundsExcelListener() {
    }
    public ImportEquityFundsExcelListener(PrivateEquityService privateEquityService) {
        this.privateEquityService = privateEquityService;
    }
    private List<String> columns= Arrays.asList(
            "13-3投资私募股权投资基金或者担任高级职务的情况",
            "干部基本信息",
            "有关事项信息",
            "干部姓名","身份证号","工作单位","现任职务","职务层次","标签","职级","人员类别","政治面貌","在职状态","姓名","称谓","投资的私募股权投资基金产品名称","编码","基金总实缴金额（人民币万元）","个人实缴金额（人民币万元）","基金投向","基金合同签署日","基金合同约定的到期日","私募股权投资基金管理人名称","登记编号","是否为该基金管理人的实际控制人","是否为该基金管理人的股东（合伙人）","认缴金额（人民币万元）","认缴比例（%）","认缴时间","是否担任该基金管理人高级职务","所担任的高级职务名称","担任高级职务的开始时间","担任高级职务的结束时间","基金管理人的经营范围","是否与报告人所在单位（系统）直接发生过经济关系","备注","填报类型","年度","有无此类情况"
    );

    @Override
    public void invokeHead(Map<Integer, CellData> var1, AnalysisContext var2){
        if (!CollectionUtils.isEmpty(var1)){
            for (CellData value : var1.values()) {
                if (Objects.nonNull(value)&& StringUtils.isNotBlank(value.toString())&&!columns.contains(value.toString())){
                    throw new ExcelAnalysisException("模板不包含此字段");
                }
            }
        }
    }

    @Override
    public void invoke(EquityFundsDTO dto, AnalysisContext analysisContext) {
        try {
            //通用方法数据校验
            ExcelImportValid.valid(dto);
        }catch (ExceptionCustom e){
            //在easyExcel监听器中抛出业务异常
            throw new ExcelAnalysisException(e.getMessage());
        }
        list.add(dto);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (list.size() >= BATCH_COUNT) {
            saveData();
            //存储完成清理 list
            list.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        //这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        log.info("所有数据解析完成！");
    }
    /**
     * 存储数据
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", list.size());
        if (list.size()>0){
            privateEquityService.saveBatchInvestmentInfo(list);
        }
        log.info("存储数据库成功！");
    }
}
