package com.shiwei.seckill.address.controller;

import com.shiwei.seckill.address.model.AddressSaveReq;
import com.shiwei.seckill.address.service.AddressService;
import com.shiwei.seckill.common.api.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;

@RestController
@RequestMapping("/address")
public class AddressController {
    @Resource
    private AddressService addressService;

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.success(addressService.list());
    }

    @GetMapping("/{addressId}")
    public ApiResponse<?> detail(@PathVariable Long addressId) {
        return ApiResponse.success(addressService.detail(addressId));
    }

    @PostMapping("/save")
    public ApiResponse<?> save(@RequestBody AddressSaveReq req) {
        return ApiResponse.successMessage("地址保存成功", addressService.save(req));
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<?> delete(@PathVariable Long addressId) {
        addressService.delete(addressId);
        return ApiResponse.success(Collections.singletonMap("deleted", true));
    }
}
