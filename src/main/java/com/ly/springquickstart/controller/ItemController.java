package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Item;
import com.ly.springquickstart.pojo.ItemSku;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.ItemService;
import com.ly.springquickstart.service.MerchantService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ══════════════════════════════════════════════════
    //  公开接口（游客/买家）
    //  ⚠️ 固定路径必须在 /{id} 之前，否则被当成 id 解析
    // ══════════════════════════════════════════════════

    /** 搜索商品 */
    @GetMapping("/search")
    public Result search(
            @RequestParam(required = false) String kw,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(itemService.search(kw, categoryId, sort, pageNum, pageSize));
    }

    /** 个性化推荐(从旧版保留,需要登录) */
    @GetMapping("/recommend")
    public Result getRecommend() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            return Result.error("用户未登录");
        }
        Integer userId = Integer.parseInt(claims.get("id").toString());

        String redisKey = "rec:user:" + userId;
        String recJson = stringRedisTemplate.opsForValue().get(redisKey);

        List<Map<String, Object>> list;

        if (recJson != null && !recJson.isEmpty() && !recJson.equals("[]")) {
            String idsStr = recJson.replace("[", "").replace("]", "");
            String sql = "SELECT * FROM items WHERE id IN (" + idsStr + ")";
            list = jdbcTemplate.queryForList(sql);
        } else {
            String fallbackSql = "SELECT * FROM items WHERE image_url IS NOT NULL LIMIT 10";
            list = jdbcTemplate.queryForList(fallbackSql);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /** 普通分页查询（从旧版保留） */
    @GetMapping("/page")
    public Result getItems(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM items WHERE image_url IS NOT NULL LIMIT ? OFFSET ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, pageSize, offset);
        String countSql = "SELECT COUNT(*) FROM items WHERE image_url IS NOT NULL";
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("total", total);
        return Result.success(data);
    }

    // ══════════════════════════════════════════════════
    //  商家接口（需要登录且是认证商家）
    // ══════════════════════════════════════════════════

    /** 商家自己的商品列表 */
    @GetMapping("/merchant/list")
    public Result merchantList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long merchantId = getMerchantId();
        return Result.success(itemService.listByMerchant(merchantId, pageNum, pageSize));
    }

    /** 发布商品 */
    @PostMapping
    public Result publish(@RequestBody Map<String, Object> body) {
        Long merchantId = getMerchantId();
        Item item = parseItem(body);
        @SuppressWarnings("unchecked")
        List<ItemSku> skus = (List<ItemSku>) body.get("skus");
        Long itemId = itemService.publish(merchantId, item, skus);
        return Result.success(itemId);
    }

    /** 编辑商品（含 SKU） */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long merchantId = getMerchantId();
        Item item = parseItem(body);
        if (item.getTitle() == null) {
            item = new Item();
            if (body.get("title") != null) item.setTitle((String) body.get("title"));
            if (body.get("categoryId") != null) item.setCategoryId(Integer.parseInt(body.get("categoryId").toString()));
            if (body.get("description") != null) item.setDescription((String) body.get("description"));
            if (body.get("imageUrl") != null) item.setImageUrl((String) body.get("imageUrl"));
            if (body.get("price") != null) item.setPrice(new java.math.BigDecimal(body.get("price").toString()));
        }
        item.setId(id);
        item.setMerchantId(merchantId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skuMaps = (List<Map<String, Object>>) body.get("skus");
        itemService.updateWithSkus(merchantId, item, skuMaps);
        return Result.success();
    }

    /** 上架 / 下架 */
    @PutMapping("/{id}/status")
    public Result changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Long merchantId = getMerchantId();
        int status = body.getOrDefault("status", 1);
        itemService.changeStatus(merchantId, id, status);
        return Result.success();
    }

    /** 修改 SKU 库存/价格 */
    @PutMapping("/skus/{skuId}")
    public Result updateSku(@PathVariable Long skuId, @RequestBody ItemSku sku) {
        sku.setId(skuId);
        itemService.updateSku(sku);
        return Result.success();
    }

    // ══════════════════════════════════════════════════
    //  ⚠️ 通配符路径放最后，避免拦截上面的固定路径
    // ══════════════════════════════════════════════════

    /** 商品详情（含 SKU） */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        return Result.success(itemService.getDetail(id));
    }

    // ══════════════════════════════════════════════════
    //  工具方法
    // ══════════════════════════════════════════════════

    private Long getCurrentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }

    private Long getMerchantId() {
        return merchantService.getMerchantIdByUser(getCurrentUserId());
    }

    @SuppressWarnings("unchecked")
    private Item parseItem(Map<String, Object> body) {
        Map<String, Object> itemMap = (Map<String, Object>) body.get("item");
        Item item = new Item();
        if (itemMap == null) return item;
        item.setCategoryId(itemMap.get("categoryId") != null
                ? Integer.parseInt(itemMap.get("categoryId").toString()) : null);
        item.setTitle((String) itemMap.get("title"));
        item.setDescription((String) itemMap.get("description"));
        item.setImageUrl((String) itemMap.get("imageUrl"));
        if (itemMap.get("price") != null) {
            item.setPrice(new java.math.BigDecimal(itemMap.get("price").toString()));
        }
        return item;
    }
}