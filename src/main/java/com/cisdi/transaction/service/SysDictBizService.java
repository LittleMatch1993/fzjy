package com.cisdi.transaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cisdi.transaction.domain.model.SysDictBiz;

import java.util.List;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/9 11:25
 */
public interface SysDictBizService extends IService<SysDictBiz> {

    /**
     * 查询有效数据
     * @return
     */
    public List<SysDictBiz> selectList();


    /**
     * 根据id查询
     * <pre>
     *     在循环中使用，可以避免重复的在数据库中查找。
     *     只需要在调用当前方法之前获取有效的数据字典列表，
     *     然后作为参数传入。
     * </pre>
     * @param id
     * @param dictList 有效的字典列表
     * @return
     */
    public SysDictBiz selectById(String id,List<SysDictBiz> dictList);

    /**
     * 根据id返回字典值
     * <pre>
     *     在循环中使用，可以避免重复的在数据库中查找。
     *     只需要在调用当前方法之前获取有效的数据字典列表，
     *     然后作为参数传入。
     * </pre>
     * @param id
     * @param dictList 有效的字典列表
     * @return
     */
    public String getDictValue(String id,List<SysDictBiz> dictList);
    public String getDictId(String value,List<SysDictBiz> dictList);

    String getEnterpriseStateDictId(String enterpriseState, List<SysDictBiz> dictList);

    String getOperatStateDictId(String operatState, List<SysDictBiz> dictList);
}
