import pymysql
import gzip
import random

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '141414',
    'db': 'rec_system',
    'charset': 'utf8mb4'
}

META_FILES = [
    '../dataset/meta_Books.json.gz',
    '../dataset/meta_CDs_and_Vinyl.json.gz',
    '../dataset/meta_Movies_and_TV.json.gz',
]

# 分类映射：把亚马逊分类映射到你的 categories 表 ID
CATEGORY_KEYWORDS = {
    'book': 10,     # 对应你 categories 表里的分类 ID
    'music': 12,
    'movie': 11,
    'cd': 12,
    'vinyl': 12,
}

def guess_category(categories_raw, title):
    """根据亚马逊分类或标题猜测分类 ID"""
    text = str(categories_raw).lower() + ' ' + (title or '').lower()
    for keyword, cat_id in CATEGORY_KEYWORDS.items():
        if keyword in text:
            return cat_id
    return None

connection = pymysql.connect(**DB_CONFIG)

try:
    with connection.cursor() as cursor:
        cursor.execute("SELECT id, amazon_item_id FROM items WHERE amazon_item_id IS NOT NULL")
        item_map = {row[1]: row[0] for row in cursor.fetchall()}
        valid_asins = set(item_map.keys())
        print(f"数据库有 {len(valid_asins)} 个商品等待匹配")

        update_batch = []

        for meta_file in META_FILES:
            print(f"\n扫描 {meta_file}...")
            match_count = 0

            try:
                with gzip.open(meta_file, 'rt', encoding='utf-8') as f:
                    for line in f:
                        try:
                            data = eval(line)
                        except:
                            continue

                        asin = data.get('asin')
                        if asin not in valid_asins:
                            continue

                        title = data.get('title', '未知商品')
                        imUrl = data.get('imUrl', '')
                        price_str = data.get('price', '')
                        categories_raw = data.get('categories', [])

                        # 解析价格
                        price = 0.0
                        if price_str:
                            try:
                                price = float(str(price_str).replace('$', '').replace(',', '').strip())
                            except:
                                price = round(random.uniform(5, 200), 2)
                        else:
                            price = round(random.uniform(5, 200), 2)

                        category_id = guess_category(categories_raw, title)

                        update_batch.append((
                            title[:200],          # title
                            imUrl,                # image_url
                            price,                # price
                            category_id,          # category_id
                            asin                  # WHERE amazon_item_id = ?
                        ))
                        match_count += 1

                        if match_count >= len(valid_asins):
                            break

            except FileNotFoundError:
                print(f"  文件不存在，跳过")
                continue

            print(f"  匹配到 {match_count} 个商品")

        print(f"\n总计更新 {len(update_batch)} 个商品")

        if update_batch:
            cursor.executemany(
                """UPDATE items
                   SET title=%s, image_url=%s, price=%s, category_id=%s
                   WHERE amazon_item_id=%s""",
                update_batch
            )
            connection.commit()
            print("更新完成！")

        # 给没有标题的商品设一个默认标题
        cursor.execute("UPDATE items SET title=CONCAT('商品-', id) WHERE title IS NULL OR title=''")
        connection.commit()
        print("兜底标题填充完成")

except Exception as e:
    print(f"错误：{e}")
    connection.rollback()
finally:
    connection.close()