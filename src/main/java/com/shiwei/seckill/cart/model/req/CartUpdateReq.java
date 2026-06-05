package com.shiwei.seckill.cart.model.req;

import lombok.Data;

@Data
public class CartUpdateReq {
    private Long cartId;
    private Integer quantity;
}
