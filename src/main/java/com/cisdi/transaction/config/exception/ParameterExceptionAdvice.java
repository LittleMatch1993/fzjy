package com.cisdi.transaction.config.exception;

import com.cisdi.transaction.config.base.ResultCode;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/3 9:45
 */
@Slf4j
@RestControllerAdvice
@Order(-1)
public class ParameterExceptionAdvice {

    /**
     * 处理请求参数格式错误 @RequestParam上validate失败后抛出的异常是javax.validation.ConstraintViolationException
     *
     * @param
     * @return
     */
    @ResponseBody
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResultMsgUtil constraintViolationExceptionHandler(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
        return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "ConstraintViolationException，输入数据异常" + message);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResultMsgUtil handleVailException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        return ResultMsgUtil.failure(ResultCode.RC999.getCode(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
    }

    /**
     * 处理Get请求,使用@Valid 验证路径中请求实体校验失败后抛出的异常，详情继续往下看代码
     *
     * @param bindException
     * @return ApiResult
     */

    @ExceptionHandler(BindException.class)
    public ResultMsgUtil bindExceptionHandler(BindException bindException) {
        HashMap<String, String> errorMsg = new HashMap();

        for (FieldError fieldError : bindException.getBindingResult().getFieldErrors()) {
            errorMsg.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "BindException，输入数据异常" + errorMsg);
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResultMsgUtil globalException(Exception exception) {
        exception.printStackTrace();
        return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "程序错误：" + exception.getMessage(), null);
    }
    @ResponseBody
    @ExceptionHandler(BusinessException.class)
    public ResultMsgUtil businessException(BusinessException exception) {
        exception.printStackTrace();
        return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "数据处理错误：" + exception.getMessage(), null);
    }

}
