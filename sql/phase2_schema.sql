-- Phase 2-4: New tables for favorites, notifications, coupons enhancement

CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_item (user_id, item_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add missing columns to coupons if they don't exist
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS title VARCHAR(100) DEFAULT '' AFTER merchant_id;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS total_count INT DEFAULT 0 AFTER min_amount;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS remain_count INT DEFAULT 0 AFTER total_count;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS created_at DATETIME DEFAULT CURRENT_TIMESTAMP;

-- Add missing columns to im_sessions
ALTER TABLE im_sessions ADD COLUMN IF NOT EXISTS updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add district column to warehouses if not exists
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS district VARCHAR(50) DEFAULT '' AFTER city;

-- Add created_at to users if not exists
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at DATETIME DEFAULT CURRENT_TIMESTAMP;
