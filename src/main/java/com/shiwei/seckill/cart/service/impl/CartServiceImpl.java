package com.shiwei.seckill.cart.service.impl;

import com.shiwei.seckill.cart.model.CartItem;
import com.shiwei.seckill.cart.model.req.CartAddReq;
import com.shiwei.seckill.cart.model.req.CartCheckReq;
import com.shiwei.seckill.cart.model.req.CartRemoveReq;
import com.shiwei.seckill.cart.model.req.CartUpdateReq;
import com.shiwei.seckill.cart.service.CartService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CartServiceImpl implements CartService {
    private final CopyOnWriteArrayList<CartItem> cartItems = new CopyOnWriteArrayList<>();
    private final AtomicLong cartIdGenerator = new AtomicLong(1L);

    @Override
    public List<CartItem> listItems() {
        return snapshot();
    }

    @Override
    public List<CartItem> addItem(CartAddReq req) {
        CartItem existing = cartItems.stream()
            .filter(item -> item.getProductId() != null && item.getProductId().equals(req.getProductId()))
            .findFirst()
            .orElse(null);

        int quantity = req.getQuantity() == null || req.getQuantity() < 1 ? 1 : req.getQuantity();

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            return snapshot();
        }

        CartItem item = new CartItem();
        item.setCartId(cartIdGenerator.getAndIncrement());
        item.setProductId(req.getProductId());
        item.setProductName(req.getProductName());
        item.setProductImage(req.getProductImage());
        item.setPrice(req.getPrice() == null ? BigDecimal.ZERO : req.getPrice());
        item.setQuantity(quantity);
        item.setChecked(true);
        cartItems.add(item);
        return snapshot();
    }

    @Override
    public List<CartItem> updateItem(CartUpdateReq req) {
        cartItems.stream()
            .filter(item -> item.getCartId().equals(req.getCartId()))
            .findFirst()
            .ifPresent(item -> item.setQuantity(Math.max(1, req.getQuantity() == null ? 1 : req.getQuantity())));
        return snapshot();
    }

    @Override
    public List<CartItem> removeItem(CartRemoveReq req) {
        cartItems.removeIf(item -> item.getCartId().equals(req.getCartId()));
        return snapshot();
    }

    @Override
    public List<CartItem> checkItem(CartCheckReq req) {
        if (req.getCartId() == null || req.getCartId() == 0L) {
            boolean checked = req.getChecked() != null && req.getChecked();
            cartItems.forEach(item -> item.setChecked(checked));
            return snapshot();
        }

        cartItems.stream()
            .filter(item -> item.getCartId().equals(req.getCartId()))
            .findFirst()
            .ifPresent(item -> item.setChecked(req.getChecked() != null && req.getChecked()));
        return snapshot();
    }

    private List<CartItem> snapshot() {
        List<CartItem> list = new ArrayList<>(cartItems);
        list.sort(Comparator.comparing(CartItem::getCartId));
        return list;
    }
}
