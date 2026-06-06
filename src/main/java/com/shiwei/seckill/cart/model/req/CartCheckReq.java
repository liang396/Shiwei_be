package com.shiwei.seckill.cart.model.req;

import lombok.Data;

@Data
public class CartCheckReq {
    private Long cartId;
    private Boolean checked;
}

