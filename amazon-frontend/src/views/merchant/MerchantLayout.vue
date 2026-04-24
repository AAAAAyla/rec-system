<!-- amazon-frontend/src/views/merchant/MerchantLayout.vue -->
<template>
  <div class="merchant-container">

    <!-- 左侧导航 -->
    <aside class="merchant-sidebar">
      <div class="sidebar-logo">
        <el-icon><Shop /></el-icon>
        <span>商家中心</span>
      </div>

      <el-menu
          :default-active="activeMenu"
          router
          background-color="#1d2535"
          text-color="#a0a8b8"
          active-text-color="#ffffff"
      >
        <el-menu-item index="/merchant/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据看板</span>
        </el-menu-item>

        <el-menu-item index="/merchant/items">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>

        <el-menu-item index="/merchant/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>

        <el-menu-item index="/merchant/shop">
          <el-icon><Setting /></el-icon>
          <span>店铺设置</span>
        </el-menu-item>

        <el-menu-item index="/merchant/warehouses">
          <el-icon><OfficeBuilding /></el-icon>
          <span>发货地管理</span>
        </el-menu-item>

        <el-menu-item index="/merchant/coupons">
          <el-icon><Ticket /></el-icon>
          <span>优惠券管理</span>
        </el-menu-item>

        <el-menu-item index="/merchant/im">
          <el-icon><ChatDotSquare /></el-icon>
          <span>客服消息</span>
        </el-menu-item>
      </el-menu>

      <!-- 底部：返回买家端 -->
      <div class="sidebar-footer">
        <el-button text style="color:#a0a8b8" @click="router.push('/home')">
          <el-icon><ArrowLeft /></el-icon> 返回商城
        </el-button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="merchant-main">
      <!-- 顶部栏 -->
      <div class="merchant-topbar">
        <span class="shop-name">{{ merchantInfo?.shopName || '我的店铺' }}</span>
        <span class="top-username">{{ userStore.user?.username }}</span>
      </div>

      <!-- 子页面渲染区 -->
      <div class="merchant-content">
        <router-view />
      </div>
    </main>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Shop, DataLine, Goods, List, Setting, ArrowLeft, OfficeBuilding, Ticket, ChatDotSquare } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/userStore'
import { getMyMerchantInfo } from '../../api/merchant'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const merchantInfo = ref(null)
const activeMenu = computed(() => route.path)

onMounted(async () => {
  try {
    const { data: res } = await getMyMerchantInfo()
    if (res.code === 1 && res.data) {
      merchantInfo.value = res.data
      // 商家未审核通过，跳回申请页
      if (res.data.status !== 1) {
        router.push('/merchant-apply')
      }
    } else {
      router.push('/merchant-apply')
    }
  } catch {
    router.push('/merchant-apply')
  }
})
</script>

<style scoped>
.merchant-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.merchant-sidebar {
  width: 220px;
  background-color: #1d2535;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #ffffff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #2d3748;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px;
  border-top: 1px solid #2d3748;
}

.merchant-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f5f7fa;
}

.merchant-topbar {
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.shop-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.top-username {
  font-size: 14px;
  color: #909399;
}

.merchant-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>