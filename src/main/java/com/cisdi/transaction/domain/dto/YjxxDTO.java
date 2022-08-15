package com.cisdi.transaction.domain.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 10:13
 */
@Data
@ApiModel(description = "预警信息实体")
public class YjxxDTO extends BaseDTO {

    private String id;
    // 采购回调ID
    private String yjxxId;
    //预警类型唯一标识
    private String yjlxid;
    //预警方式0:派单1:禁止操作2:预警提示
    private String yjfs;
    //预警级别 0:轻微1:一般2:重大 3：较大
    private String yjjb;
    //招标编码
    private String zbbm;
    //招标编号
    private String zbbh;
    //招标名称
    private String zbmc;
    //招标制单人编码
    private String zbzdrbm;
    //招标制单人名称
    private String zbzdrmc;
    //招标制单人单位编码
    private String zbzdrdwbm;
    //招标制单人单位名称
    private String zbzdrdwmc;
    //预警时间
    private Date yjsj;
    //接收数据时间
    private Date jssjsj;
    //状态 0:处理中1:处理完成 2 已发现 3已派单 4已移交
    private String zt;
    //预警内容
    private String yjnr;
    // 要素
    private String ys;
    //类型:禁业经营-评审专家-围标串标
    private String type;
    // 阈值
    private String waringValue;
    // 体征
    private String sign;
    // 异常原因
    private String errorCause;
    // 处置措施
    private String treatment;
    // 动态字段（table.field）
    private String dynamicField;
    // 动态字段值
    private String dynamicFieldValue;
    // 发送状态 0-未发送 1-已发送
    private String sendStatus;
    // 相关单位
    private String correlationCompany;
    // 输出字段
    private String outputField;
    // 旧版预警内容
    private String yjnrFormer;
}
