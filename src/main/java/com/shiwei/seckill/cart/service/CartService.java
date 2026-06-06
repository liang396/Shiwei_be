package com.shiwei.seckill.cart.service;

import com.shiwei.seckill.cart.model.CartItem;
import com.shiwei.seckill.cart.model.req.CartAddReq;
import com.shiwei.seckill.cart.model.req.CartCheckReq;
import com.shiwei.seckill.cart.model.req.CartRemoveReq;
import com.shiwei.seckill.cart.model.req.CartUpdateReq;

import java.util.List;

public interface CartService {
    List<CartItem> listItems();

    List<CartItem> addItem(CartAddReq req);

    List<CartItem> updateItem(CartUpdateReq req);

    List<CartItem> removeItem(CartRemoveReq req);

    List<CartItem> checkItem(CartCheckReq req);
}

