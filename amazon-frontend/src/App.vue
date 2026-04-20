<template>
  <div class="app-container">
    <el-header class="amazon-header">
      <div class="logo" @click="router.push('/home')">
        <el-icon><Goods /></el-icon> AmazonRec
      </div>

      <div class="search-area">
        <el-input
            v-model="searchKw"
            placeholder="搜索商品..."
            class="search-input"
            @keyup.enter="doSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="doSearch" />
          </template>
        </el-input>
      </div>

      <div class="user-actions">
        <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0">
          <el-button circle :icon="ShoppingCart" @click="router.push('/cart')" />
        </el-badge>

        <el-button
            v-if="userStore.user?.role === 1"
            type="primary"
            plain
            @click="router.push('/merchant')"
        >
          商家后台
        </el-button>

        <template v-if="userStore.token">
          <span class="username">{{ userStore.user?.username }}</span>
          <el-button text @click="logout">退出</el-button>
        </template>

        <template v-else>
          <el-button type="warning" @click="router.push('/login')">登录</el-button>
        </template>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Goods, ShoppingCart, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from './store/cartStore'
import { useUserStore } from './store/userStore'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const searchKw = ref('')

const doSearch = () => {
  if (!searchKw.value.trim()) return
  router.push({ path: '/search', query: { kw: searchKw.value.trim() } })
}

const logout = () => {
  userStore.logout()
  ElMessage.success('已安全退出')
  router.push('/login')
}
</script>

<style>
body { margin: 0; padding: 0; background-color: #f3f3f3; font-family: Arial, sans-serif; }
.app-container { min-height: 100vh; display: flex; flex-direction: column; }
.amazon-header {
  background-color: #131921;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  height: 60px;
  gap: 20px;
}
.logo {
  color: white;
  font-weight: bold;
  font-size: 22px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}
.search-area { flex: 1; max-width: 600px; }
.search-input { width: 100%; }
.user-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.username {
  color: #f0c14b;
  font-weight: bold;
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.main-content { padding: 0; flex: 1; }
</style>