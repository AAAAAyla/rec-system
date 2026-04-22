package com.ly.springquickstart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ly.springquickstart.mapper.ItemMapper;
import com.ly.springquickstart.pojo.CartItem;
import com.ly.springquickstart.pojo.ItemSku;
import com.ly.springquickstart.pojo.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 购物车服务（购物车数据存储于 Redis Hash，不落库）
 * Redis 结构：key = cart:{userId}，field = {itemId}:{skuId}，value = CartItem JSON
 */
@Service
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_EXPIRE_DAYS = 7;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String field(Long itemId, Long skuId) {
        return itemId + ":" + skuId;
    }

    /** 获取购物车列表 */
    public List<CartItem> list(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey(userId));
        List<CartItem> result = new ArrayList<>();
        for (Object val : entries.values()) {
            try {
                result.add(objectMapper.readValue(val.toString(), CartItem.class));
            } catch (JsonProcessingException ignored) {
            }
        }
        return result;
    }

    /**
     * 加入购物车（已有则叠加数量）
     */
    public void add(Long userId, Long itemId, Long skuId, Integer quantity) {
        ItemSku sku = itemMapper.findSkuById(skuId);
        if (sku == null) throw new RuntimeException("SKU 不存在");

        Item item = itemMapper.findById(itemId);
        if (item == null) throw new RuntimeException("商品不存在");

        String key   = cartKey(userId);
        String field = field(itemId, skuId);

        CartItem existing = getItem(userId, itemId, skuId);
        CartItem cartItem;
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            existing.setPrice(sku.getPrice());
            cartItem = existing;
        } else {
            cartItem = new CartItem();
            cartItem.setItemId(itemId);
            cartItem.setSkuId(skuId);
            cartItem.setTitle(item.getTitle());
            cartItem.setImageUrl(item.getImageUrl());
            cartItem.setSpecJson(sku.getSpecJson());
            cartItem.setPrice(sku.getPrice());
            cartItem.setQuantity(quantity);
        }

        redisTemplate.opsForHash().put(key, field, toJson(cartItem));
        redisTemplate.expire(key, CART_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /** 修改购物车条目数量 */
    public void updateQuantity(Long userId, Long itemId, Long skuId, Integer quantity) {
        if (quantity <= 0) {
            remove(userId, itemId, skuId);
            return;
        }
        CartItem item = getItem(userId, itemId, skuId);
        if (item == null) throw new RuntimeException("购物车中无此商品");
        item.setQuantity(quantity);
        redisTemplate.opsForHash().put(cartKey(userId), field(itemId, skuId), toJson(item));
    }

    /** 切换选中状态 */
    public void toggleChecked(Long userId, Long itemId, Long skuId, Boolean checked) {
        CartItem item = getItem(userId, itemId, skuId);
        if (item == null) throw new RuntimeException("购物车中无此商品");
        item.setChecked(checked);
        redisTemplate.opsForHash().put(cartKey(userId), field(itemId, skuId), toJson(item));
    }

    /** 全选 / 取消全选 */
    public void checkAll(Long userId, Boolean checked) {
        List<CartItem> items = list(userId);
        for (CartItem item : items) {
            item.setChecked(checked);
            redisTemplate.opsForHash().put(cartKey(userId), field(item.getItemId(), item.getSkuId()), toJson(item));
        }
    }

    /** 删除单条 */
    public void remove(Long userId, Long itemId, Long skuId) {
        redisTemplate.opsForHash().delete(cartKey(userId), field(itemId, skuId));
    }

    /** 清空购物车 */
    public void clear(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    /** 删除已选中的条目（下单后调用） */
    public void removeChecked(Long userId) {
        List<CartItem> items = list(userId);
        for (CartItem item : items) {
            if (Boolean.TRUE.equals(item.getChecked())) {
                remove(userId, item.getItemId(), item.getSkuId());
            }
        }
    }

    /** 计算选中商品总价 */
    public BigDecimal calcCheckedTotal(Long userId) {
        return list(userId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getChecked()))
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartItem getItem(Long userId, Long itemId, Long skuId) {
        Object val = redisTemplate.opsForHash().get(cartKey(userId), field(itemId, skuId));
        if (val == null) return null;
        try {
            return objectMapper.readValue(val.toString(), CartItem.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
