<template>
  <div class="cart-page">
    <div class="page-header">
      <el-icon><ShoppingCart /></el-icon> 购物车
      <span class="count-badge">{{ cartStore.totalCount }} 件</span>
    </div>

    <el-empty v-if="!cartStore.items.length" description="购物车空空如也" :image-size="120">
      <el-button type="primary" @click="$router.push('/home')">去逛逛</el-button>
    </el-empty>

    <template v-else>
      <div class="cart-list">
        <div v-for="(item, index) in cartStore.items" :key="index" class="cart-item">
          <el-checkbox v-model="checkedIndexes" :label="index" />
          <img :src="item.imageUrl || 'https://via.placeholder.com/80'" class="item-img" />
          <div class="item-info">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-spec" v-if="item.specJson">
              {{ formatSpec(item.specJson) }}
            </div>
          </div>
          <div class="item-price">¥{{ item.price }}</div>
          <el-input-number
            v-model="item.quantity"
            :min="1" :max="999" size="small"
            @change="val => cartStore.updateQuantity(index, val)"
          />
          <div class="item-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          <el-button text type="danger" @click="cartStore.removeItem(index)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="cart-footer">
        <el-checkbox
          v-model="checkAll"
          :indeterminate="isIndeterminate"
          @change="toggleAll"
        >全选</el-checkbox>
        <div class="footer-right">
          <span class="selected-info">
            已选 <b>{{ checkedIndexes.length }}</b> 件，合计：
            <b class="total-price">¥{{ selectedTotal }}</b>
          </span>
          <el-button
            type="danger" size="large"
            :disabled="!checkedIndexes.length"
            @click="goCheckout"
          >去结算</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart, Delete } from '@element-plus/icons-vue'
import { useCartStore } from '../../store/cartStore'

const router     = useRouter()
const cartStore  = useCartStore()
const checkedIndexes = ref([])

// 全选逻辑
const checkAll = computed(() =>
  checkedIndexes.value.length === cartStore.items.length && cartStore.items.length > 0
)
const isIndeterminate = computed(() =>
  checkedIndexes.value.length > 0 && checkedIndexes.value.length < cartStore.items.length
)
const toggleAll = (val) => {
  checkedIndexes.value = val ? cartStore.items.map((_, i) => i) : []
}

// 计算选中商品总价
const selectedTotal = computed(() =>
  checkedIndexes.value
    .reduce((s, i) => s + cartStore.items[i].price * cartStore.items[i].quantity, 0)
    .toFixed(2)
)

const formatSpec = (specJson) => {
  try {
    const obj = JSON.parse(specJson)
    return Object.entries(obj).map(([k, v]) => `${k}: ${v}`).join(' | ')
  } catch { return specJson }
}

const goCheckout = () => {
  if (!checkedIndexes.value.length) return
  // 把选中项存到 sessionStorage，结算页读取
  const selected = checkedIndexes.value.map(i => cartStore.items[i])
  sessionStorage.setItem('checkoutItems', JSON.stringify(selected))
  sessionStorage.setItem('checkoutIndexes', JSON.stringify(checkedIndexes.value))
  router.push('/checkout')
}
</script>

<style scoped>
.cart-page { max-width: 1000px; margin: 20px auto; padding: 0 16px; }
.page-header { font-size: 22px; font-weight: bold; margin-bottom: 20px; display: flex; align-items: center; gap: 8px; }
.count-badge { font-size: 14px; background: #f56c6c; color: #fff; border-radius: 12px; padding: 2px 8px; }
.cart-list { border: 1px solid #eee; border-radius: 8px; overflow: hidden; }
.cart-item { display: flex; align-items: center; gap: 16px; padding: 16px; border-bottom: 1px solid #f5f5f5; background: #fff; }
.cart-item:last-child { border-bottom: none; }
.item-img { width: 80px; height: 80px; object-fit: contain; border-radius: 6px; border: 1px solid #eee; }
.item-info { flex: 1; min-width: 0; }
.item-title { font-size: 14px; line-height: 1.4; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.item-spec { font-size: 12px; color: #999; margin-top: 4px; }
.item-price { width: 80px; text-align: center; color: #999; font-size: 14px; }
.item-subtotal { width: 90px; text-align: center; color: #f56c6c; font-weight: bold; font-size: 16px; }
.cart-footer { display: flex; align-items: center; justify-content: space-between; padding: 20px; background: #fff; border-radius: 8px; margin-top: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.footer-right { display: flex; align-items: center; gap: 20px; }
.selected-info { font-size: 14px; color: #666; }
.total-price { color: #f56c6c; font-size: 22px; }
</style>
