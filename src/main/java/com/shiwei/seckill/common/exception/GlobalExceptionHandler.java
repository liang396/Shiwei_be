package com.shiwei.seckill.common.exception;

import com.shiwei.seckill.common.api.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ApiResponse<?> handleBiz(BizException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleOther(Exception ex) {
        return ApiResponse.fail(ex.getMessage());
    }
}
