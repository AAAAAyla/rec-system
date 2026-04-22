package com.ly.springquickstart.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
