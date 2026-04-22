-- ============================================================
-- 平台自营店初始化脚本
-- 执行方式：在 MySQL 中选中你的数据库后运行此文件
-- ============================================================

-- ─────────────────────────────────────────────────────────
-- 第 1 步：扩展 users 表（如缺少字段则添加）
-- ─────────────────────────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS nickname VARCHAR(60)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS avatar   VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS phone    VARCHAR(20)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS role     TINYINT      NOT NULL DEFAULT 0
        COMMENT '0=普通买家 1=商家 2=管理员';

-- ─────────────────────────────────────────────────────────
-- 第 2 步：创建平台官方管理员账号
--   默认密码 admin123（明文，与当前系统一致），上线前请修改！
-- ─────────────────────────────────────────────────────────
INSERT INTO users (username, password, role, status)
SELECT 'platform_admin', 'admin123', 2, 1
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'platform_admin'
);

-- ─────────────────────────────────────────────────────────
-- 第 3 步：创建 merchants 表（如不存在）
-- ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS merchants (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL UNIQUE,
    shop_name      VARCHAR(100) NOT NULL,
    shop_desc      VARCHAR(500) DEFAULT NULL,
    avatar         VARCHAR(500) DEFAULT NULL,
    license_url    VARCHAR(500) DEFAULT NULL,
    contact_phone  VARCHAR(20)  DEFAULT NULL,
    status         TINYINT      NOT NULL DEFAULT 0
        COMMENT '0=待审核 1=正常 2=拒绝 3=封禁',
    reject_reason  VARCHAR(300) DEFAULT NULL,
    create_time    DATETIME     DEFAULT NOW(),
    update_time    DATETIME     DEFAULT NOW() ON UPDATE NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────
-- 第 4 步：为平台官方账号创建"平台自营店"商家记录
-- ─────────────────────────────────────────────────────────
INSERT INTO merchants (user_id, shop_name, shop_desc, avatar, status)
SELECT u.id,
       '平台自营旗舰店',
       '官方直营，品质保证，七天无理由退换',
       'https://via.placeholder.com/100?text=Official',
       1
FROM users u
WHERE u.username = 'platform_admin'
  AND NOT EXISTS (
      SELECT 1 FROM merchants m WHERE m.user_id = u.id
  );

-- ─────────────────────────────────────────────────────────
-- 第 5 步：将所有 merchant_id 为 NULL 的商品归属到平台自营店
-- ─────────────────────────────────────────────────────────
UPDATE items
SET merchant_id = (
    SELECT m.id FROM merchants m
    JOIN users u ON u.id = m.user_id
    WHERE u.username = 'platform_admin'
    LIMIT 1
)
WHERE merchant_id IS NULL;

-- ─────────────────────────────────────────────────────────
-- 第 6 步：为没有 SKU 的在售商品自动生成默认 SKU
--   避免下单时因找不到 SKU 而报错
-- ─────────────────────────────────────────────────────────
INSERT INTO item_skus (item_id, spec_json, price, stock, image_url)
SELECT i.id, '{"规格":"默认"}', i.price, COALESCE(i.stock, 100), i.image_url
FROM items i
WHERE i.status = 1
  AND i.price IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM item_skus s WHERE s.item_id = i.id
  );

-- ─────────────────────────────────────────────────────────
-- 第 7 步：确认结果
-- ─────────────────────────────────────────────────────────
SELECT '=== 平台自营店初始化完成 ===' AS msg;

SELECT u.id AS user_id, u.username, u.role, m.id AS merchant_id, m.shop_name, m.status AS merchant_status
FROM users u
JOIN merchants m ON m.user_id = u.id
WHERE u.username = 'platform_admin';

SELECT COUNT(*) AS total_items,
       SUM(CASE WHEN merchant_id IS NULL THEN 1 ELSE 0 END) AS items_without_merchant
FROM items;
