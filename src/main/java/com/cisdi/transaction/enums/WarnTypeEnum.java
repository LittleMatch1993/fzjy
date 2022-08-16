package com.cisdi.transaction.enums;

/**
 * @author tangguilin
 * @date 2022/8/16 14:31
 * @description
 */
public enum WarnTypeEnum {

    JYJY01("JYJY01", "禁止交易"),
    JYJY06("JYJY06", "招标信息"),
    JYJY07("JYJY07", "中标信息"),
    JYJY08("JYJY08", "签订合同信息");

    private String code;
    private String name;

    public String getCode() {
        return code;
    }
    public String getName() {
        return name;
    }

    WarnTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
