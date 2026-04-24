package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.ItemMapper;
import com.ly.springquickstart.pojo.Item;
import com.ly.springquickstart.pojo.ItemSku;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemService {

    @Autowired
    private ItemMapper itemMapper;

    // ── 查询 ──────────────────────────────────────────

    /** 商品详情（含 SKU + 图片） */
    public Item getDetail(Long id) {
        Item item = itemMapper.findById(id);
        if (item == null) throw new RuntimeException("商品不存在");
        item.setSkus(itemMapper.findSkusByItemId(id));
        item.setImageUrls(itemMapper.findImageUrls(id));
        return item;
    }

    /** 搜索（分页） */
    public Map<String, Object> search(String kw, Integer categoryId,
                                       String sort, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Item> rows = itemMapper.search(kw, categoryId, sort, offset, pageSize);
        int total = itemMapper.searchCount(kw, categoryId);

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    /** 商家自己的商品列表 */
    public Map<String, Object> listByMerchant(Long merchantId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Item> rows = itemMapper.findByMerchant(merchantId, offset, pageSize);
        int total = itemMapper.countByMerchant(merchantId);

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    // ── 商家写操作 ────────────────────────────────────

    /** 发布商品（含 SKU 批量） */
    @Transactional
    public Long publish(Long merchantId, Item item, List<ItemSku> skus) {
        item.setMerchantId(merchantId);
        itemMapper.insert(item);
        Long itemId = item.getId();

        if (!CollectionUtils.isEmpty(skus)) {
            for (ItemSku sku : skus) {
                sku.setItemId(itemId);
                itemMapper.insertSku(sku);
            }
            // 同步总库存
            int totalStock = skus.stream().mapToInt(ItemSku::getStock).sum();
            item.setId(itemId);
            item.setStock(totalStock);
        }
        return itemId;
    }

    /** 编辑商品基本信息 */
    public void update(Long merchantId, Item item) {
        itemMapper.update(item);
    }

    /** 编辑商品 + SKU */
    @Transactional
    public void updateWithSkus(Long merchantId, Item item, List<Map<String, Object>> skuMaps) {
        itemMapper.update(item);
        if (skuMaps != null) {
            itemMapper.deleteSkusByItemId(item.getId());
            for (Map<String, Object> m : skuMaps) {
                ItemSku sku = new ItemSku();
                sku.setItemId(item.getId());
                sku.setSpecJson(m.get("specJson") != null ? m.get("specJson").toString() : "");
                sku.setPrice(m.get("price") != null ? new java.math.BigDecimal(m.get("price").toString()) : java.math.BigDecimal.ZERO);
                sku.setStock(m.get("stock") != null ? Integer.parseInt(m.get("stock").toString()) : 0);
                sku.setImageUrl(m.get("imageUrl") != null ? m.get("imageUrl").toString() : null);
                itemMapper.insertSku(sku);
            }
        }
    }

    /** 上架 / 下架 */
    public void changeStatus(Long merchantId, Long itemId, int status) {
        itemMapper.updateStatus(itemId, merchantId, status);
    }

    /** 修改单个 SKU 库存/价格 */
    public void updateSku(ItemSku sku) {
        itemMapper.updateSku(sku);
    }

    // ── 内部方法（供 OrderService 调用）──────────────

    public ItemSku getSkuById(Long skuId) {
        return itemMapper.findSkuById(skuId);
    }

    /**
     * 原子扣减 SKU 库存（乐观锁：stock >= qty 才更新）
     * @return true=扣减成功 false=库存不足
     */
    public boolean deductStock(Long skuId, int qty) {
        return itemMapper.deductSkuStock(skuId, qty) > 0;
    }

    /** 累计销量 */
    public void incrSales(Long itemId, int qty) {
        itemMapper.incrSales(itemId, qty);
    }

    public void restoreStock(Long skuId, int qty) {
        itemMapper.restoreSkuStock(skuId, qty);
    }
}
