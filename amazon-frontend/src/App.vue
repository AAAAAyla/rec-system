<template>
  <div class="app-container">
    <el-header class="amazon-header">
      <div class="logo" @click="goHome">
        <el-icon><Goods /></el-icon> AmazonRec
      </div>

      <div class="user-actions">
        <el-badge :value="cartStore.totalCount" class="cart-badge">
          <el-button type="warning" circle :icon="ShoppingCart" />
        </el-badge>
        <span class="user-badge">欢迎回来！</span>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Goods, ShoppingCart } from '@element-plus/icons-vue'
// 引入刚刚建好的仓库
import { useCartStore } from './store/cartStore'

const router = useRouter()
const cartStore = useCartStore()

const goHome = () => {
  router.push('/')
}
</script>

<style>
body { margin: 0; padding: 0; background-color: #f3f3f3; font-family: Arial, sans-serif; }
.app-container { min-height: 100vh; display: flex; flex-direction: column; }
.amazon-header { background-color: #131921; display: flex; align-items: center; justify-content: space-between; padding: 0 30px; height: 60px;}
.logo { color: white; font-weight: bold; font-size: 24px; display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-actions { display: flex; align-items: center; gap: 20px; }
.user-badge { color: #f0c14b; font-weight: bold; font-size: 14px;}
.cart-badge { margin-right: 15px; }
.main-content { padding: 0; flex: 1; }
</style>