package com.cisdi.transaction.config.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.metadata.CellData;
import com.cisdi.transaction.config.excel.ExcelImportValid;
import com.cisdi.transaction.config.excel.ExceptionCustom;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.BaseDTO;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.vo.ExportReturnMessageVO;
import com.cisdi.transaction.domain.vo.ExportReturnVO;
import com.cisdi.transaction.service.InvestInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/6 10:36
 */
@Slf4j
public class ImportInvestmentExcelListener extends AnalysisEventListener<InvestmentDTO> {


    /**
     * 每隔 1000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 1000;

    // 存款的list对象
    List<InvestmentDTO> list = new ArrayList<InvestmentDTO>();

    // ImportInvestmentExcelListener  没有交给Spring进行管理，所有不能在此方法使用    @Autowired 注入
    //  不能使用数据库等操作,可以使用setter和getter方法，在调用方传入
    private InvestInfoService investInfoService;

    private ExportReturnVO exportReturnVO;

    private int i=4;
    public ImportInvestmentExcelListener() {
    }

    private BaseDTO baseDTO;

    public ImportInvestmentExcelListener(InvestInfoService investInfoService, BaseDTO baseDTO, ExportReturnVO exportReturnVO) {
        this.investInfoService = investInfoService;
        this.baseDTO=baseDTO;
        this.exportReturnVO=exportReturnVO;
    }

    private List<String> columns= Arrays.asList(
            "干部姓名","身份证号","干部类型","工作单位","现任职务","职务层次","标签","职级","人员类别","政治面貌","在职状态","家人姓名","称谓","统一社会信用代码/注册号","企业或其他市场主体名称","成立日期","经营范围","注册地（国家）","注册地（省）","注册地（市）","经营地","企业或其他市场主体类型","注册资本（金）或资金数额（出资额）（人民币万元）","企业状态","是否为股东（合伙人、所有人）","个人认缴出资额或个人出资额（人民币万元）","个人认缴出资比例或个人出资比例（%）","投资时间","是否担任高级职务","所担任的高级职务名称","担任高级职务的开始时间","担任高级职务的结束时间","该企业或其他市场主体是否与报告人所在单位（系统）直接发生过商品、劳务、服务等经济关系","备注","填报类型","年度","有无此类情况"
    );

    @Override
    public void invokeHead(Map<Integer, CellData> var1, AnalysisContext var2){
        if (var2.readRowHolder().getRowIndex()==2){
            if (CollectionUtils.isEmpty(var1)){
                throw new ExcelAnalysisException("无内容");
            }
            for (CellData value : var1.values()) {
                if (Objects.nonNull(value)&& StringUtils.isNotBlank(value.toString())&&!columns.contains(value.toString())){
                    throw new ExcelAnalysisException("模板不包含此内容："+value);
                }
            }
        }
    }

    // 读取Excel内容，一行一行的读
    @Override
    public void invoke(InvestmentDTO dto, AnalysisContext analysisContext) {
        try {

            dto.setColumnNumber(i++);
            dto.setIsSituation("有此类情况");

            //不是股东（合伙人、所有人）时将个人认缴出资额或个人出资额、个人认缴出资比例或个人出资比例、投资时间置空
            if (SystemConstant.WHETHER_NO.equals(dto.getShareholder())){
                dto.setPersonalCapital(null);
                dto.setPersonalRatio(null);
                dto.setInvestTime(null);
            }
            //未担任高级职务时将所担任的高级职务名称、担任高级职务的时间置空
            if (SystemConstant.WHETHER_NO.equals(dto.getSeniorPosition())){
                dto.setSeniorPositionName(null);
                dto.setSeniorPositionEndTime(null);
                dto.setSeniorPositionStartTime(null);
            }

            //通用方法数据校验
            ExcelImportValid.valid(dto);
        } catch (ExceptionCustom e) {
//            System.out.println(e.getMessage());
            //在easyExcel监听器中抛出业务异常
            dto.setMessage(e.getMessage());
            ExportReturnMessageVO exportReturnMessageVO=new ExportReturnMessageVO();
            exportReturnMessageVO.setColumn(i-1);
            exportReturnMessageVO.setMessage(e.getMessage());
            exportReturnVO.getFailMessage().add(exportReturnMessageVO);
            exportReturnVO.setFailNumber(exportReturnVO.getFailNumber()+1);
//            throw new ExcelAnalysisException(e.getMessage());
        }
        list.add(dto);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (list.size() >= BATCH_COUNT) {
            saveData();
            //存储完成清理 list
            list.clear();
        }
    }

    // 所有数据解析完成了 都会来调用
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
        log.info(list.toString());
        if (list.size()>0){
            int i=4;
            for (InvestmentDTO investmentDTO : list) {
                investmentDTO.setColumnNumber(i++);
            }
            investInfoService.saveBatchInvestmentInfo(list,baseDTO,exportReturnVO);
        }
        log.info("存储数据库成功！");
    }

}
