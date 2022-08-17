package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.domain.dto.BaseDTO;
import com.cisdi.transaction.domain.dto.InvestInfoDTO;
import com.cisdi.transaction.domain.dto.InvestmentDTO;
import com.cisdi.transaction.domain.dto.SubmitDto;
import com.cisdi.transaction.domain.model.BanDealInfo;
import com.cisdi.transaction.domain.model.InvestInfo;
import com.cisdi.transaction.domain.vo.ExportReturnVO;
import com.cisdi.transaction.domain.vo.KVVO;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/3 15:17
 */
public interface InvestInfoService extends IService<InvestInfo> {
    /**
     * 修改状态
     * @param ids
     * @param state
     * @return
     */
    boolean  updateState(List<String> ids, String state);
    /**
     * 批量修改的tips不同相同的值
     * @param kvList
     * @return
     */
    int  updateTips(List<KVVO> kvList);

    /**
     * 批量修改的tips相同的值
     * @param kvList
     * @return
     */
    boolean  updateBathTips(List<KVVO> kvList);

    /**
     * 查询干部姓名 身份证号 社会信用代码数据条数
     * @param name
     * @param cardId
     * @param code
     * @return
     */

    int  countByNameAndCardIdAndCode(String name,String cardId,String code);

    public ResultMsgUtil<String> submitInvestInfo(SubmitDto ids);

    /**
     * 新增
     * @param dto
     */
    void saveInvestInfo(InvestInfoDTO dto);

    /**
     * 覆盖重复数据
     * @param id 以存在的数据id
     * @param dto 需要更新的数据
     */
    void overrideInvestInfo(String id ,InvestInfoDTO dto);


    InvestInfo getRepeatInvestInfo(String name,String cardId,String code);

    /**
     * 编辑
     * @param dto
     */
    void editInvestInfo(InvestInfoDTO dto);

    /**
     * 批量导入新增
     * @param list
     */

    void saveBatchInvestmentInfo(List<InvestmentDTO> list);

    /**
     * 导出功能
     * @return
     */
    List<InvestmentDTO> exportInvestmentExcel(List<String> ids);

    /**
     * 批量导入新增
     * @param list
     */
    void saveBatchInvestmentInfo(List<InvestmentDTO> list, BaseDTO baseDTO, ExportReturnVO exportReturnVO);
}
