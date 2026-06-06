package com.shiwei.seckill.cart.controller;

import com.shiwei.seckill.cart.model.req.CartAddReq;
import com.shiwei.seckill.cart.model.req.CartCheckReq;
import com.shiwei.seckill.cart.model.req.CartRemoveReq;
import com.shiwei.seckill.cart.model.req.CartUpdateReq;
import com.shiwei.seckill.cart.service.CartService;
import com.shiwei.seckill.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Resource
    private CartService cartService;

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.success(cartService.listItems());
    }

    @PostMapping("/add")
    public ApiResponse<?> add(@RequestBody CartAddReq req) {
        return ApiResponse.successMessage("加入购物车成功", cartService.addItem(req));
    }

    @PostMapping("/update")
    public ApiResponse<?> update(@RequestBody CartUpdateReq req) {
        return ApiResponse.success(cartService.updateItem(req));
    }

    @PostMapping("/remove")
    public ApiResponse<?> remove(@RequestBody CartRemoveReq req) {
        return ApiResponse.success(cartService.removeItem(req));
    }

    @PostMapping("/check")
    public ApiResponse<?> check(@RequestBody CartCheckReq req) {
        return ApiResponse.success(cartService.checkItem(req));
    }
}

