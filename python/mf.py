import pymysql
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity
import redis
import json
import warnings

warnings.filterwarnings('ignore')

print("推荐系统启动...")

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '141414',
    'db': 'rec_system',
    'charset': 'utf8mb4'
}

db = pymysql.connect(**DB_CONFIG)
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

# 读取评分数据（兼容你的 rating 表）
print("读取评分数据...")
query = "SELECT user_id, item_id, score FROM rating"
df = pd.read_sql(query, db)

if df.empty:
    print("评分表为空，请先运行 import_data.py")
    db.close()
    exit()

user_item_matrix = df.pivot_table(index='user_id', columns='item_id', values='score', fill_value=0)
print(f"矩阵：{user_item_matrix.shape[0]} 用户 x {user_item_matrix.shape[1]} 商品")

# 全站热门商品（兜底用）
global_popular = df.groupby('item_id')['score'].mean().sort_values(ascending=False).index.tolist()

# 计算用户相似度
print("计算用户相似度...")
user_sim = cosine_similarity(user_item_matrix)
user_sim_df = pd.DataFrame(user_sim, index=user_item_matrix.index, columns=user_item_matrix.index)

# 为所有用户生成推荐（不只前 10 个）
print("为所有用户生成推荐...")
all_users = user_item_matrix.index.tolist()
batch_size = 100
generated = 0

for i in range(0, len(all_users), batch_size):
    batch = all_users[i:i + batch_size]

    for target_user in batch:
        user_bought = set(df[df['user_id'] == target_user]['item_id'].tolist())

        # 协同过滤：找 5 个最相似的用户
        similar_users = user_sim_df[target_user].sort_values(ascending=False)[1:6].index
        neighbor_items = df[df['user_id'].isin(similar_users)]
        recommendations = neighbor_items[~neighbor_items['item_id'].isin(user_bought)]
        top_items = recommendations.groupby('item_id')['score'].mean().sort_values(ascending=False).head(10)
        rec_ids = top_items.index.tolist()

        # 兜底：用热门商品补齐 10 个
        if len(rec_ids) < 10:
            for item in global_popular:
                if item not in user_bought and item not in rec_ids:
                    rec_ids.append(item)
                if len(rec_ids) >= 10:
                    break

        # 写入 Redis（key 格式和 ItemController.getRecommend() 一致）
        redis_key = f"rec:user:{target_user}"
        r.set(redis_key, json.dumps(rec_ids))
        generated += 1

    print(f"  已处理 {min(i + batch_size, len(all_users))}/{len(all_users)} 个用户")

db.close()
print(f"完成！共为 {generated} 个用户生成推荐结果")