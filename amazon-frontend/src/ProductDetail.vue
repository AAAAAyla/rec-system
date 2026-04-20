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
          <el-icon class="mock-img-icon"><Goods /></el-icon>
          <div class="img-text">商品 ID: {{ productId }}</div>
        </div>
      </el-col>

      <el-col :xs="24" :md="14">
        <h1 class="product-title">AI 推荐精选商品</h1>
        <div class="product-price">
          <span class="currency">￥</span>
          <span class="amount">99.00</span>
        </div>

        <p class="product-desc">
          这是一件由 AmazonRec 智能体通过 RAG 知识库为您精准召回的推荐商品。
        </p>

        <div class="actions">
          <el-button type="warning" size="large" @click="handleAddToCart">
            <el-icon><ShoppingCart /></el-icon> 加入购物车
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
// 🔴 引入我们的 Pinia 仓库
import { useCartStore } from './store/cartStore'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const productId = ref('')

onMounted(() => {
  // 从路由地址中抓取 ID，例如 /product/123 -> 获取到 123
  productId.value = route.params.id
})

const goBack = () => {
  router.push('/home')
}

// 🔴 点击加入购物车按钮触发的动作
const handleAddToCart = () => {
  cartStore.addToCart({
    id: productId.value,
    title: `测试商品-${productId.value}`,
    price: 99.00
  })
  ElMessage.success('成功加入购物车！右上角角标已更新。')
}
</script>

<style scoped>
.detail-container { max-width: 1200px; margin: 0 auto; padding: 20px; background: white; border-radius: 8px; margin-top: 20px;}
.header { padding-bottom: 20px; border-bottom: 1px solid #ebeef5; }
.product-main { margin-top: 30px; }
.image-box { background: #f7f9fa; height: 350px; display: flex; flex-direction: column; justify-content: center; align-items: center; border-radius: 8px; color: #a8abb2; }
.mock-img-icon { font-size: 80px; margin-bottom: 20px; }
.product-title { font-size: 28px; color: #303133; margin-top: 0; }
.product-price { color: #f56c6c; margin: 20px 0; }
.currency { font-size: 20px; font-weight: bold; }
.amount { font-size: 36px; font-weight: bold; }
.product-desc { color: #606266; line-height: 1.8; font-size: 15px; margin-bottom: 40px; }
.actions { display: flex; align-items: center; gap: 20px; }
</style>