<!-- amazon-frontend/src/views/buyer/Checkout.vue -->
<template>
  <div class="checkout-page">
    <div class="page-title">确认订单</div>

    <!-- 收货地址 -->
    <el-card class="section-card">
      <template #header><b>收货地址</b></template>
      <div v-if="selectedAddress" class="selected-addr">
        <span class="addr-tag">{{ selectedAddress.name }} {{ selectedAddress.phone }}</span>
        <span class="addr-text">{{ formatAddr(selectedAddress) }}</span>
        <el-button text type="primary" @click="showAddrPicker = true">更换</el-button>
      </div>
      <div v-else>
        <el-button type="primary" @click="showAddrPicker = true">选择收货地址</el-button>
      </div>
    </el-card>

    <!-- 商品清单 -->
    <el-card class="section-card">
      <template #header><b>商品清单</b></template>
      <div v-for="item in checkoutItems" :key="item.skuId || item.itemId" class="order-item">
        <img :src="item.imageUrl || 'https://via.placeholder.com/60'" class="item-img" />
        <div class="item-info">
          <div class="item-title">{{ item.title }}</div>
          <div class="item-spec" v-if="item.specJson">{{ formatSpec(item.specJson) }}</div>
        </div>
        <div class="item-price">¥{{ item.price }}</div>
        <div class="item-qty">x{{ item.quantity }}</div>
        <div class="item-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
      </div>
    </el-card>

    <!-- 备注 -->
    <el-card class="section-card">
      <template #header><b>订单备注</b></template>
      <el-input v-model="remark" placeholder="选填：给商家留言" maxlength="200" show-word-limit />
    </el-card>

    <!-- 底部结算栏 -->
    <div class="checkout-footer">
      <div class="footer-info">
        共 <b>{{ totalCount }}</b> 件商品，合计：
        <span class="total-price">¥{{ totalPrice }}</span>
      </div>
      <el-button type="danger" size="large" :loading="submitting" :disabled="!selectedAddress" @click="submitOrder">
        提交订单
      </el-button>
    </div>

    <!-- 地址选择弹窗 -->
    <el-dialog v-model="showAddrPicker" title="选择收货地址" width="600px">
      <div v-loading="addrLoading">
        <el-empty v-if="!addressList.length" description="暂无地址">
          <el-button type="primary" @click="$router.push('/addresses?from=checkout')">去添加</el-button>
        </el-empty>
        <div v-for="addr in addressList" :key="addr.id"
             :class="['picker-item', { active: selectedAddress?.id === addr.id }]"
             @click="pickAddr(addr)">
          <div><b>{{ addr.name }}</b> {{ addr.phone }}
            <el-tag v-if="addr.isDefault" size="small" type="success">默认</el-tag>
          </div>
          <div class="picker-detail">{{ formatAddr(addr) }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAddresses } from '../../api/address'
import { createOrder } from '../../api/order'
import { useCartStore } from '../../store/cartStore'

const router    = useRouter()
const cartStore = useCartStore()

const checkoutItems   = ref([])
const checkoutIndexes = ref([])
const remark          = ref('')
const submitting      = ref(false)
const showAddrPicker  = ref(false)
const addrLoading     = ref(false)
const addressList     = ref([])
const selectedAddress = ref(null)

const totalCount = computed(() => checkoutItems.value.reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => checkoutItems.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2))

const formatAddr = (a) => `${a.province} ${a.city} ${a.district || ''} ${a.detail}`
const formatSpec = (specJson) => {
  try { return Object.entries(JSON.parse(specJson)).map(([k,v]) => `${k}:${v}`).join(' | ') }
  catch { return specJson }
}

const pickAddr = (addr) => {
  selectedAddress.value = addr
  showAddrPicker.value = false
}

const loadAddresses = async () => {
  addrLoading.value = true
  try {
    const { data: res } = await getAddresses()
    if (res.code === 1) {
      addressList.value = res.data
      // 自动选中默认地址
      const def = res.data.find(a => a.isDefault === 1)
      if (def && !selectedAddress.value) selectedAddress.value = def
    }
  } finally { addrLoading.value = false }
}

const submitOrder = async () => {
  if (!selectedAddress.value) return ElMessage.warning('请选择收货地址')
  submitting.value = true
  try {
    const cartItems = checkoutItems.value.map(item => ({
      itemId:   item.itemId || item.id,
      skuId:    item.skuId || null,
      quantity: item.quantity,
      title:    item.title,
      imageUrl: item.imageUrl,
      specJson: item.specJson || null,
    }))

    const { data: res } = await createOrder({
      addressId: selectedAddress.value.id,
      remark: remark.value,
      cartItems
    })

    if (res.code === 1) {
      ElMessage.success('下单成功！')
      // 清除购物车已勾选项
      cartStore.clearChecked(checkoutIndexes.value)
      // 跳到订单详情或订单列表
      router.push('/orders')
    } else {
      ElMessage.error(res.msg)
    }
  } finally { submitting.value = false }
}

onMounted(() => {
  // 从 sessionStorage 读取 Cart 页面传过来的选中项
  const items = sessionStorage.getItem('checkoutItems')
  const indexes = sessionStorage.getItem('checkoutIndexes')
  if (items) checkoutItems.value = JSON.parse(items)
  if (indexes) checkoutIndexes.value = JSON.parse(indexes)
  if (!checkoutItems.value.length) {
    ElMessage.warning('没有选中商品')
    router.push('/cart')
    return
  }
  loadAddresses()
})
</script>

<style scoped>
.checkout-page { max-width: 900px; margin: 20px auto; padding: 0 16px 80px; }
.page-title { font-size: 24px; font-weight: bold; margin-bottom: 20px; }
.section-card { margin-bottom: 16px; }
.selected-addr { display: flex; align-items: center; gap: 12px; }
.addr-tag { font-weight: bold; white-space: nowrap; }
.addr-text { color: #666; flex: 1; }
.order-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.order-item:last-child { border-bottom: none; }
.item-img { width: 60px; height: 60px; object-fit: contain; border-radius: 4px; border: 1px solid #eee; }
.item-info { flex: 1; min-width: 0; }
.item-title { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-spec { font-size: 12px; color: #999; margin-top: 4px; }
.item-price { width: 80px; text-align: center; color: #666; }
.item-qty { width: 40px; text-align: center; color: #999; }
.item-subtotal { width: 90px; text-align: right; color: #f56c6c; font-weight: bold; }
.checkout-footer { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; padding: 16px 30px; box-shadow: 0 -2px 8px rgba(0,0,0,0.08); display: flex; justify-content: flex-end; align-items: center; gap: 20px; z-index: 100; }
.total-price { color: #f56c6c; font-size: 24px; font-weight: bold; }
.picker-item { padding: 12px 16px; border: 2px solid #eee; border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: 0.2s; }
.picker-item:hover { border-color: #409eff; }
.picker-item.active { border-color: #67c23a; background: #f0f9eb; }
.picker-detail { font-size: 13px; color: #666; margin-top: 4px; }
</style>