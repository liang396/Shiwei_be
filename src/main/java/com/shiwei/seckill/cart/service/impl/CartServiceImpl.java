package com.shiwei.seckill.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.cart.mapper.CartItemMapper;
import com.shiwei.seckill.cart.model.CartItem;
import com.shiwei.seckill.cart.model.CartItemEntity;
import com.shiwei.seckill.cart.model.req.CartAddReq;
import com.shiwei.seckill.cart.model.req.CartCheckReq;
import com.shiwei.seckill.cart.model.req.CartRemoveReq;
import com.shiwei.seckill.cart.model.req.CartUpdateReq;
import com.shiwei.seckill.cart.service.CartService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    private static final Long DEFAULT_USER_ID = 1L;

    @Resource
    private CartItemMapper cartItemMapper;

    @Override
    public List<CartItem> listItems() {
        return toItems(cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getUserId, DEFAULT_USER_ID)
                .orderByAsc(CartItemEntity::getCartId)
        ));
    }

    @Override
    public List<CartItem> addItem(CartAddReq req) {
        CartItemEntity existing = cartItemMapper.selectOne(
            new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getUserId, DEFAULT_USER_ID)
                .eq(CartItemEntity::getProductId, req.getProductId())
                .last("limit 1")
        );
        int quantity = req.getQuantity() == null || req.getQuantity() < 1 ? 1 : req.getQuantity();

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemMapper.updateById(existing);
            return listItems();
        }

        CartItemEntity entity = new CartItemEntity();
        entity.setUserId(DEFAULT_USER_ID);
        entity.setProductId(req.getProductId());
        entity.setProductName(req.getProductName());
        entity.setProductImage(req.getProductImage());
        entity.setPrice(req.getPrice() == null ? BigDecimal.ZERO : req.getPrice());
        entity.setQuantity(quantity);
        entity.setChecked(Boolean.TRUE);
        cartItemMapper.insert(entity);
        return listItems();
    }

    @Override
    public List<CartItem> updateItem(CartUpdateReq req) {
        CartItemEntity entity = cartItemMapper.selectById(req.getCartId());
        if (entity != null && DEFAULT_USER_ID.equals(entity.getUserId())) {
            entity.setQuantity(Math.max(1, req.getQuantity() == null ? 1 : req.getQuantity()));
            cartItemMapper.updateById(entity);
        }
        return listItems();
    }

    @Override
    public List<CartItem> removeItem(CartRemoveReq req) {
        CartItemEntity entity = cartItemMapper.selectById(req.getCartId());
        if (entity != null && DEFAULT_USER_ID.equals(entity.getUserId())) {
            cartItemMapper.deleteById(req.getCartId());
        }
        return listItems();
    }

    @Override
    public List<CartItem> checkItem(CartCheckReq req) {
        if (req.getCartId() == null || req.getCartId() == 0L) {
            List<CartItemEntity> entities = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItemEntity>().eq(CartItemEntity::getUserId, DEFAULT_USER_ID)
            );
            boolean checked = req.getChecked() != null && req.getChecked();
            for (CartItemEntity entity : entities) {
                entity.setChecked(checked);
                cartItemMapper.updateById(entity);
            }
            return listItems();
        }

        CartItemEntity entity = cartItemMapper.selectById(req.getCartId());
        if (entity != null && DEFAULT_USER_ID.equals(entity.getUserId())) {
            entity.setChecked(req.getChecked() != null && req.getChecked());
            cartItemMapper.updateById(entity);
        }
        return listItems();
    }

    private List<CartItem> toItems(List<CartItemEntity> entities) {
        return entities.stream().map(this::toItem).collect(Collectors.toList());
    }

    private CartItem toItem(CartItemEntity entity) {
        CartItem item = new CartItem();
        item.setCartId(entity.getCartId());
        item.setProductId(entity.getProductId());
        item.setProductName(entity.getProductName());
        item.setProductImage(entity.getProductImage());
        item.setPrice(entity.getPrice());
        item.setQuantity(entity.getQuantity());
        item.setChecked(Boolean.TRUE.equals(entity.getChecked()));
        return item;
    }
}
