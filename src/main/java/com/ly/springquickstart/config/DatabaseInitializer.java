package com.ly.springquickstart.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 应用启动后自动检查并修复数据库 Schema（兼容 MySQL 5.7 / 8.0）
 * 每次启动都会运行，但每条语句都有幂等保护（先判断后执行）
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("[DB Init] 开始检查数据库 Schema...");
        fixMerchantsTable();
        fixUsersTable();
        fixItemSkusTable();
        createItemReviewsTable();
        createShipmentsTable();
        createShipmentTracksTable();
        createAddressesTable();
        fixZeroStock();
        createFavoritesTable();
        createNotificationsTable();
        fixCouponsTable();
        fixImSessionsTable();
        fixWarehousesTable();
        addColumnIfMissing("users", "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");
        initCategoriesTable();
        addColumnIfMissing("items", "category_id", "INT DEFAULT NULL");
        initAdminAndOfficialMerchant();
        System.out.println("[DB Init] Schema 检查完成");
    }

    // ── merchants 表：先建表（保证存在），再补缺失列 ──────
    private void fixMerchantsTable() {
        // 先确保表存在（含所有列）
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS merchants (
                id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id        BIGINT       NOT NULL UNIQUE,
                shop_name      VARCHAR(100) NOT NULL,
                shop_desc      VARCHAR(500) DEFAULT NULL,
                avatar         VARCHAR(500) DEFAULT NULL,
                license_url    VARCHAR(500) DEFAULT NULL,
                contact_phone  VARCHAR(20)  DEFAULT NULL,
                status         TINYINT      NOT NULL DEFAULT 0,
                reject_reason  VARCHAR(300) DEFAULT NULL,
                create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
                update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        // 如果表是旧版本，再逐列补充（兼容已有表）
        addColumnIfMissing("merchants", "shop_desc",     "VARCHAR(500) DEFAULT NULL");
        addColumnIfMissing("merchants", "avatar",        "VARCHAR(500) DEFAULT NULL");
        addColumnIfMissing("merchants", "license_url",   "VARCHAR(500) DEFAULT NULL");
        addColumnIfMissing("merchants", "contact_phone", "VARCHAR(20)  DEFAULT NULL");
        addColumnIfMissing("merchants", "reject_reason", "VARCHAR(300) DEFAULT NULL");
        addColumnIfMissing("merchants", "update_time",
                "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    }

    // ── users 表：补充 role / nickname / avatar / phone / status ──
    private void fixUsersTable() {
        addColumnIfMissing("users", "role",     "TINYINT NOT NULL DEFAULT 0 COMMENT '0=买家 1=商家 2=管理员'");
        addColumnIfMissing("users", "nickname", "VARCHAR(60)  DEFAULT NULL");
        addColumnIfMissing("users", "avatar",   "VARCHAR(500) DEFAULT NULL");
        addColumnIfMissing("users", "phone",    "VARCHAR(20)  DEFAULT NULL");
        addColumnIfMissing("users", "status",   "TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=封禁'");
    }

    // ── item_skus 表：补充 status 列 ─────────────────────
    private void fixItemSkusTable() {
        addColumnIfMissing("item_skus", "status", "TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=下架'");
        // 把 NULL 值的 status 设为 1
        jdbc.update("UPDATE item_skus SET status = 1 WHERE status IS NULL");
    }

    // ── item_reviews 表 ───────────────────────────────────
    private void createItemReviewsTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS item_reviews (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                order_id      BIGINT    NOT NULL,
                order_item_id BIGINT    NOT NULL,
                user_id       BIGINT    NOT NULL,
                item_id       BIGINT    NOT NULL,
                score         TINYINT   NOT NULL DEFAULT 5,
                content       TEXT,
                images        VARCHAR(1000),
                create_time   DATETIME  DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_order_item (order_item_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    // ── shipments 表 ──────────────────────────────────────
    private void createShipmentsTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS shipments (
                id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                order_id        BIGINT       NOT NULL UNIQUE,
                express_company VARCHAR(60)  DEFAULT NULL,
                tracking_no     VARCHAR(60)  DEFAULT NULL,
                ship_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
                create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    // ── shipment_tracks 表 ───────────────────────────────
    private void createShipmentTracksTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS shipment_tracks (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                shipment_id BIGINT        NOT NULL,
                location    VARCHAR(100)  DEFAULT NULL,
                description VARCHAR(300)  NOT NULL,
                track_time  DATETIME      DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    // ── addresses 表 ─────────────────────────────────────
    private void createAddressesTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS addresses (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id     BIGINT       NOT NULL,
                name        VARCHAR(50)  NOT NULL,
                phone       VARCHAR(20)  NOT NULL,
                province    VARCHAR(30)  DEFAULT NULL,
                city        VARCHAR(30)  DEFAULT NULL,
                district    VARCHAR(30)  DEFAULT NULL,
                detail      VARCHAR(200) NOT NULL,
                is_default  TINYINT      NOT NULL DEFAULT 0,
                create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    // ── 修复库存为 0 的 SKU ───────────────────────────────
    private void fixZeroStock() {
        int updated = jdbc.update(
            "UPDATE item_skus SET stock = 100 WHERE stock IS NULL OR stock = 0"
        );
        if (updated > 0) {
            System.out.println("[DB Init] 已将 " + updated + " 个库存为 0 的 SKU 补设为 100");
        }
    }

    private void createFavoritesTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS favorites (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                item_id BIGINT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_user_item (user_id, item_id),
                INDEX idx_user (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void createNotificationsTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS notifications (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                title VARCHAR(100) NOT NULL,
                content TEXT,
                type VARCHAR(30) DEFAULT 'system',
                is_read TINYINT DEFAULT 0,
                related_id BIGINT DEFAULT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_user_read (user_id, is_read)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void fixCouponsTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS coupons (
                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                merchant_id  BIGINT       NOT NULL,
                type         VARCHAR(20)  DEFAULT 'fixed',
                title        VARCHAR(100) DEFAULT '',
                discount     DECIMAL(10,2) DEFAULT 0,
                min_amount   DECIMAL(10,2) DEFAULT 0,
                total_count  INT          DEFAULT 0,
                remain_count INT          DEFAULT 0,
                expire_time  DATETIME     DEFAULT NULL,
                created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_merchant (merchant_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS user_coupons (
                id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id   BIGINT      NOT NULL,
                coupon_id BIGINT      NOT NULL,
                status    VARCHAR(20) DEFAULT 'unused',
                claimed_at DATETIME   DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_user_coupon (user_id, coupon_id),
                INDEX idx_user (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        addColumnIfMissing("coupons", "title", "VARCHAR(100) DEFAULT ''");
        addColumnIfMissing("coupons", "total_count", "INT DEFAULT 0");
        addColumnIfMissing("coupons", "remain_count", "INT DEFAULT 0");
        addColumnIfMissing("coupons", "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");
    }

    private void fixImSessionsTable() {
        addColumnIfMissing("im_sessions", "updated_at",
                "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        try {
            jdbc.execute("ALTER TABLE im_messages MODIFY COLUMN sender_type VARCHAR(20) NOT NULL DEFAULT 'user'");
            jdbc.execute("ALTER TABLE im_messages MODIFY COLUMN type VARCHAR(20) NOT NULL DEFAULT 'text'");
        } catch (Exception ignored) {}
    }

    private void fixWarehousesTable() {
        addColumnIfMissing("warehouses", "district", "VARCHAR(50) DEFAULT ''");
    }

    private void initCategoriesTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS categories (
                id        INT AUTO_INCREMENT PRIMARY KEY,
                parent_id INT          NOT NULL DEFAULT 0,
                name      VARCHAR(60)  NOT NULL,
                level     TINYINT      NOT NULL DEFAULT 1,
                sort      INT          NOT NULL DEFAULT 0,
                icon_url  VARCHAR(500) DEFAULT NULL,
                status    TINYINT      NOT NULL DEFAULT 1,
                INDEX idx_parent (parent_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
        if (count != null && count == 0) {
            System.out.println("[DB Init] 初始化默认分类...");
            String[][] defaultCategories = {
                    {"图书", "📚"}, {"电子产品", "📱"}, {"服装", "👕"},
                    {"食品", "🍎"}, {"家居", "🏠"}, {"美妆", "💄"},
                    {"运动", "⚽"}, {"玩具", "🧸"}, {"影音", "🎬"}
            };
            for (int i = 0; i < defaultCategories.length; i++) {
                jdbc.update("INSERT INTO categories (parent_id, name, level, sort, icon_url) VALUES (0, ?, 1, ?, ?)",
                        defaultCategories[i][0], (i + 1) * 10, defaultCategories[i][1]);
            }
            // 二级分类
            String[][] subCategories = {
                    {"图书", "小说"}, {"图书", "文学"}, {"图书", "科技"}, {"图书", "教育"},
                    {"电子产品", "手机"}, {"电子产品", "电脑"}, {"电子产品", "配件"},
                    {"影音", "音乐"}, {"影音", "电影"}, {"影音", "唱片"}
            };
            for (String[] sub : subCategories) {
                try {
                    Integer parentId = jdbc.queryForObject(
                            "SELECT id FROM categories WHERE name = ? AND parent_id = 0", Integer.class, sub[0]);
                    if (parentId != null) {
                        jdbc.update("INSERT INTO categories (parent_id, name, level, sort) VALUES (?, ?, 2, 0)",
                                parentId, sub[1]);
                    }
                } catch (Exception ignored) {}
            }
            System.out.println("[DB Init] 默认分类初始化完成");
        }

        // 给尚未分类的商品根据标题关键词自动分配分类
        Integer unCatCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM items WHERE category_id IS NULL", Integer.class);
        if (unCatCount != null && unCatCount > 0) {
            System.out.println("[DB Init] 发现 " + unCatCount + " 件商品无分类，自动分配中...");
            String[][] rules = {
                    {"书", "图书"}, {"小说", "图书"}, {"文学", "图书"}, {"教程", "图书"}, {"编程", "图书"},
                    {"手机", "电子产品"}, {"电脑", "电子产品"}, {"耳机", "电子产品"}, {"键盘", "电子产品"},
                    {"笔记本", "电子产品"}, {"平板", "电子产品"}, {"充电", "电子产品"}, {"数码", "电子产品"},
                    {"衣", "服装"}, {"裤", "服装"}, {"鞋", "服装"}, {"帽", "服装"}, {"T恤", "服装"},
                    {"零食", "食品"}, {"茶", "食品"}, {"咖啡", "食品"}, {"奶", "食品"},
                    {"沙发", "家居"}, {"床", "家居"}, {"灯", "家居"}, {"桌", "家居"}, {"椅", "家居"},
                    {"口红", "美妆"}, {"面膜", "美妆"}, {"护肤", "美妆"}, {"香水", "美妆"},
                    {"球", "运动"}, {"跑步", "运动"}, {"健身", "运动"}, {"瑜伽", "运动"},
                    {"玩具", "玩具"}, {"积木", "玩具"}, {"娃娃", "玩具"},
                    {"音乐", "影音"}, {"电影", "影音"}, {"唱片", "影音"}, {"蓝牙", "电子产品"}
            };
            for (String[] rule : rules) {
                try {
                    Integer catId = jdbc.queryForObject(
                            "SELECT id FROM categories WHERE name = ? AND parent_id = 0 LIMIT 1",
                            Integer.class, rule[1]);
                    if (catId != null) {
                        jdbc.update("UPDATE items SET category_id = ? WHERE category_id IS NULL AND title LIKE ?",
                                catId, "%" + rule[0] + "%");
                    }
                } catch (Exception ignored) {}
            }
            // 剩余未匹配的随机分配到一级分类
            try {
                List<Integer> catIds = jdbc.queryForList(
                        "SELECT id FROM categories WHERE parent_id = 0", Integer.class);
                if (!catIds.isEmpty()) {
                    int remaining = jdbc.queryForObject(
                            "SELECT COUNT(*) FROM items WHERE category_id IS NULL", Integer.class);
                    if (remaining > 0) {
                        for (int cid : catIds) {
                            jdbc.update(
                                "UPDATE items SET category_id = ? WHERE category_id IS NULL ORDER BY RAND() LIMIT ?",
                                cid, Math.max(1, remaining / catIds.size()));
                        }
                        jdbc.update("UPDATE items SET category_id = ? WHERE category_id IS NULL", catIds.get(0));
                    }
                }
            } catch (Exception ignored) {}
            System.out.println("[DB Init] 商品分类分配完成");
        }
    }

    /**
     * 自动初始化：管理员账号 + 官方自营商家
     * - 创建 platform_admin (role=2, 密码 admin123)
     * - 为该账号创建 merchants 记录（平台自营旗舰店，状态=1 已审核）
     * - 把所有 merchant_id IS NULL 的商品归属到官方自营店
     * - 为无 SKU 的商品创建默认 SKU（库存 100）
     */
    private void initAdminAndOfficialMerchant() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 1. 创建管理员账号（幂等）
        Integer adminExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = 'platform_admin'", Integer.class);
        if (adminExists == null || adminExists == 0) {
            String bcryptPwd = encoder.encode("admin123");
            jdbc.update("INSERT INTO users (username, password, role, status) VALUES (?, ?, 2, 1)",
                    "platform_admin", bcryptPwd);
            System.out.println("[DB Init] 已创建管理员账号 platform_admin (密码 admin123)");
        }

        // 2. 获取管理员 user_id
        Long adminUserId;
        try {
            adminUserId = jdbc.queryForObject(
                    "SELECT id FROM users WHERE username = 'platform_admin'", Long.class);
        } catch (Exception e) {
            System.err.println("[DB Init] 无法查询管理员账号: " + e.getMessage());
            return;
        }

        // 3. 创建官方自营商家记录（幂等）
        Integer merchantExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM merchants WHERE user_id = ?", Integer.class, adminUserId);
        if (merchantExists == null || merchantExists == 0) {
            jdbc.update("INSERT INTO merchants (user_id, shop_name, shop_desc, status) VALUES (?, ?, ?, 1)",
                    adminUserId, "平台自营旗舰店", "官方直营，品质保证，七天无理由退换");
            System.out.println("[DB Init] 已创建官方自营商家");
        }

        // 4. 获取官方商家 merchant_id
        Long officialMerchantId;
        try {
            officialMerchantId = jdbc.queryForObject(
                    "SELECT id FROM merchants WHERE user_id = ?", Long.class, adminUserId);
        } catch (Exception e) {
            System.err.println("[DB Init] 无法查询官方商家: " + e.getMessage());
            return;
        }

        // 5. 把所有无归属的商品划归官方自营店
        int assigned = jdbc.update(
                "UPDATE items SET merchant_id = ? WHERE merchant_id IS NULL", officialMerchantId);
        if (assigned > 0) {
            System.out.println("[DB Init] 已将 " + assigned + " 个无归属商品分配给官方自营店");
        }

        // 6. 为官方商家的商品（无 SKU 的）创建默认 SKU，库存 100
        List<Map<String, Object>> noSkuItems = jdbc.queryForList(
                "SELECT i.id, i.price, i.image_url FROM items i " +
                "WHERE i.merchant_id = ? AND NOT EXISTS (SELECT 1 FROM item_skus s WHERE s.item_id = i.id)",
                officialMerchantId);
        for (Map<String, Object> item : noSkuItems) {
            Object price = item.get("price");
            if (price == null) continue;
            jdbc.update("INSERT INTO item_skus (item_id, spec_json, price, stock, image_url) VALUES (?, ?, ?, 100, ?)",
                    item.get("id"), "{\"规格\":\"默认\"}", price, item.get("image_url"));
        }
        if (!noSkuItems.isEmpty()) {
            System.out.println("[DB Init] 为官方商品创建了 " + noSkuItems.size() + " 个默认 SKU (库存 100)");
        }
    }

    // ── 工具：检查列是否存在，不存在则 ALTER TABLE 添加 ──
    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column
            );
            if (count == null || count == 0) {
                jdbc.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
                System.out.println("[DB Init] ALTER TABLE " + table + " ADD COLUMN " + column);
            }
        } catch (Exception e) {
            System.err.println("[DB Init] 跳过 " + table + "." + column + ": " + e.getMessage());
        }
    }
}
