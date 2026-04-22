<!-- amazon-frontend/src/views/buyer/Orders.vue -->
<template>
  <div class="orders-page">
    <div class="page-title">我的订单</div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="已完成" name="3" />
    </el-tabs>

    <div v-loading="loading">
      <el-empty v-if="!orders.length && !loading" description="暂无订单" />

      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small">{{ statusText(order.status) }}</el-tag>
        </div>

        <div v-for="item in order.orderItems" :key="item.id" class="order-item"
             @click="router.push(`/orders/${order.id}`)">
          <img :src="item.imageUrl || 'https://via.placeholder.com/50'" class="item-img" />
          <div class="item-info">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-spec" v-if="item.specJson">{{ formatSpec(item.specJson) }}</div>
          </div>
          <div class="item-price">¥{{ item.price }} x{{ item.quantity }}</div>
        </div>

        <div class="order-footer">
          <span class="order-total">实付：<b>¥{{ order.payAmount }}</b></span>
          <div class="order-actions">
            <el-button v-if="order.status === 0" type="danger" size="small" @click="doPay(order)">去支付</el-button>
            <el-button v-if="order.status === 0" size="small" @click="doCancel(order)">取消订单</el-button>
            <el-button v-if="order.status === 2" type="success" size="small" @click="doConfirm(order)">确认收货</el-button>
            <el-button v-if="order.status === 1 || order.status === 2" size="small" @click="doRefund(order)">申请退款</el-button>
            <el-button size="small" @click="router.push(`/orders/${order.id}`)">查看详情</el-button>
          </div>
        </div>
      </div>

      <el-pagination
          v-if="total > 0"
          v-model:current-page="pageNum"
          :total="total"
          :page-size="pageSize"
          layout="prev, pager, next"
          style="margin-top:20px;justify-content:center;display:flex"
          @current-change="loadOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyOrders, mockPay, cancelOrder, confirmReceive, applyRefund } from '../../api/order'

const router   = useRouter()
const orders   = ref([])
const loading  = ref(false)
const total    = ref(0)
const pageNum  = ref(1)
const pageSize = 10
const activeTab = ref('all')

const STATUS_MAP = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '待收货', type: '' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'info' },
  5: { text: '退款中', type: 'danger' },
  6: { text: '已退款', type: 'info' },
}
const statusText = (s) => STATUS_MAP[s]?.text || '未知'
const statusType = (s) => STATUS_MAP[s]?.type || 'info'

const formatSpec = (specJson) => {
  try { return Object.entries(JSON.parse(specJson)).map(([k,v]) => `${k}:${v}`).join(' | ') }
  catch { return specJson }
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (activeTab.value !== 'all') params.status = parseInt(activeTab.value)
    const { data: res } = await getMyOrders(params)
    if (res.code === 1) {
      orders.value = res.data.rows
      total.value  = res.data.total
    }
  } finally { loading.value = false }
}

const onTabChange = () => { pageNum.value = 1; loadOrders() }

const doPay = async (order) => {
  await ElMessageBox.confirm(`确认支付 ¥${order.payAmount}？（模拟支付）`, '支付确认')
  const { data: res } = await mockPay(order.orderNo)
  if (res.code === 1) { ElMessage.success('支付成功！'); loadOrders() }
  else ElMessage.error(res.msg)
}

const doCancel = async (order) => {
  await ElMessageBox.confirm('确定取消该订单？', '取消订单', { type: 'warning' })
  const { data: res } = await cancelOrder(order.id, '买家主动取消')
  if (res.code === 1) { ElMessage.success('已取消'); loadOrders() }
  else ElMessage.error(res.msg)
}

const doConfirm = async (order) => {
  await ElMessageBox.confirm('确认已收到商品？', '确认收货')
  const { data: res } = await confirmReceive(order.id)
  if (res.code === 1) { ElMessage.success('已确认收货'); loadOrders() }
  else ElMessage.error(res.msg)
}

const doRefund = async (order) => {
  await ElMessageBox.confirm('确定申请退款？', '退款申请', { type: 'warning' })
  const { data: res } = await applyRefund(order.id)
  if (res.code === 1) { ElMessage.success('退款申请已提交'); loadOrders() }
  else ElMessage.error(res.msg)
}

onMounted(loadOrders)
</script>

<style scoped>
.orders-page { max-width: 900px; margin: 20px auto; padding: 0 16px; }
.page-title { font-size: 24px; font-weight: bold; margin-bottom: 16px; }
.order-card { background: #fff; border-radius: 8px; margin-bottom: 16px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.order-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: #fafafa; border-bottom: 1px solid #f0f0f0; }
.order-no { font-size: 13px; color: #999; }
.order-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; cursor: pointer; transition: 0.15s; }
.order-item:hover { background: #f9f9f9; }
.item-img { width: 50px; height: 50px; object-fit: contain; border-radius: 4px; border: 1px solid #eee; }
.item-info { flex: 1; min-width: 0; }
.item-title { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-spec { font-size: 12px; color: #999; margin-top: 2px; }
.item-price { font-size: 13px; color: #666; white-space: nowrap; }
.order-footer { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.order-total { font-size: 14px; color: #333; }
.order-total b { color: #f56c6c; font-size: 18px; }
.order-actions { display: flex; gap: 8px; }
</style>