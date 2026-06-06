package com.shiwei.seckill.product.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.product.model.Product;
import com.shiwei.seckill.product.model.ProductPageResult;
import com.shiwei.seckill.product.model.ProductSaveReq;
import com.shiwei.seckill.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Resource
    private ProductService productService;

    @GetMapping("/list")
    public ApiResponse<List<Product>> list() {
        return ApiResponse.success(productService.list());
    }

    @GetMapping("/page")
    public ApiResponse<ProductPageResult> page(Long lastId, Integer size) {
        return ApiResponse.success(productService.page(lastId, size));
    }

    @GetMapping("/{productId}")
    public ApiResponse<Product> detail(@PathVariable Long productId) {
        Product product = productService.detail(productId);
        return ApiResponse.success(product == null ? (productService.list().isEmpty() ? null : productService.list().get(0)) : product);
    }

    @PostMapping("/admin/save")
    public ApiResponse<Product> save(@RequestBody ProductSaveReq req) {
        return ApiResponse.successMessage("商品保存成功", productService.save(req));
    }
}

