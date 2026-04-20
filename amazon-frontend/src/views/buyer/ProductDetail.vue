<template>
  <div class="detail-container">
    <el-page-header @back="goBack" title="返回商城" class="header">
      <template #content>
        <span class="text-large font-600 mr-3"> 商品详情 </span>
      </template>
    </el-page-header>

    <el-row :gutter="40" class="product-main">
      <el-col :xs="24" :md="10">
        <div class="image-box">
          <img v-if="productData.image" :src="productData.image" class="detail-image" @error="handleImageError" />
          <div v-else class="mock-img-wrapper">
            <el-icon class="mock-img-icon"><Goods /></el-icon>
            <div class="img-text">商品 ID: {{ productId }}</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :md="14">
        <h1 class="product-title">{{ productData.title || 'AI 推荐精选商品' }}</h1>
        <div class="product-price">
          <span class="currency">￥</span>
          <span class="amount">{{ productData.price || '99.00' }}</span>
        </div>

        <p class="product-desc">
          这是一件由 AmazonRec 智能体通过算法和知识库为您精准召回的推荐商品。具备高品质保证，支持七天无理由退换。
        </p>

        <div class="actions">
          <el-input-number v-model="buyCount" :min="1" :max="99" style="margin-right: 15px;" />
          <el-button type="warning" size="large" @click="handleAddToCart">
            <el-icon><ShoppingCart /></el-icon> 加入购物车
          </el-button>
          <el-button type="danger" size="large" @click="handleBuyNow">
            立即购买
          </el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Goods, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from '../../store/cartStore.js'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const productId = ref('')
const productData = ref({})
const buyCount = ref(1)

onMounted(() => {
  productId.value = route.params.id
  productData.value = {
    title: route.query.title,
    price: route.query.price,
    image: route.query.image
  }
})

const goBack = () => {
  router.push('/home')
}

const handleImageError = (e) => {
  e.target.src = 'https://via.placeholder.com/400?text=No+Image'
}

const handleAddToCart = () => {
  const item = {
    id: productId.value,
    title: productData.value.title || `精选商品-${productId.value}`,
    price: parseFloat(productData.value.price || 99.00)
  }

  // 支持加入多个数量
  for(let i=0; i<buyCount.value; i++){
    cartStore.addToCart(item)
  }

  ElMessage.success('成功加入购物车！')
}

const handleBuyNow = () => {
  handleAddToCart()
  router.push('/cart')
}
</script>

<style scoped>
.detail-container { max-width: 1200px; margin: 0 auto; padding: 20px; background: white; border-radius: 8px; margin-top: 20px;}
.header { padding-bottom: 20px; border-bottom: 1px solid #ebeef5; }
.product-main { margin-top: 30px; }
.image-box { background: #fff; height: 400px; display: flex; justify-content: center; align-items: center; border-radius: 8px; overflow: hidden; border: 1px solid #eee; }
.detail-image { max-width: 100%; max-height: 100%; object-fit: contain; }
.mock-img-wrapper { display: flex; flex-direction: column; align-items: center; color: #a8abb2; }
.mock-img-icon { font-size: 80px; margin-bottom: 20px; }
.product-title { font-size: 24px; color: #303133; margin-top: 0; line-height: 1.4; }
.product-price { color: #B12704; margin: 20px 0; }
.currency { font-size: 20px; font-weight: bold; }
.amount { font-size: 36px; font-weight: bold; }
.product-desc { color: #606266; line-height: 1.8; font-size: 15px; margin-bottom: 40px; padding: 15px; background: #f9f9f9; border-radius: 4px; }
.actions { display: flex; align-items: center; margin-top: 20px; }
</style>