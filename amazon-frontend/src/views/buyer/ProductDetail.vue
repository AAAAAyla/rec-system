<template>
  <div class="detail-container">
    <el-page-header @back="goBack" title="返回商城" class="header">
      <template #content><span class="text-large font-600 mr-3">商品详情</span></template>
    </el-page-header>

    <!-- 骨架屏 -->
    <div v-if="loading" class="skeleton-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <el-row v-else-if="item" :gutter="40" class="product-main">
      <!-- 左：图片 -->
      <el-col :xs="24" :md="10">
        <div class="image-box">
          <img
            :src="currentImage"
            class="detail-image"
            @error="handleImageError"
          />
        </div>
      </el-col>

      <!-- 右：信息 -->
      <el-col :xs="24" :md="14">
        <h1 class="product-title">{{ item.title }}</h1>

        <div class="product-price">
          <span class="currency">￥</span>
          <span class="amount">{{ currentPrice }}</span>
        </div>

        <!-- SKU 规格选择（有多个 SKU 时显示） -->
        <div v-if="item.skus && item.skus.length > 0" class="sku-section">
          <div class="sku-label">规格：</div>
          <div class="sku-options">
            <el-button
              v-for="sku in item.skus"
              :key="sku.id"
              :type="selectedSku?.id === sku.id ? 'primary' : 'default'"
              size="small"
              style="margin: 4px"
              @click="selectedSku = sku"
            >
              {{ formatSpec(sku.specJson) || '默认规格' }}
            </el-button>
          </div>
        </div>

        <!-- 库存 -->
        <div class="stock-info" v-if="selectedSku">
          库存：<span :style="{ color: selectedSku.stock > 0 ? '#67c23a' : '#f56c6c' }">
            {{ selectedSku.stock > 0 ? selectedSku.stock + ' 件' : '暂时缺货' }}
          </span>
        </div>

        <p class="product-desc">{{ item.description || '商品暂无描述' }}</p>

        <div class="actions">
          <el-input-number
            v-model="buyCount"
            :min="1"
            :max="selectedSku ? selectedSku.stock : 99"
            style="margin-right: 15px;"
          />
          <el-button type="warning" size="large" @click="handleAddToCart">
            <el-icon><ShoppingCart /></el-icon> 加入购物车
          </el-button>
          <el-button type="danger" size="large" @click="handleBuyNow">
            立即购买
          </el-button>
          <el-button :type="isFavorited ? 'warning' : 'default'" size="large" @click="toggleFav">
            {{ isFavorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button type="info" size="large" plain @click="contactMerchant">
            联系客服
          </el-button>
        </div>
      </el-col>
    </el-row>

    <el-empty v-else-if="!loading" description="商品不存在或已下架" />

    <!-- 评价区 -->
    <el-card v-if="item" class="review-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>商品评价 ({{ reviews.length }})</b>
          <span v-if="avgScore > 0" style="color:#e6a23c;font-size:14px">
            平均 {{ avgScore.toFixed(1) }} 分
          </span>
        </div>
      </template>
      <el-empty v-if="!reviews.length" description="暂无评价，购买后可评价" :image-size="80" />
      <div v-for="r in reviews" :key="r.id" class="review-item">
        <div class="review-header">
          <div class="reviewer-avatar">{{ (r.username || '匿')[0] }}</div>
          <b>{{ r.username || '匿名用户' }}</b>
          <el-rate v-model="r.score" disabled size="small" />
          <span class="review-time">{{ formatReviewTime(r.createTime || r.create_time) }}</span>
        </div>
        <div class="review-content">{{ r.content }}</div>
      </div>
      <div v-if="reviews.length >= 5" style="text-align:center;margin-top:12px">
        <el-button text type="primary" size="small">查看更多评价</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useCartStore } from '../../store/cartStore.js'
import { checkFavorite, addFavorite, removeFavorite } from '../../api/favorite'
import { createSession } from '../../api/im'

const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const route     = useRoute()
const router    = useRouter()
const cartStore = useCartStore()

const item        = ref(null)
const loading     = ref(true)
const buyCount    = ref(1)
const selectedSku = ref(null)
const reviews     = ref([])
const avgScore    = ref(0)

// 当前展示的图片：优先选中 SKU 图，其次商品主图
const currentImage = computed(() =>
  selectedSku.value?.imageUrl || item.value?.imageUrl || ''
)

// 当前价格：有 SKU 取 SKU 价，否则取商品默认价
const currentPrice = computed(() => {
  if (selectedSku.value) return selectedSku.value.price
  return item.value?.price || '—'
})

const formatSpec = (specJson) => {
  if (!specJson) return ''
  try {
    const obj = JSON.parse(specJson)
    return Object.entries(obj).map(([k, v]) => `${k}:${v}`).join(' ')
  } catch {
    return specJson
  }
}

const handleImageError = (e) => {
  if (e?.target) e.target.src = 'https://via.placeholder.com/400?text=No+Image'
}

const goBack = () => router.push('/home')

// 加载商品详情
const loadItem = async () => {
  loading.value = true
  try {
    const { data: res } = await axios.get(`${BASE}/items/${route.params.id}`)
    if (res.code === 1) {
      item.value = res.data
      // 如果只有一个 SKU，默认选中
      if (res.data.skus && res.data.skus.length === 1) {
        selectedSku.value = res.data.skus[0]
      } else if (res.data.skus && res.data.skus.length > 1) {
        selectedSku.value = res.data.skus[0]
      }
    } else {
      ElMessage.error(res.msg || '商品加载失败')
    }
  } catch (err) {
    ElMessage.error('网络错误：' + (err.message || '未知'))
  } finally {
    loading.value = false
  }
}

const formatReviewTime = (t) => {
  if (!t) return ''
  try {
    const d = new Date(t)
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
  } catch { return t }
}

// 加载评价（失败不影响主流程）
const loadReviews = async () => {
  try {
    const { data: res } = await axios.get(`${BASE}/reviews/items/${route.params.id}`, {
      params: { pageNum: 1, pageSize: 5 }
    })
    if (res.code === 1) {
      reviews.value = res.data.rows || []
      avgScore.value = res.data.avgScore || 0
    }
  } catch { /* 静默 */ }
}

const handleAddToCart = () => {
  if (!item.value) return
  const sku = selectedSku.value

  const cartItem = {
    itemId:   item.value.id,
    skuId:    sku?.id || null,
    title:    item.value.title,
    imageUrl: sku?.imageUrl || item.value.imageUrl || '',
    specJson: sku?.specJson || null,
    price:    parseFloat(sku?.price ?? item.value.price ?? 0),
    quantity: buyCount.value,
  }

  cartStore.addToCart(cartItem)
  ElMessage.success('已加入购物车')
}

const handleBuyNow = () => {
  handleAddToCart()
  router.push('/cart')
}

const isFavorited = ref(false)

const loadFavStatus = async () => {
  try {
    const { data: res } = await checkFavorite(route.params.id)
    if (res.code === 1) isFavorited.value = res.data
  } catch {}
}

const toggleFav = async () => {
  try {
    if (isFavorited.value) {
      await removeFavorite(route.params.id)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(route.params.id)
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  } catch { ElMessage.error('操作失败') }
}

const contactMerchant = async () => {
  if (!item.value?.merchantId) return ElMessage.warning('商家信息不可用')
  try {
    const { data: res } = await createSession(item.value.merchantId)
    if (res.code === 1) router.push('/im')
  } catch { ElMessage.error('创建会话失败') }
}

onMounted(() => {
  loadItem()
  loadReviews()
  if (localStorage.getItem('token')) loadFavStatus()
})
</script>

<style scoped>
.detail-container { max-width: 1200px; margin: 20px auto; padding: 20px; background: #fff; border-radius: 8px; }
.header { padding-bottom: 20px; border-bottom: 1px solid #ebeef5; }
.skeleton-wrap { padding: 40px; }
.product-main { margin-top: 30px; }
.image-box { background: #fff; height: 380px; display: flex; justify-content: center; align-items: center; border-radius: 8px; overflow: hidden; border: 1px solid #eee; }
.detail-image { max-width: 100%; max-height: 100%; object-fit: contain; }
.product-title { font-size: 22px; color: #303133; margin-top: 0; line-height: 1.5; }
.product-price { color: #B12704; margin: 16px 0; }
.currency { font-size: 18px; font-weight: bold; }
.amount { font-size: 34px; font-weight: bold; }
.sku-section { margin: 16px 0; }
.sku-label { font-size: 14px; color: #666; margin-bottom: 8px; }
.sku-options { display: flex; flex-wrap: wrap; gap: 4px; }
.stock-info { font-size: 13px; color: #909399; margin-bottom: 12px; }
.product-desc { color: #606266; line-height: 1.8; font-size: 14px; margin-bottom: 32px; padding: 12px; background: #f9f9f9; border-radius: 4px; }
.actions { display: flex; align-items: center; margin-top: 20px; flex-wrap: wrap; gap: 8px; }
.review-card { margin-top: 24px; }
.review-item { padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.review-item:last-child { border-bottom: none; }
.review-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.review-time { font-size: 12px; color: #bbb; margin-left: auto; }
.review-content { font-size: 14px; color: #606266; padding-left: 40px; }
.reviewer-avatar { width: 28px; height: 28px; border-radius: 50%; background: #409eff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; flex-shrink: 0; }
</style>
