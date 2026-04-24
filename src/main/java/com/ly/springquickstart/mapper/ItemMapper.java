package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Item;
import com.ly.springquickstart.pojo.ItemSku;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ItemMapper {

    // ── 商品基本查询 ────────────────────────────────────
    @Select("SELECT * FROM items WHERE id = #{id}")
    Item findById(Long id);

    @Select("""
        SELECT * FROM items
        WHERE status = 1
          AND (#{kw} IS NULL OR title LIKE CONCAT('%',#{kw},'%'))
          AND (#{categoryId} IS NULL
               OR category_id = #{categoryId}
               OR category_id IN (SELECT id FROM categories WHERE parent_id = #{categoryId}))
        ORDER BY
          CASE #{sort}
            WHEN 'price_asc'  THEN price  END ASC,
          CASE #{sort}
            WHEN 'price_desc' THEN price  END DESC,
          CASE #{sort}
            WHEN 'sales'      THEN sales_count END DESC,
          create_time DESC
        LIMIT #{offset}, #{size}
        """)
    List<Item> search(@Param("kw") String kw,
                      @Param("categoryId") Integer categoryId,
                      @Param("sort") String sort,
                      @Param("offset") int offset,
                      @Param("size") int size);

    @Select("""
        SELECT COUNT(*) FROM items
        WHERE status = 1
          AND (#{kw} IS NULL OR title LIKE CONCAT('%',#{kw},'%'))
          AND (#{categoryId} IS NULL
               OR category_id = #{categoryId}
               OR category_id IN (SELECT id FROM categories WHERE parent_id = #{categoryId}))
        """)
    int searchCount(@Param("kw") String kw, @Param("categoryId") Integer categoryId);

    @Select("SELECT * FROM items WHERE merchant_id = #{merchantId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Item> findByMerchant(@Param("merchantId") Long merchantId,
                              @Param("offset") int offset,
                              @Param("size") int size);

    @Select("SELECT COUNT(*) FROM items WHERE merchant_id = #{merchantId}")
    int countByMerchant(Long merchantId);

    // ── 商品写操作 ──────────────────────────────────────
    @Insert("""
        INSERT INTO items(merchant_id, category_id, title, description, image_url, price, stock, status)
        VALUES(#{merchantId}, #{categoryId}, #{title}, #{description}, #{imageUrl}, #{price}, #{stock}, 2)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Item item);

    @Update("""
        UPDATE items SET title=#{title}, category_id=#{categoryId},
        description=#{description}, price=#{price}, image_url=#{imageUrl},
        stock=#{stock}
        WHERE id=#{id} AND merchant_id=#{merchantId}
        """)
    void update(Item item);

    @Update("UPDATE items SET status=#{status} WHERE id=#{id} AND merchant_id=#{merchantId}")
    void updateStatus(@Param("id") Long id,
                      @Param("merchantId") Long merchantId,
                      @Param("status") int status);

    /** 审核通过 */
    @Update("UPDATE items SET status=1 WHERE id=#{id}")
    void approve(Long id);

    /** 累计销量 */
    @Update("UPDATE items SET sales_count = sales_count + #{qty}, stock = stock - #{qty} WHERE id = #{id}")
    void incrSales(@Param("id") Long id, @Param("qty") int qty);

    // ── SKU ────────────────────────────────────────────
    @Select("SELECT * FROM item_skus WHERE item_id = #{itemId} AND status = 1")
    List<ItemSku> findSkusByItemId(Long itemId);

    @Select("SELECT * FROM item_skus WHERE id = #{id}")
    ItemSku findSkuById(Long id);

    @Insert("""
        INSERT INTO item_skus(item_id, spec_json, price, stock, image_url)
        VALUES(#{itemId}, #{specJson}, #{price}, #{stock}, #{imageUrl})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSku(ItemSku sku);

    @Update("UPDATE item_skus SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}")
    int deductSkuStock(@Param("id") Long id, @Param("qty") int qty);

    @Update("UPDATE item_skus SET stock = #{stock}, price = #{price} WHERE id = #{id}")
    void updateSku(ItemSku sku);

    @Delete("DELETE FROM item_skus WHERE item_id = #{itemId}")
    void deleteSkusByItemId(Long itemId);

    // ── 图片 ───────────────────────────────────────────
    @Insert("INSERT INTO item_images(item_id, url, sort, type) VALUES(#{itemId}, #{url}, #{sort}, #{type})")
    void insertImage(@Param("itemId") Long itemId,
                     @Param("url") String url,
                     @Param("sort") int sort,
                     @Param("type") int type);

    @Select("SELECT url FROM item_images WHERE item_id = #{itemId} AND type = 1 ORDER BY sort")
    List<String> findImageUrls(Long itemId);

    // ItemMapper.java 里新增
    @Update("UPDATE item_skus SET stock = stock + #{qty} WHERE id = #{id}")
    void restoreSkuStock(@Param("id") Long id, @Param("qty") int qty);
}