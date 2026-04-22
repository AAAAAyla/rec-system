<!-- amazon-frontend/src/views/buyer/OrderDetail.vue -->
<template>
  <div class="detail-page">
    <el-page-header @back="router.push('/orders')" title="返回订单列表">
      <template #content>订单详情</template>
    </el-page-header>

    <div v-loading="loading" v-if="order" style="margin-top:20px">
      <!-- 状态 -->
      <el-card class="section">
        <div class="status-bar">
          <el-tag :type="statusType(order.status)" size="large">{{ statusText(order.status) }}</el-tag>
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span class="order-time">下单时间：{{ order.createTime }}</span>
        </div>
        <div class="action-bar">
          <el-button v-if="order.status===0" type="danger" @click="doPay">去支付</el-button>
          <el-button v-if="order.status===0" @click="doCancel">取消订单</el-button>
          <el-button v-if="order.status===2" type="success" @click="doConfirm">确认收货</el-button>
        </div>
      </el-card>

      <!-- 物流信息 -->
      <el-card v-if="order.shipment" class="section">
        <template #header><b>物流信息</b></template>
        <div class="logistics-info">
          <span>{{ order.shipment.expressCompany }}</span>
          <span>单号：{{ order.shipment.trackingNo }}</span>
        </div>
        <el-timeline style="margin-top:16px">
          <el-timeline-item
              v-for="track in order.shipment.tracks"
              :key="track.id"
              :timestamp="track.trackTime"
              placement="top"
          >
            <div>{{ track.description }}</div>
            <div v-if="track.location" class="track-location">{{ track.location }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <!-- 收货地址 -->
      <el-card class="section">
        <template #header><b>收货地址</b></template>
        <div v-if="addrObj">
          <div><b>{{ addrObj.name }}</b> {{ addrObj.phone }}</div>
          <div class="addr-text">{{ addrObj.province }} {{ addrObj.city }} {{ addrObj.district || '' }} {{ addrObj.detail }}</div>
        </div>
      </el-card>

      <!-- 商品明细 -->
      <el-card class="section">
        <template #header><b>商品明细</b></template>
        <div v-for="item in order.orderItems" :key="item.id" class="order-item">
          <img :src="item.imageUrl || 'https://via.placeholder.com/50'" class="item-img" />
          <div class="item-info">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-spec" v-if="item.specJson">{{ formatSpec(item.specJson) }}</div>
          </div>
          <div class="item-price">¥{{ item.price }} x{{ item.quantity }}</div>
          <div class="item-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
        </div>
        <div class="order-summary">
          <div>商品总价：¥{{ order.totalAmount }}</div>
          <div>运费：¥{{ order.freightAmount }}</div>
          <div class="pay-total">实付金额：<b>¥{{ order.payAmount }}</b></div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, mockPay, cancelOrder, confirmReceive } from '../../api/order'

const route  = useRoute()
const router = useRouter()
const order   = ref(null)
const loading = ref(false)

const STATUS_MAP = {
  0:'待付款', 1:'待发货', 2:'待收货', 3:'已完成', 4:'已取消', 5:'退款中', 6:'已退款'
}
const TYPE_MAP = { 0:'warning', 1:'primary', 2:'', 3:'success', 4:'info', 5:'danger', 6:'info' }
const statusText = (s) => STATUS_MAP[s] || '未知'
const statusType = (s) => TYPE_MAP[s] || 'info'

const addrObj = computed(() => {
  if (!order.value?.addressSnapshot) return null
  try { return JSON.parse(order.value.addressSnapshot) } catch { return null }
})

const formatSpec = (s) => {
  try { return Object.entries(JSON.parse(s)).map(([k,v])=>`${k}:${v}`).join(' | ') }
  catch { return s }
}

const load = async () => {
  loading.value = true
  try {
    const { data: res } = await getOrderDetail(route.params.id)
    if (res.code === 1) order.value = res.data
    else ElMessage.error(res.msg)
  } finally { loading.value = false }
}

const doPay = async () => {
  await ElMessageBox.confirm(`确认支付 ¥${order.value.payAmount}？`, '模拟支付')
  const { data: res } = await mockPay(order.value.orderNo)
  if (res.code === 1) { ElMessage.success('支付成功'); load() }
  else ElMessage.error(res.msg)
}

const doCancel = async () => {
  await ElMessageBox.confirm('确定取消？', '取消订单', { type: 'warning' })
  const { data: res } = await cancelOrder(order.value.id, '买家取消')
  if (res.code === 1) { ElMessage.success('已取消'); load() }
  else ElMessage.error(res.msg)
}

const doConfirm = async () => {
  await ElMessageBox.confirm('确认已收到商品？', '确认收货')
  const { data: res } = await confirmReceive(order.value.id)
  if (res.code === 1) { ElMessage.success('已确认收货'); load() }
  else ElMessage.error(res.msg)
}

onMounted(load)
</script>

<style scoped>
.detail-page { max-width: 900px; margin: 20px auto; padding: 0 16px; }
.section { margin-bottom: 16px; }
.status-bar { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.order-no, .order-time { font-size: 13px; color: #999; }
.action-bar { margin-top: 12px; display: flex; gap: 8px; }
.logistics-info { font-size: 14px; color: #333; display: flex; gap: 20px; }
.track-location { font-size: 12px; color: #999; }
.addr-text { color: #666; margin-top: 4px; }
.order-item { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.order-item:last-child { border-bottom: none; }
.item-img { width: 50px; height: 50px; object-fit: contain; border-radius: 4px; border: 1px solid #eee; }
.item-info { flex: 1; }
.item-title { font-size: 14px; }
.item-spec { font-size: 12px; color: #999; }
.item-price { color: #666; font-size: 13px; }
.item-subtotal { width: 90px; text-align: right; color: #f56c6c; font-weight: bold; }
.order-summary { margin-top: 16px; text-align: right; font-size: 14px; color: #666; line-height: 2; }
.pay-total b { color: #f56c6c; font-size: 20px; }
</style>