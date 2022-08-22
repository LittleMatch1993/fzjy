package com.cisdi.transaction.config.exception;

import com.cisdi.transaction.config.base.ResultCode;
import lombok.Getter;

/**
 * @Author: cxh
 * @Description:
 * @Date: 2022/8/22 13:05
 */
@Getter
public class BusinessException extends  RuntimeException{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;


    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(String msg) {
        super(msg);
        this.code = ResultCode.RC999.getCode();
        this.msg = msg;
    }
}
