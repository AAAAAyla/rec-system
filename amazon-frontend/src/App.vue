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

        <template v-if="userStore.token">
          <el-button text style="color:#f0c14b" @click="router.push('/orders')">我的订单</el-button>
          <el-button text style="color:#ccc" @click="router.push('/favorites')">收藏</el-button>
          <el-button text style="color:#ccc" @click="router.push('/im')">消息</el-button>

          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :offset="[-5, 5]">
            <el-button circle :icon="Bell" size="small" @click="router.push('/notifications')" />
          </el-badge>

          <el-button
              v-if="userStore.user?.role === 2"
              type="danger" plain size="small"
              @click="router.push('/admin')"
          >管理后台</el-button>

          <el-button
              v-if="userStore.user?.role === 1 || userStore.user?.role === 2"
              type="primary" plain size="small"
              @click="router.push('/merchant')"
          >商家后台</el-button>

          <el-button
              v-if="userStore.user?.role === 0 || userStore.user?.role == null"
              type="success" plain size="small"
              @click="router.push('/merchant-apply')"
          >申请入驻</el-button>

          <el-dropdown trigger="click">
            <span class="username" style="cursor:pointer">{{ userStore.user?.username }} ▾</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/addresses')">地址管理</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-button type="warning" @click="router.push('/login')">登录</el-button>
        </template>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <AiAssistant />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Goods, ShoppingCart, Search, Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from './store/cartStore'
import { useUserStore } from './store/userStore'
import AiAssistant from './components/AiAssistant.vue'
import axios from 'axios'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const searchKw = ref('')
const unreadCount = ref(0)
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const loadUnread = async () => {
  if (!userStore.token) return
  try {
    const { data: res } = await axios.get(`${BASE}/notifications/unread-count`)
    if (res.code === 1) unreadCount.value = res.data || 0
  } catch {}
}

onMounted(() => { loadUnread(); setInterval(loadUnread, 30000) })
watch(() => userStore.token, loadUnread)

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