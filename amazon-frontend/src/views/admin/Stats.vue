<template>
  <div>
    <h2>平台数据统计</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAdminStats } from '../../api/admin'

const stats = ref({})

const cards = computed(() => [
  { label: '总用户数', value: stats.value.totalUsers || 0 },
  { label: '商家数', value: stats.value.totalMerchants || 0 },
  { label: '在售商品', value: stats.value.totalItems || 0 },
  { label: '总订单数', value: stats.value.totalOrders || 0 },
  { label: '总 GMV', value: '¥' + (stats.value.totalGMV || 0) },
  { label: '今日订单', value: stats.value.todayOrders || 0 },
  { label: '今日 GMV', value: '¥' + (stats.value.todayGMV || 0) },
])

onMounted(async () => {
  const { data: res } = await getAdminStats()
  if (res.code === 1) stats.value = res.data
})
</script>

<style scoped>
.stat-card { text-align: center; margin-bottom: 20px; }
.stat-value { font-size: 28px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
