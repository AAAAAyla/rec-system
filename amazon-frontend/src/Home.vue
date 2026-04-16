<template>
  <div class="common-layout">
    <el-header class="amazon-header">
      <div class="logo">
        <el-icon><Goods /></el-icon> AmazonRec
      </div>
      <el-input v-model="searchQuery" placeholder="搜索商品..." class="search-input">
        <template #append>
          <el-button :icon="Search" />
        </template>
      </el-input>
      <div class="user-actions">
        <span class="user-badge">欢迎回来！</span>
        <el-button type="danger" size="small" plain @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>

    <el-main>
      <div class="section-header">
        <el-icon color="#f0c14b"><StarFilled /></el-icon> 猜你喜欢 (基于协同过滤)
      </div>

      <el-row :gutter="20" v-if="recommendList.length > 0">
        <el-col v-for="item in recommendList" :key="item.id" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card class="product-card" :body-style="{ padding: '10px' }">
            <div class="image-container">
              <img :src="item.image_url" class="product-image" @error="handleImageError" />
            </div>
            <div class="product-info">
              <div class="product-title">{{ item.title }}</div>
              <el-rate v-model="item.mockRate" disabled show-score text-color="#ff9900" />
              <div class="price">
                <span class="currency">$</span>
                <span class="amount">{{ (Math.random() * 100).toFixed(2) }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-else description="正在为您拼命计算推荐商品..." />
    </el-main>

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
import { Search, StarFilled, Goods, ChatDotRound } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const recommendList = ref([])
const searchQuery = ref('')
const router = useRouter()

const chatVisible = ref(false)
const aiLoading = ref(false)
const userInput = ref('')
const chatHistory = ref([{ role: 'ai', content: '您好！我是您的专属购物助手，想找点什么？' }])
const chatWindow = ref(null)

const handleLogout = () => {
  localStorage.removeItem('token')
  ElMessage.success('已安全退出')
  router.push('/login')
}

const loadRecommendations = async () => {
  try {
    const res = await axios.get('http://localhost:8080/items/recommend')
    if (res.data.code === 1 && res.data.data && Array.isArray(res.data.data.rows)) {
      recommendList.value = res.data.data.rows.map(item => ({
        ...item,
        mockRate: 4.5 + Math.random() * 0.5
      }))
    }
  } catch (err) {
    ElMessage.error('获取推荐列表失败，手环可能已过期')
    handleLogout()
  }
}

const handleImageError = (e) => {
  e.target.src = 'https://via.placeholder.com/150?text=No+Image'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatWindow.value) {
      chatWindow.value.scrollTop = chatWindow.value.scrollHeight
    }
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
    if (res.data.code === 1) {
      chatHistory.value.push({ role: 'ai', content: res.data.data })
    } else {
      chatHistory.value.push({ role: 'ai', content: '系统返回错误：' + res.data.msg })
    }
  } catch (err) {
    chatHistory.value.push({ role: 'ai', content: '抱歉，大脑开了一点小差，请检查后端或网络连接。' })
  } finally {
    aiLoading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  loadRecommendations()
})
</script>

<style scoped>
.amazon-header { background-color: #131921; display: flex; align-items: center; justify-content: space-between; padding: 10px 20px; }
.logo { color: white; font-weight: bold; font-size: 24px; display: flex; align-items: center; gap: 8px; }
.search-input { width: 50%; max-width: 600px; }
.user-actions { display: flex; align-items: center; gap: 15px; }
.user-badge { color: #f0c14b; font-weight: bold; font-size: 14px;}
.section-header { font-size: 22px; font-weight: bold; margin: 20px 0; display: flex; align-items: center; gap: 8px; color: #111; }
.product-card { margin-bottom: 20px; height: 360px; cursor: pointer; transition: 0.3s; border: none; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.product-card:hover { transform: translateY(-5px); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
.image-container { height: 200px; display: flex; align-items: center; justify-content: center; overflow: hidden; background: #fff; padding: 10px;}
.product-image { max-width: 100%; max-height: 100%; object-fit: contain; }
.product-title { font-size: 14px; height: 40px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin: 10px 0; line-height: 1.4; color: #007185; }
.product-title:hover { color: #c45500; text-decoration: underline; }
.price { color: #B12704; font-size: 20px; margin-top: 5px; font-weight: bold; }
.currency { font-size: 12px; position: relative; top: -5px; }

/* AI 助手样式 */
.ai-agent-container { position: fixed; right: 30px; bottom: 30px; z-index: 1000; }
.ai-fab { border-radius: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.chat-window { height: 300px; overflow-y: auto; padding: 10px; background-color: #f9f9f9; border-radius: 4px; display: flex; flex-direction: column; gap: 10px; }
.msg { display: flex; width: 100%; }
.msg.user { justify-content: flex-end; }
.msg.ai { justify-content: flex-start; }
.msg-content { max-width: 80%; padding: 8px 12px; border-radius: 8px; font-size: 14px; line-height: 1.5; word-break: break-all; }
.msg.user .msg-content { background-color: #d1e8ff; color: #004085; }
.msg.ai .msg-content { background-color: #ffffff; border: 1px solid #e0e0e0; color: #333; }
</style>