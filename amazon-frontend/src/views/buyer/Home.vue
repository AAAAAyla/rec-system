<template>
  <div class="common-layout">

    <!-- 推荐区（仅登录用户显示） -->
    <el-main v-if="userStore.isLoggedIn && recommendList.length > 0">
      <div class="section-header">
        <el-icon color="#f0c14b"><StarFilled /></el-icon> 猜你喜欢
      </div>
      <el-row :gutter="20">
        <el-col v-for="item in recommendList" :key="'rec-' + item.id" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="product-card" :body-style="{ padding: '10px' }" @click="goToDetail(item)">
            <div class="image-container">
              <img :src="item.image_url" class="product-image" @error="handleImageError" />
            </div>
            <div class="product-info">
              <div class="product-title">{{ item.title }}</div>
              <div class="price"><span class="currency">￥</span>{{ item.price }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-divider />
    </el-main>

    <!-- 商品列表（所有人可见） -->
    <el-main>
      <div class="section-header">
        <el-icon color="#f0c14b"><Goods /></el-icon> 全部商品
      </div>

      <el-row :gutter="20" v-if="productList.length > 0">
        <el-col v-for="item in productList" :key="item.id" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="product-card" :body-style="{ padding: '10px' }" @click="goToDetail(item)">
            <div class="image-container">
              <img :src="item.imageUrl || item.image_url" class="product-image" @error="handleImageError" />
            </div>
            <div class="product-info">
              <div class="product-title">{{ item.title }}</div>
              <el-rate v-model="item.mockRate" disabled show-score text-color="#ff9900" />
              <div class="price">
                <span class="currency">￥</span>
                <span class="amount">{{ item.price }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-else-if="!loading" description="暂无商品，请稍后再试" />
      <div v-if="loading" class="loading-tip">加载中...</div>

      <!-- 分页 -->
      <el-pagination
          v-if="total > pageSize"
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          class="pagination"
          @current-change="loadProducts"
      />
    </el-main>

    <!-- AI 助手 -->
    <div class="ai-agent-container">
      <el-button type="primary" class="ai-fab" @click="chatVisible = true">
        <el-icon><ChatDotRound /></el-icon> AI 助手
      </el-button>

      <el-dialog v-model="chatVisible" title="AmazonRec 智能导购" width="400px" :modal="false" class="chat-dialog">
        <div class="chat-window" ref="chatWindow">
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['msg', msg.role]">
            <div class="msg-content">{{ msg.content }}</div>
          </div>
        </div>
        <template #footer>
          <el-input v-model="userInput" placeholder="输入您的需求..." @keyup.enter="sendToAI">
            <template #append>
              <el-button @click="sendToAI" :loading="aiLoading">发送</el-button>
            </template>
          </el-input>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { StarFilled, ChatDotRound, Goods } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/userStore'

const router    = useRouter()
const userStore = useUserStore()

const productList   = ref([])
const recommendList = ref([])
const loading  = ref(false)
const pageNum  = ref(1)
const pageSize = ref(20)
const total    = ref(0)

const chatVisible = ref(false)
const aiLoading   = ref(false)
const userInput   = ref('')
const chatHistory = ref([{ role: 'ai', content: '您好！我是您的专属购物助手，想找点什么？' }])
const chatWindow  = ref(null)

// 加载公开商品列表（无需登录）
const loadProducts = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/items/search', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
    })
    if (res.data.code === 1) {
      productList.value = (res.data.data.rows || []).map(item => ({
        ...item,
        mockRate: parseFloat((4 + Math.random()).toFixed(1))
      }))
      total.value = res.data.data.total || 0
    }
  } catch (err) {
    ElMessage.error('加载商品失败：' + (err.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 加载个性化推荐（仅登录用户，失败静默处理）
const loadRecommendations = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await axios.get('http://localhost:8080/items/recommend')
    if (res.data.code === 1 && Array.isArray(res.data.data?.rows)) {
      recommendList.value = res.data.data.rows
    }
  } catch {
    // 推荐接口失败不影响主流程
  }
}

const goToDetail = (item) => {
  router.push(`/product/${item.id}`)
}

const handleImageError = (e) => {
  if (e?.target) e.target.src = 'https://via.placeholder.com/150?text=No+Image'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatWindow.value) chatWindow.value.scrollTop = chatWindow.value.scrollHeight
  })
}

const sendToAI = async () => {
  if (!userInput.value) return
  const text = userInput.value
  chatHistory.value.push({ role: 'user', content: text })
  userInput.value = ''
  aiLoading.value = true
  scrollToBottom()
  try {
    const res = await axios.post('http://localhost:8080/ai/chat', { message: text })
    chatHistory.value.push({
      role: 'ai',
      content: res.data.code === 1 ? res.data.data : '系统返回错误：' + res.data.msg
    })
  } catch {
    chatHistory.value.push({ role: 'ai', content: '抱歉，大脑开了一点小差，请稍后再试。' })
  } finally {
    aiLoading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  loadProducts()
  loadRecommendations()
})
</script>

<style scoped>
.section-header { font-size: 22px; font-weight: bold; margin: 20px 0; display: flex; align-items: center; gap: 8px; color: #111; }
.product-card { margin-bottom: 20px; height: 360px; cursor: pointer; transition: 0.3s; border: none; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.product-card:hover { transform: translateY(-5px); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
.image-container { height: 200px; display: flex; align-items: center; justify-content: center; overflow: hidden; background: #fff; padding: 10px; }
.product-image { max-width: 100%; max-height: 100%; object-fit: contain; }
.product-title { font-size: 14px; height: 40px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin: 10px 0; line-height: 1.4; color: #007185; }
.product-title:hover { color: #c45500; text-decoration: underline; }
.price { color: #B12704; font-size: 20px; margin-top: 5px; font-weight: bold; }
.currency { font-size: 12px; position: relative; top: -5px; }
.pagination { margin: 20px 0; display: flex; justify-content: center; }
.loading-tip { text-align: center; color: #999; padding: 20px; }
.ai-agent-container { position: fixed; right: 30px; bottom: 30px; z-index: 1000; }
.ai-fab { border-radius: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.chat-window { height: 300px; overflow-y: auto; padding: 10px; background-color: #f9f9f9; border-radius: 4px; display: flex; flex-direction: column; gap: 10px; }
.msg { display: flex; width: 100%; }
.msg.user { justify-content: flex-end; }
.msg.ai  { justify-content: flex-start; }
.msg-content { max-width: 80%; padding: 8px 12px; border-radius: 8px; font-size: 14px; line-height: 1.5; word-break: break-all; }
.msg.user .msg-content { background-color: #d1e8ff; color: #004085; }
.msg.ai  .msg-content { background-color: #ffffff; border: 1px solid #e0e0e0; color: #333; }
</style>
