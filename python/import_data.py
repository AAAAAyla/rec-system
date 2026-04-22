import pymysql
import gzip
import json
import os

# ================= 配置 =================
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '141414',
    'db': 'rec_system',
    'charset': 'utf8mb4'
}

# 要导入的数据集（放在 ./raw/ 目录下）
REVIEW_FILES = [
    '../dataset/reviews_Books_5.json.gz',
    '../dataset/reviews_CDs_and_Vinyl_5.json.gz',
    '../dataset/reviews_Movies_and_TV_5.json.gz',
]

# 每个文件最多导入多少条（0 = 不限制）
MAX_PER_FILE = 10000

# ================= 主逻辑 =================
connection = pymysql.connect(**DB_CONFIG)

user_id_map = {}
item_id_map = {}
current_user_id = 1
current_item_id = 1

users_batch = []
items_batch = []
ratings_batch = []

# 先查数据库现有的最大 ID，避免主键冲突
with connection.cursor() as cursor:
    cursor.execute("SELECT MAX(id) FROM users")
    row = cursor.fetchone()
    if row[0]: current_user_id = row[0] + 1

    cursor.execute("SELECT MAX(id) FROM items")
    row = cursor.fetchone()
    if row[0]: current_item_id = row[0] + 1

    # 加载已有的 amazon_item_id 映射
    cursor.execute("SELECT id, amazon_item_id FROM items WHERE amazon_item_id IS NOT NULL")
    for r in cursor.fetchall():
        item_id_map[r[1]] = r[0]

    cursor.execute("SELECT id, username FROM users")
    for r in cursor.fetchall():
        user_id_map[r[1]] = r[0]

print(f"数据库现有 {len(user_id_map)} 个用户，{len(item_id_map)} 个商品")

domain_map = {
    'reviews_Books': 'book',
    'reviews_CDs': 'cd',
    'reviews_Movies': 'movie',
}

def get_domain(filename):
    for key, val in domain_map.items():
        if key in filename:
            return val
    return 'unknown'

for filepath in REVIEW_FILES:
    if not os.path.exists(filepath):
        print(f"文件不存在，跳过：{filepath}")
        continue

    domain = get_domain(filepath)
    print(f"\n正在解析 {filepath}（域={domain}）...")
    count = 0

    with gzip.open(filepath, 'rt', encoding='utf-8') as f:
        for line in f:
            if MAX_PER_FILE and count >= MAX_PER_FILE:
                break

            data = json.loads(line)
            amazon_user = data.get('reviewerID')
            amazon_item = data.get('asin')
            score = data.get('overall')

            if not amazon_user or not amazon_item or score is None:
                continue

            # 用户去重
            if amazon_user not in user_id_map:
                user_id_map[amazon_user] = current_user_id
                users_batch.append((amazon_user, '123456', 0, 1))
                current_user_id += 1

            # 商品去重
            if amazon_item not in item_id_map:
                item_id_map[amazon_item] = current_item_id
                # 插入 items 表，适配 MVP 字段：status=1, price=随机, stock=999
                items_batch.append((amazon_item, 1, 999))
                current_item_id += 1

            real_user_id = user_id_map[amazon_user]
            real_item_id = item_id_map[amazon_item]
            ratings_batch.append((real_user_id, real_item_id, score, domain))
            count += 1

    print(f"  解析完成，本文件 {count} 条")

print(f"\n总计：{len(users_batch)} 新用户，{len(items_batch)} 新商品，{len(ratings_batch)} 条评分")
print("写入 MySQL...")

try:
    with connection.cursor() as cursor:
        if users_batch:
            cursor.executemany(
                "INSERT IGNORE INTO users (username, password, role, status) VALUES (%s, %s, %s, %s)",
                users_batch
            )

        if items_batch:
            cursor.executemany(
                "INSERT IGNORE INTO items (amazon_item_id, status, stock) VALUES (%s, %s, %s)",
                items_batch
            )

        if ratings_batch:
            # 兼容你现有的 rating 表
            cursor.executemany(
                "INSERT INTO rating (user_id, item_id, score, domain) VALUES (%s, %s, %s, %s)",
                ratings_batch
            )

    connection.commit()
    print(f"写入完成！")

except Exception as e:
    print(f"错误：{e}")
    connection.rollback()
finally:
    connection.close()