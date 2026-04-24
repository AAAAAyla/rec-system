<template>
  <div class="fav-page">
    <h2>我的收藏</h2>
    <el-empty v-if="list.length === 0" description="暂无收藏" />
    <el-row :gutter="16">
      <el-col :xs="12" :sm="8" :md="6" v-for="item in list" :key="item.id">
        <el-card shadow="hover" class="fav-card" @click="$router.push('/product/' + item.item_id)">
          <el-image :src="item.image_url" fit="cover" style="width:100%;height:180px;border-radius:4px" />
          <div class="fav-title">{{ item.title }}</div>
          <div class="fav-bottom">
            <span class="fav-price">¥{{ item.price }}</span>
            <el-button type="danger" text size="small" @click.stop="handleRemove(item.item_id)">取消收藏</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-pagination v-if="total > pageSize" layout="prev, pager, next" :total="total"
      :page-size="pageSize" v-model:current-page="pageNum" @current-change="load" style="margin-top:20px;justify-content:center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getFavorites, removeFavorite } from '../../api/favorite'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20

const load = async () => {
  const { data: res } = await getFavorites({ pageNum: pageNum.value, pageSize })
  if (res.code === 1) { list.value = res.data.rows; total.value = res.data.total }
}

const handleRemove = async (itemId) => {
  await removeFavorite(itemId)
  ElMessage.success('已取消收藏')
  load()
}

onMounted(load)
</script>

<style scoped>
.fav-page { padding: 20px; max-width: 1200px; margin: 0 auto; }
.fav-card { cursor: pointer; margin-bottom: 16px; }
.fav-title { font-size: 14px; margin-top: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fav-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.fav-price { color: #f56c6c; font-weight: bold; font-size: 16px; }
</style>
