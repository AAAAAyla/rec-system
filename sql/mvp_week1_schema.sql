-- =============================================
-- AmazonRec MVP 数据库建表脚本（Week 1）
-- 在现有 users / items 表基础上补充
-- =============================================

USE rec_system;

-- ----------------------------
-- 1. 扩展 users 表（如果字段不存在则添加）
-- ----------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    ADD COLUMN IF NOT EXISTS avatar       VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    ADD COLUMN IF NOT EXISTS role         TINYINT      NOT NULL DEFAULT 0 COMMENT '0=买家 1=商家 2=管理员',
    ADD COLUMN IF NOT EXISTS status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=正常 0=封禁',
    ADD COLUMN IF NOT EXISTS create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ----------------------------
-- 2. 商家资料表
-- ----------------------------
CREATE TABLE IF NOT EXISTS merchants (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL UNIQUE COMMENT '关联 users.id',
    shop_name    VARCHAR(100) NOT NULL COMMENT '店铺名称',
    shop_desc    VARCHAR(500) DEFAULT NULL COMMENT '店铺简介',
    avatar       VARCHAR(500) DEFAULT NULL COMMENT '店铺头像',
    license_url  VARCHAR(500) DEFAULT NULL COMMENT '营业执照图片',
    contact_phone VARCHAR(20) DEFAULT NULL,
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待审核 1=正常 2=拒绝 3=封禁',
    reject_reason VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家资料';

-- ----------------------------
-- 3. 商品分类表（三级树）
-- ----------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parent_id   INT          NOT NULL DEFAULT 0 COMMENT '0=一级分类',
    name        VARCHAR(50)  NOT NULL,
    level       TINYINT      NOT NULL DEFAULT 1 COMMENT '1/2/3',
    sort        INT          NOT NULL DEFAULT 0,
    icon_url    VARCHAR(500) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- 初始化部分分类数据
INSERT IGNORE INTO categories (id, parent_id, name, level, sort) VALUES
(1,  0, '电子产品',    1, 1),
(2,  0, '运动户外',    1, 2),
(3,  0, '服饰鞋包',    1, 3),
(4,  0, '家居生活',    1, 4),
(10, 1, '手机',        2, 1),
(11, 1, '电脑',        2, 2),
(12, 1, '耳机',        2, 3),
(20, 2, '健身器材',    2, 1),
(21, 2, '户外运动',    2, 2),
(30, 3, '男装',        2, 1),
(31, 3, '女装',        2, 2),
(32, 3, '鞋类',        2, 3);

-- ----------------------------
-- 4. 扩展 items 表（添加商家与分类字段）
-- ----------------------------
ALTER TABLE items
    ADD COLUMN IF NOT EXISTS merchant_id  BIGINT       DEFAULT NULL COMMENT '商家ID',
    ADD COLUMN IF NOT EXISTS category_id  INT          DEFAULT NULL COMMENT '分类ID',
    ADD COLUMN IF NOT EXISTS description  TEXT         DEFAULT NULL COMMENT '商品详情富文本',
    ADD COLUMN IF NOT EXISTS status       TINYINT      NOT NULL DEFAULT 1 COMMENT '0=下架 1=在售 2=待审核',
    ADD COLUMN IF NOT EXISTS sales_count  INT          NOT NULL DEFAULT 0 COMMENT '累计销量',
    ADD COLUMN IF NOT EXISTS price        DECIMAL(10,2) DEFAULT 0.00 COMMENT '默认价格（兜底）',
    ADD COLUMN IF NOT EXISTS stock        INT          NOT NULL DEFAULT 0 COMMENT '总库存（冗余）',
    ADD COLUMN IF NOT EXISTS create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    ADD INDEX IF NOT EXISTS idx_merchant (merchant_id),
    ADD INDEX IF NOT EXISTS idx_category (category_id),
    ADD INDEX IF NOT EXISTS idx_status   (status);

-- ----------------------------
-- 5. SKU 规格表（每个商品可有多个 SKU）
-- ----------------------------
CREATE TABLE IF NOT EXISTS item_skus (
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id    BIGINT         NOT NULL COMMENT '关联 items.id',
    spec_json  VARCHAR(500)   DEFAULT NULL COMMENT '规格JSON，如 {"颜色":"红","尺寸":"M"}',
    price      DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    stock      INT            NOT NULL DEFAULT 0,
    image_url  VARCHAR(500)   DEFAULT NULL COMMENT 'SKU 对应图片',
    status     TINYINT        NOT NULL DEFAULT 1 COMMENT '1=正常 0=下架',
    INDEX idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU规格';

-- ----------------------------
-- 6. 商品图片表
-- ----------------------------
CREATE TABLE IF NOT EXISTS item_images (
    id       BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id  BIGINT      NOT NULL,
    url      VARCHAR(500) NOT NULL,
    sort     INT         NOT NULL DEFAULT 0,
    type     TINYINT     NOT NULL DEFAULT 1 COMMENT '1=主图 2=详情图',
    INDEX idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片';

-- ----------------------------
-- 7. 商家发货仓库/地址
-- ----------------------------
CREATE TABLE IF NOT EXISTS warehouses (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT      NOT NULL,
    name        VARCHAR(100) NOT NULL COMMENT '仓库名称',
    contact     VARCHAR(50)  DEFAULT NULL,
    phone       VARCHAR(20)  DEFAULT NULL,
    province    VARCHAR(50)  NOT NULL,
    city        VARCHAR(50)  NOT NULL,
    district    VARCHAR(50)  DEFAULT NULL,
    detail      VARCHAR(200) NOT NULL,
    is_default  TINYINT     NOT NULL DEFAULT 0,
    INDEX idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家仓库/发货地址';

-- ----------------------------
-- 8. 用户收货地址
-- ----------------------------
CREATE TABLE IF NOT EXISTS addresses (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    name       VARCHAR(50)  NOT NULL COMMENT '收件人姓名',
    phone      VARCHAR(20)  NOT NULL,
    province   VARCHAR(50)  NOT NULL,
    city       VARCHAR(50)  NOT NULL,
    district   VARCHAR(50)  DEFAULT NULL,
    detail     VARCHAR(200) NOT NULL,
    is_default TINYINT     NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址';

-- ----------------------------
-- 9. 订单主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no         VARCHAR(32)    NOT NULL UNIQUE COMMENT '订单号（雪花/时间戳生成）',
    user_id          BIGINT         NOT NULL,
    merchant_id      BIGINT         NOT NULL,
    status           TINYINT        NOT NULL DEFAULT 0
                     COMMENT '0=待付款 1=已付款/待发货 2=已发货 3=已完成 4=已取消 5=退款中 6=已退款',
    total_amount     DECIMAL(10,2)  NOT NULL COMMENT '商品总价',
    freight_amount   DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '运费',
    pay_amount       DECIMAL(10,2)  NOT NULL COMMENT '实际支付金额',
    address_snapshot JSON           DEFAULT NULL COMMENT '下单时地址快照',
    remark           VARCHAR(200)   DEFAULT NULL COMMENT '买家备注',
    cancel_reason    VARCHAR(200)   DEFAULT NULL,
    pay_time         DATETIME       DEFAULT NULL,
    create_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- ----------------------------
-- 10. 订单商品明细
-- ----------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    item_id    BIGINT         NOT NULL,
    sku_id     BIGINT         DEFAULT NULL,
    title      VARCHAR(200)   NOT NULL COMMENT '下单时商品名快照',
    image_url  VARCHAR(500)   DEFAULT NULL,
    spec_json  VARCHAR(500)   DEFAULT NULL COMMENT '规格快照',
    price      DECIMAL(10,2)  NOT NULL COMMENT '下单时单价',
    quantity   INT            NOT NULL DEFAULT 1,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细';

-- ----------------------------
-- 11. 物流信息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS shipments (
    id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id         BIGINT      NOT NULL UNIQUE,
    express_company  VARCHAR(50) NOT NULL COMMENT '快递公司',
    tracking_no      VARCHAR(50) NOT NULL COMMENT '快递单号',
    ship_time        DATETIME    DEFAULT NULL COMMENT '发货时间',
    status           TINYINT     NOT NULL DEFAULT 0 COMMENT '0=运输中 1=已签收',
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流信息';

-- ----------------------------
-- 12. 物流节点（时间轴）
-- ----------------------------
CREATE TABLE IF NOT EXISTS shipment_tracks (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT      NOT NULL,
    location    VARCHAR(100) DEFAULT NULL COMMENT '当前位置',
    description VARCHAR(200) NOT NULL COMMENT '节点说明',
    track_time  DATETIME    NOT NULL,
    INDEX idx_shipment_id (shipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流节点';

-- ----------------------------
-- 13. IM 会话表
-- ----------------------------
CREATE TABLE IF NOT EXISTS im_sessions (
    id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    merchant_id      BIGINT      NOT NULL,
    last_message     VARCHAR(200) DEFAULT NULL,
    last_time        DATETIME    DEFAULT CURRENT_TIMESTAMP,
    unread_user      INT         NOT NULL DEFAULT 0 COMMENT '买家未读数',
    unread_merchant  INT         NOT NULL DEFAULT 0 COMMENT '商家未读数',
    UNIQUE KEY uk_session (user_id, merchant_id),
    INDEX idx_user (user_id),
    INDEX idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM 会话';

-- ----------------------------
-- 14. IM 消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS im_messages (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id   BIGINT      NOT NULL,
    sender_type  TINYINT     NOT NULL COMMENT '0=买家 1=商家',
    content      TEXT        NOT NULL,
    type         TINYINT     NOT NULL DEFAULT 0 COMMENT '0=文字 1=图片',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM 消息';

-- ----------------------------
-- 15. 商品评价表（Week1 建好，Week2 用）
-- ----------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id            BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT         NOT NULL,
    user_id       BIGINT         NOT NULL,
    item_id       BIGINT         NOT NULL,
    score         TINYINT        NOT NULL DEFAULT 5 COMMENT '1-5分',
    content       VARCHAR(500)   DEFAULT NULL,
    images        VARCHAR(2000)  DEFAULT NULL COMMENT '图片URL逗号分隔',
    create_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item (item_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价';


-- 1. 添加列
ALTER TABLE items ADD COLUMN amazon_item_id VARCHAR(20) DEFAULT NULL COMMENT '亚马逊原商品ID';

-- 2. 创建索引
CREATE INDEX idx_amazon_item ON items(amazon_item_id);

ALTER TABLE users ADD COLUMN role TINYINT NOT NULL DEFAULT 0 COMMENT '0:普通用户 1:商家 2:管理员';
ALTER TABLE users ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0:禁用 1:正常';
ALTER TABLE items ADD COLUMN amazon_item_id VARCHAR(20) DEFAULT NULL COMMENT '原始亚马逊商品ID';

ALTER TABLE items ADD COLUMN status TINYINT NOT NULL DEFAULT 1;
ALTER TABLE items ADD COLUMN stock INT NOT NULL DEFAULT 0;
ALTER TABLE items ADD COLUMN price DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE items ADD COLUMN merchant_id BIGINT DEFAULT NULL;
ALTER TABLE items ADD COLUMN category_id INT DEFAULT NULL;
ALTER TABLE items ADD COLUMN description TEXT DEFAULT NULL;
ALTER TABLE items ADD COLUMN sales_count INT NOT NULL DEFAULT 0;

ALTER TABLE items ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP;