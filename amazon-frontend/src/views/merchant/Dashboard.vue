<!-- amazon-frontend/src/views/merchant/Dashboard.vue -->
<template>
  <div>
    <div class="page-title">数据看板</div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 饼图区域 -->
    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :span="12">
        <el-card>
          <template #header><span class="chart-title">商品分类分布</span></template>
          <div class="pie-container">
            <div class="pie-chart">
              <svg viewBox="0 0 200 200">
                <circle v-for="(seg, i) in catPieSegments" :key="'cat-'+i"
                  cx="100" cy="100" r="80" fill="none"
                  :stroke="seg.color" stroke-width="40"
                  :stroke-dasharray="seg.dashArray" :stroke-dashoffset="seg.dashOffset"
                  :style="{ transition: 'all 0.6s ease ' + (i * 0.1) + 's' }" />
              </svg>
            </div>
            <div class="pie-legend">
              <div v-for="(item, i) in catPieData" :key="'cl-'+i" class="legend-item">
                <span class="legend-dot" :style="{ background: PIE_COLORS[i % PIE_COLORS.length] }" />
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-val">{{ item.value }}</span>
              </div>
              <div v-if="catPieData.length === 0" class="legend-empty">暂无数据</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="chart-title">订单状态分布</span></template>
          <div class="pie-container">
            <div class="pie-chart">
              <svg viewBox="0 0 200 200">
                <circle v-for="(seg, i) in orderPieSegments" :key="'ord-'+i"
                  cx="100" cy="100" r="80" fill="none"
                  :stroke="seg.color" stroke-width="40"
                  :stroke-dasharray="seg.dashArray" :stroke-dashoffset="seg.dashOffset"
                  :style="{ transition: 'all 0.6s ease ' + (i * 0.1) + 's' }" />
              </svg>
            </div>
            <div class="pie-legend">
              <div v-for="(item, i) in orderPieData" :key="'ol-'+i" class="legend-item">
                <span class="legend-dot" :style="{ background: PIE_COLORS[i % PIE_COLORS.length] }" />
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-val">{{ item.value }}</span>
              </div>
              <div v-if="orderPieData.length === 0" class="legend-empty">暂无数据</div>
            </div>
          </div>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, List, Setting } from '@element-plus/icons-vue'
import { getDashboardStats } from '../../api/dashboard'

const router = useRouter()
const PIE_COLORS = ['#409eff','#67c23a','#e6a23c','#f56c6c','#909399','#b37feb','#36cfc9','#ff85c0']
const CIRC = 2 * Math.PI * 80

const statCards = ref([
  { label: '在售商品数', value: 0, color: '#409eff' },
  { label: '今日订单数', value: 0, color: '#e6a23c' },
  { label: '待发货订单', value: 0, color: '#f56c6c' },
  { label: '本月销售额', value: '¥0.00', color: '#67c23a' },
])
const catPieData = ref([])
const orderPieData = ref([])

const buildSegments = (data) => {
  const total = data.reduce((s, d) => s + (d.value || 0), 0)
  if (total === 0) return []
  let offset = 0
  return data.map((d, i) => {
    const pct = d.value / total
    const seg = { color: PIE_COLORS[i % PIE_COLORS.length], dashArray: `${pct * CIRC} ${CIRC}`, dashOffset: -offset }
    offset += pct * CIRC
    return seg
  })
}

const catPieSegments = computed(() => buildSegments(catPieData.value))
const orderPieSegments = computed(() => buildSegments(orderPieData.value))

onMounted(async () => {
  const { data: res } = await getDashboardStats()
  if (res.code === 1) {
    const s = res.data
    statCards.value[0].value = s.onSaleItems || 0
    statCards.value[1].value = s.todayOrders || 0
    statCards.value[2].value = s.pendingOrders || 0
    statCards.value[3].value = '¥' + (s.monthRevenue || '0.00')
    catPieData.value = (s.categoryDistribution || []).map(r => ({ name: r.name, value: Number(r.value) }))
    orderPieData.value = (s.orderStatusDistribution || []).map(r => ({ name: r.name, value: Number(r.value) }))
  }
})
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: bold; color: #303133; margin-bottom: 20px; }
.stat-row { margin-bottom: 8px; }
.stat-card { text-align: center; cursor: default; }
.stat-value { font-size: 32px; font-weight: bold; margin-bottom: 8px; }
.stat-label { font-size: 14px; color: #909399; }

.chart-title { font-size: 15px; font-weight: 600; color: #303133; }
.pie-container { display: flex; align-items: center; gap: 24px; min-height: 180px; }
.pie-chart { width: 160px; height: 160px; flex-shrink: 0; }
.pie-chart svg { width: 100%; height: 100%; transform: rotate(-90deg); }
.pie-legend { flex: 1; }
.legend-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.legend-name { flex: 1; color: #606266; }
.legend-val { font-weight: 600; color: #303133; }
.legend-empty { color: #c0c4cc; font-size: 13px; }

.quick-card { text-align: center; cursor: pointer; padding: 20px 0; transition: 0.2s; }
.quick-card:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.quick-card div { margin-top: 10px; color: #606266; font-size: 14px; }
</style>