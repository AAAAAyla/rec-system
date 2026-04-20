<!-- amazon-frontend/src/views/merchant/Dashboard.vue -->
<template>
  <div>
    <div class="page-title">数据看板</div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">
            {{ card.value }}
          </div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :span="8">
        <el-card class="quick-card" @click="router.push('/merchant/items/new')">
          <el-icon size="32" color="#409eff"><Plus /></el-icon>
          <div>发布新商品</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="quick-card" @click="router.push('/merchant/orders')">
          <el-icon size="32" color="#e6a23c"><List /></el-icon>
          <div>查看待发货订单</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="quick-card" @click="router.push('/merchant/shop')">
          <el-icon size="32" color="#67c23a"><Setting /></el-icon>
          <div>编辑店铺信息</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, List, Setting } from '@element-plus/icons-vue'
import { getMerchantItems } from '../../api/item'

const router = useRouter()

const statCards = ref([
  { label: '在售商品数', value: 0, color: '#409eff' },
  { label: '今日订单数', value: 0, color: '#e6a23c' },
  { label: '待发货订单', value: 0, color: '#f56c6c' },
  { label: '累计销售额', value: '¥0.00', color: '#67c23a' },
])

onMounted(async () => {
  // 用商品列表接口拿在售数量
  const { data: res } = await getMerchantItems({ pageNum: 1, pageSize: 1 })
  if (res.code === 1) {
    statCards.value[0].value = res.data.total
  }
  // Week 3 接入订单接口后补充其余卡片数据
})
</script>

<style scoped>
.page-title {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
}
.stat-row { margin-bottom: 8px; }
.stat-card { text-align: center; cursor: default; }
.stat-value { font-size: 32px; font-weight: bold; margin-bottom: 8px; }
.stat-label { font-size: 14px; color: #909399; }
.quick-card {
  text-align: center;
  cursor: pointer;
  padding: 20px 0;
  transition: 0.2s;
}
.quick-card:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.quick-card div { margin-top: 10px; color: #606266; font-size: 14px; }
</style>