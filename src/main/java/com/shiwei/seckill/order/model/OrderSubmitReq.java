package com.shiwei.seckill.order.model;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitReq {
    @NotNull(message = "地址ID不能为空")
    private Long addressId;
    @NotBlank(message = "收货人不能为空")
    private String consignee;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;
    @NotBlank(message = "地址不能为空")
    private String address;
    private Long couponId;
    private String couponTitle;
    @NotNull(message = "商品金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "商品金额必须大于0")
    private BigDecimal goodsAmount;
    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.0", message = "优惠金额不能小于0")
    private BigDecimal discountAmount;
    @NotNull(message = "实付金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "实付金额必须大于0")
    private BigDecimal payAmount;
    @Valid
    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemPayload> items;
}
