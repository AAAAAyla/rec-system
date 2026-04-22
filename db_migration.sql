-- ============================================================
-- 数据库补丁脚本（修复缺失表/列/数据）
-- 执行方式：直接在 MySQL 命令行或 Navicat/DBeaver 中运行
-- 安全：所有操作均含 IF NOT EXISTS 保护
-- ============================================================

USE rec_system;   -- ← 数据库名与 application.yaml 保持一致

-- ─────────────────────────────────────────────────────────
-- 1. users 表：补充 role / nickname / avatar / phone 字段
-- ─────────────────────────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role     TINYINT      NOT NULL DEFAULT 0
        COMMENT '0=买家 1=商家 2=管理员',
    ADD COLUMN IF NOT EXISTS nickname VARCHAR(60)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS avatar   VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS phone    VARCHAR(20)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS status   TINYINT      NOT NULL DEFAULT 1
        COMMENT '1=正常 0=封禁';

-- ─────────────────────────────────────────────────────────
-- 2. merchants 表：创建（如不存在）+ 补缺失列
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

-- 已有表时，补加缺失列（MySQL 8.0+: IF NOT EXISTS；5.7 可删掉 IF NOT EXISTS 并手动执行缺失的行）
ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS shop_desc     VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS avatar        VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS license_url   VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(300) DEFAULT NULL;

-- ─────────────────────────────────────────────────────────
-- 3. addresses 表（如不存在则创建）
-- ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS addresses (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(50)  NOT NULL COMMENT '收件人姓名',
    phone       VARCHAR(20)  NOT NULL,
    province    VARCHAR(30)  DEFAULT NULL,
    city        VARCHAR(30)  DEFAULT NULL,
    district    VARCHAR(30)  DEFAULT NULL,
    detail      VARCHAR(200) NOT NULL,
    is_default  TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     DEFAULT NOW(),
    update_time DATETIME     DEFAULT NOW() ON UPDATE NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────
-- 4. shipments / shipment_tracks 表（如不存在则创建）
-- ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS shipments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT       NOT NULL UNIQUE,
    express_company VARCHAR(60)  DEFAULT NULL,
    tracking_no     VARCHAR(60)  DEFAULT NULL,
    ship_time       DATETIME     DEFAULT NOW(),
    create_time     DATETIME     DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shipment_tracks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT        NOT NULL,
    location    VARCHAR(100)  DEFAULT NULL,
    description VARCHAR(300)  NOT NULL,
    track_time  DATETIME      DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────
-- 5. item_reviews 表（评价系统）
-- ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS item_reviews (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT    NOT NULL,
    order_item_id BIGINT    NOT NULL,
    user_id       BIGINT    NOT NULL,
    item_id       BIGINT    NOT NULL,
    score         TINYINT   NOT NULL DEFAULT 5 COMMENT '1-5分',
    content       TEXT,
    images        VARCHAR(1000) COMMENT '图片URL，英文逗号分隔',
    create_time   DATETIME  DEFAULT NOW(),
    UNIQUE KEY uk_order_item (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────
-- 6. item_skus 表：补充 status 字段（如缺失）
-- ─────────────────────────────────────────────────────────
ALTER TABLE item_skus
    ADD COLUMN IF NOT EXISTS status TINYINT NOT NULL DEFAULT 1
        COMMENT '1=正常 0=下架';

-- 把所有 status=NULL 的 SKU 设为正常
UPDATE item_skus SET status = 1 WHERE status IS NULL;

-- ─────────────────────────────────────────────────────────
-- 7. 修复库存为 0 的 SKU（开发环境默认补 100）
-- ─────────────────────────────────────────────────────────
UPDATE item_skus SET stock = 100 WHERE stock = 0 OR stock IS NULL;
UPDATE items      SET stock = 100 WHERE stock = 0 OR stock IS NULL;

-- ─────────────────────────────────────────────────────────
-- 8. 为没有 SKU 的在售商品自动生成默认 SKU
-- ─────────────────────────────────────────────────────────
INSERT INTO item_skus (item_id, spec_json, price, stock, status)
SELECT i.id,
       '{"规格":"默认"}',
       COALESCE(i.price, 9.99),
       COALESCE(i.stock, 100),
       1
FROM items i
WHERE i.status = 1
  AND i.price IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM item_skus s WHERE s.item_id = i.id
  );

-- ─────────────────────────────────────────────────────────
-- 9. 平台自营店：官方管理员账号
-- ─────────────────────────────────────────────────────────
INSERT INTO users (username, password, role, status)
SELECT 'platform_admin', 'admin123', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'platform_admin');

-- 官方商家记录
INSERT INTO merchants (user_id, shop_name, shop_desc, status)
SELECT u.id,
       '平台自营旗舰店',
       '官方直营，品质保证，七天无理由退换',
       1
FROM users u
WHERE u.username = 'platform_admin'
  AND NOT EXISTS (
      SELECT 1 FROM merchants m WHERE m.user_id = u.id
  );

-- 把所有 merchant_id 为 NULL 的商品归属到平台自营店
UPDATE items
SET merchant_id = (
    SELECT m.id FROM merchants m
    JOIN users u ON u.id = m.user_id
    WHERE u.username = 'platform_admin'
    LIMIT 1
)
WHERE merchant_id IS NULL;

-- ─────────────────────────────────────────────────────────
-- 确认
-- ─────────────────────────────────────────────────────────
SELECT '=== 数据库补丁执行完成 ===' AS result;

SELECT COUNT(*) AS sku_count,
       SUM(CASE WHEN stock = 0 THEN 1 ELSE 0 END) AS zero_stock_skus
FROM item_skus;

SELECT COUNT(*) AS review_table_ok FROM item_reviews LIMIT 0;
