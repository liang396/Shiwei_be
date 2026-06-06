package com.shiwei.seckill.common.api;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.message = "ok";
        res.data = data;
        return res;
    }

    public static <T> ApiResponse<T> successMessage(String message, T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.message = message;
        res.data = data;
        return res;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = false;
        res.message = message;
        return res;
    }
}

