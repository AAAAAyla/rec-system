<template>
  <div class="ai-assistant" v-if="isLoggedIn" :style="positionStyle">
    <!-- 悬浮按钮（可拖动） -->
    <transition name="bounce">
      <div
        v-if="!chatOpen"
        class="ai-fab"
        @mousedown="startDrag"
        @touchstart.passive="startDrag"
        @click="onFabClick"
      >
        <div class="fab-icon">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="white">
            <path d="M12 2C6.48 2 2 6.48 2 12c0 1.74.5 3.37 1.36 4.75L2 22l5.25-1.36C8.63 21.5 10.26 22 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2zm-1 15h-1.5v-1.5H11V17zm2.07-5.25l-.9.92C11.45 13.36 11 14 11 15h-1.5v-.5c0-.83.45-1.57 1.08-2.19l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H7c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"/>
          </svg>
        </div>
        <span class="fab-label">AI 助手</span>
      </div>
    </transition>

    <!-- 聊天面板（可拖动） -->
    <transition name="slide-up">
      <div v-if="chatOpen" class="chat-panel">
        <!-- 顶栏 (拖拽手柄) -->
        <div class="chat-header" @mousedown="startDrag" @touchstart.passive="startDrag">
          <div class="header-left">
            <div class="avatar-dot" />
            <div>
              <div class="header-title">小A · 购物助手</div>
              <div class="header-sub">AI 驱动 · 随时为你服务</div>
            </div>
          </div>
          <div class="header-actions" @mousedown.stop @touchstart.stop>
            <button class="icon-btn" @click="clearChat" title="清空聊天">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
            </button>
            <button class="icon-btn" @click="chatOpen = false" title="最小化">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13H5v-2h14v2z"/></svg>
            </button>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="chat-messages" ref="messagesRef">
          <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
            <div v-if="msg.role === 'ai'" class="msg-avatar ai-avatar">A</div>
            <div class="msg-bubble">
              <div class="msg-text">{{ msg.text }}</div>
              <!-- 商品卡片 -->
              <div v-if="msg.products && msg.products.length" class="product-cards">
                <div
                  v-for="p in msg.products" :key="p.id"
                  class="product-card"
                  @click="goProduct(p.id)"
                >
                  <img :src="p.image_url || 'https://via.placeholder.com/60?text=No+Img'" class="p-img" />
                  <div class="p-info">
                    <div class="p-title">{{ p.title }}</div>
                    <div class="p-price">￥{{ p.price }}</div>
                  </div>
                  <svg class="p-arrow" viewBox="0 0 24 24" width="16" height="16" fill="#999"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6z"/></svg>
                </div>
              </div>
            </div>
            <div v-if="msg.role === 'user'" class="msg-avatar user-avatar">我</div>
          </div>

          <!-- 打字指示器 -->
          <div v-if="loading" class="msg-row ai">
            <div class="msg-avatar ai-avatar">A</div>
            <div class="msg-bubble">
              <div class="typing-indicator">
                <span /><span /><span />
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷提问（实时更新，点过的消失，AI回复后刷新） -->
        <div v-if="visibleQuickQuestions.length > 0 && !loading" class="quick-actions">
          <div class="quick-label">{{ pageHint }}</div>
          <transition-group name="chip">
            <button v-for="q in visibleQuickQuestions" :key="q" class="quick-btn" @click="onQuickClick(q)">{{ q }}</button>
          </transition-group>
        </div>

        <!-- 输入区 -->
        <div class="chat-input">
          <input
            v-model="inputText"
            placeholder="问我任何购物问题..."
            @keyup.enter="sendMsg()"
            :disabled="loading"
          />
          <button class="send-btn" @click="sendMsg()" :disabled="loading || !inputText.trim()">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="white"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/userStore'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isLoggedIn = computed(() => !!userStore.token)
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

// ── 拖动逻辑 ──
const fabPos = ref({ right: 24, bottom: 24 })
const dragging = ref(false)
let dragStartX = 0, dragStartY = 0, startRight = 0, startBottom = 0, hasMoved = false

const positionStyle = computed(() => ({
  right: fabPos.value.right + 'px',
  bottom: fabPos.value.bottom + 'px',
}))

const startDrag = (e) => {
  const evt = e.touches ? e.touches[0] : e
  dragStartX = evt.clientX
  dragStartY = evt.clientY
  startRight = fabPos.value.right
  startBottom = fabPos.value.bottom
  hasMoved = false
  dragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('touchend', endDrag)
}

const onDrag = (e) => {
  const evt = e.touches ? e.touches[0] : e
  const dx = dragStartX - evt.clientX
  const dy = dragStartY - evt.clientY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  if (!hasMoved) return
  e.preventDefault?.()
  const panelW = chatOpen.value ? 380 : 130
  const panelH = chatOpen.value ? 560 : 60
  const maxRight = window.innerWidth - panelW
  const maxBottom = window.innerHeight - panelH
  fabPos.value.right = Math.max(0, Math.min(maxRight, startRight + dx))
  fabPos.value.bottom = Math.max(0, Math.min(maxBottom, startBottom + dy))
}

const endDrag = () => {
  dragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', endDrag)
}

const onFabClick = () => {
  if (!hasMoved) chatOpen.value = true
}

const chatOpen = ref(false)
const loading = ref(false)
const inputText = ref('')
const messagesRef = ref(null)

// ── 页面上下文感知 ──
const pageData = ref({})

const PAGE_CONFIG = {
  home:     { hint: '首页逛逛，有什么想找的？', questions: ['有什么热销商品推荐？', '帮我找一本好书', '最近有什么优惠？', '电子产品推荐'] },
  product:  { hint: '正在看这个商品，想了解什么？', questions: ['这个商品值得买吗？', '有没有类似但更便宜的？', '帮我对比一下同类商品', '这个商品适合送礼吗？'] },
  cart:     { hint: '购物车里有东西，需要帮忙吗？', questions: ['帮我看看买的东西搭不搭', '有没有凑单优惠的建议？', '帮我推荐一个搭配商品', '现在下单划算吗？'] },
  search:   { hint: '正在搜索，帮你找到更好的', questions: ['帮我筛选性价比最高的', '有没有别的关键词推荐？', '这个品类哪个牌子好？', '帮我找评价最好的'] },
  orders:   { hint: '查看订单中，有什么问题吗？', questions: ['怎么申请退款？', '物流一般几天到？', '我想再买一次上次的东西', '帮我推荐类似的商品'] },
  checkout: { hint: '即将下单，有什么疑问？', questions: ['有没有可用的优惠券？', '选哪个配送方式好？', '下单后多久发货？', '可以修改收货地址吗？'] },
  favorites:{ hint: '收藏夹里的好东西～', questions: ['收藏里哪个最值得买？', '有没有降价的商品？', '帮我比较一下收藏的商品', '推荐类似商品'] },
  merchant: { hint: '商家后台，有运营问题可以问我', questions: ['怎么提高商品曝光？', '定价有什么建议？', '怎么写好商品标题？', '如何处理售后问题？'] },
  admin:    { hint: '管理后台，需要数据分析吗？', questions: ['今日平台数据怎样？', '哪类商品最畅销？', '怎么处理违规商家？', '有什么运营建议？'] },
  default:  { hint: '有什么可以帮你的？', questions: ['有什么热销商品推荐？', '帮我找一本好书', '最近有什么优惠？', '电子产品推荐'] }
}

const currentPageKey = computed(() => {
  const path = route.path
  if (path.startsWith('/product/'))   return 'product'
  if (path === '/cart')               return 'cart'
  if (path === '/search')             return 'search'
  if (path === '/checkout')           return 'checkout'
  if (path.startsWith('/orders'))     return 'orders'
  if (path === '/favorites')          return 'favorites'
  if (path.startsWith('/merchant'))   return 'merchant'
  if (path.startsWith('/admin'))      return 'admin'
  if (path === '/home' || path === '/') return 'home'
  return 'default'
})

const pageHint = computed(() => PAGE_CONFIG[currentPageKey.value]?.hint || PAGE_CONFIG.default.hint)

// 已用过的问题
const usedQuestions = ref(new Set())
// 当前活跃的快捷问题池
const activeQuestions = ref([])

const FOLLOWUP_POOL = {
  home:     ['换个品类看看？', '有什么新品上架？', '帮我找性价比高的', '今日特价商品'],
  product:  ['有其他颜色/规格吗？', '这个商品的评价怎么样？', '推荐搭配商品', '帮我找更便宜的替代品'],
  cart:     ['还有什么值得加购？', '帮我算算总价', '有没有满减活动？', '推荐凑单商品'],
  search:   ['换个关键词试试', '按价格排序看看？', '这些哪个评分最高？', '推荐同类热销款'],
  orders:   ['查物流进度', '我想复购上次的商品', '推荐类似商品', '如何评价商品？'],
  checkout: ['确认地址对不对？', '有没有更快的配送？', '还能加优惠券吗？', '预计多久到货？'],
  favorites:['哪个最近降价了？', '帮我挑一个下单', '推荐类似商品', '收藏里哪个最热门？'],
  merchant: ['如何优化商品详情？', '竞品分析建议', '促销活动怎么做？', '提高复购率的方法'],
  admin:    ['本周数据趋势？', '异常订单排查', '商家违规处理建议', '用户增长分析'],
  default:  ['逛逛热门商品', '有什么好物推荐？', '帮我找个礼物', '看看新品']
}

// 初始化问题池
const initQuestions = () => {
  const cfg = PAGE_CONFIG[currentPageKey.value] || PAGE_CONFIG.default
  activeQuestions.value = [...cfg.questions]
  usedQuestions.value.clear()
}
initQuestions()

// 可见的快捷问题（排除已点击的）
const visibleQuickQuestions = computed(() =>
  activeQuestions.value.filter(q => !usedQuestions.value.has(q)).slice(0, 4)
)

// 点击快捷问题后：标记为已用，发送消息
const onQuickClick = (q) => {
  usedQuestions.value.add(q)
  sendMsg(q)
}

// AI 回复后，刷新为跟进问题
const refreshFollowups = () => {
  const pool = FOLLOWUP_POOL[currentPageKey.value] || FOLLOWUP_POOL.default
  const unused = pool.filter(q => !usedQuestions.value.has(q))
  if (unused.length > 0) {
    activeQuestions.value = unused
  } else {
    activeQuestions.value = []
  }
}

const buildPageContext = () => {
  const key = currentPageKey.value
  const ctx = { page: key }

  if (key === 'product') {
    const el = document.querySelector('.product-title-text, .product-detail h1, [class*="product"] [class*="title"]')
    const priceEl = document.querySelector('.product-price, [class*="price"] .amount, [class*="price"]')
    if (el) ctx.productName = el.textContent?.trim()
    if (priceEl) ctx.productPrice = priceEl.textContent?.trim()
    ctx.productId = route.params.id
  }

  if (key === 'search') {
    ctx.searchKeyword = route.query.kw || ''
  }

  if (key === 'cart') {
    const items = document.querySelectorAll('.cart-item, [class*="cart"] tr, .el-table__row')
    ctx.cartItemCount = items.length
  }

  return ctx
}

// 页面切换时：刷新快捷问题 + 插入场景提示
let lastPageKey = ''
watch(currentPageKey, (newKey) => {
  // 刷新快捷问题池
  usedQuestions.value.clear()
  const cfg = PAGE_CONFIG[newKey] || PAGE_CONFIG.default
  activeQuestions.value = [...cfg.questions]

  if (chatOpen.value && newKey !== lastPageKey && messages.value.length > 1) {
    const hint = PAGE_CONFIG[newKey]?.hint || ''
    if (hint && newKey !== 'default') {
      messages.value.push({
        role: 'system',
        text: `📍 你来到了新页面 — ${hint}`,
        products: []
      })
      scrollToBottom()
    }
  }
  lastPageKey = newKey
})

const messages = ref([
  {
    role: 'ai',
    text: '你好！我是小A，你的 AI 购物助手 🛒\n有什么想找的商品？直接告诉我就好～',
    products: []
  }
])

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const sendMsg = async (text) => {
  const content = text || inputText.value
  if (!content || !content.trim()) return

  messages.value.push({ role: 'user', text: content.trim(), products: [] })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  const historyForApi = messages.value
    .filter(m => m.role === 'user' || m.role === 'ai')
    .slice(-10)
    .map(m => ({ role: m.role, content: m.text }))

  const pageContext = buildPageContext()

  try {
    const { data: res } = await axios.post(`${BASE}/ai/chat`, {
      message: content.trim(),
      history: historyForApi,
      pageContext
    })

    if (res.code === 1 && res.data) {
      messages.value.push({
        role: 'ai',
        text: res.data.text || '抱歉，我没理解你的意思。',
        products: res.data.products || []
      })
    } else {
      messages.value.push({
        role: 'ai',
        text: res.msg || '出了点问题，请稍后再试。',
        products: []
      })
    }
  } catch {
    messages.value.push({
      role: 'ai',
      text: '网络不太好，请稍后重试～',
      products: []
    })
  } finally {
    loading.value = false
    refreshFollowups()
    scrollToBottom()
  }
}

const clearChat = () => {
  messages.value = [
    {
      role: 'ai',
      text: '聊天已清空～有什么新需求随时说！',
      products: []
    }
  ]
  initQuestions()
}

const goProduct = (id) => {
  router.push(`/product/${id}`)
}
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ── 悬浮按钮 ── */
.ai-fab {
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 20px;
  border-radius: 50px;
  cursor: grab;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.45);
  transition: box-shadow 0.3s, transform 0.3s;
  user-select: none;
  touch-action: none;
}
.ai-fab:active { cursor: grabbing; }
.ai-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(102, 126, 234, 0.6);
}
.fab-icon { display: flex; }
.fab-label { font-size: 14px; font-weight: 600; }

/* ── 聊天面板 ── */
.chat-panel {
  width: 380px;
  height: 560px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: grab;
  user-select: none;
}
.chat-header:active { cursor: grabbing; }
.header-left { display: flex; align-items: center; gap: 10px; }
.avatar-dot {
  width: 36px; height: 36px;
  background: rgba(255,255,255,0.25);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  position: relative;
}
.avatar-dot::after {
  content: 'A';
  font-weight: bold;
  font-size: 16px;
}
.header-title { font-size: 15px; font-weight: 600; }
.header-sub { font-size: 11px; opacity: 0.8; margin-top: 1px; }
.header-actions { display: flex; gap: 4px; }
.icon-btn {
  background: rgba(255,255,255,0.15);
  border: none;
  color: white;
  width: 32px; height: 32px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.icon-btn:hover { background: rgba(255,255,255,0.3); }

/* ── 消息区 ── */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8f9fb;
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 4px; }

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 100%;
}
.msg-row.user { flex-direction: row-reverse; }

.msg-avatar {
  width: 30px; height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}
.ai-avatar { background: linear-gradient(135deg, #667eea, #764ba2); color: white; }
.user-avatar { background: #f0c14b; color: #333; }

.msg-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.6;
}
.msg-row.ai .msg-bubble {
  background: white;
  color: #333;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-top-right-radius: 4px;
}
.msg-row.system {
  justify-content: center;
}
.msg-row.system .msg-bubble {
  background: transparent;
  color: #999;
  font-size: 12px;
  padding: 4px 12px;
  max-width: 100%;
  text-align: center;
  box-shadow: none;
}
.msg-text { white-space: pre-wrap; word-break: break-word; }

/* ── 商品卡片 ── */
.product-cards {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.product-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8f9fb;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.product-card:hover {
  border-color: #667eea;
  background: #f0f2ff;
}
.p-img {
  width: 48px; height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background: white;
  flex-shrink: 0;
}
.p-info { flex: 1; min-width: 0; }
.p-title {
  font-size: 12.5px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-price { font-size: 14px; color: #e44; font-weight: 600; margin-top: 2px; }
.p-arrow { flex-shrink: 0; }

/* ── 打字动画 ── */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-indicator span {
  width: 7px; height: 7px;
  background: #bbb;
  border-radius: 50%;
  animation: typingBounce 1.4s infinite ease-in-out;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* ── 快捷提问 ── */
.quick-actions {
  padding: 0 16px 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  background: #f8f9fb;
  position: relative;
}
.quick-label {
  width: 100%;
  font-size: 11.5px;
  color: #888;
  margin-bottom: 2px;
}
.quick-btn {
  background: white;
  border: 1px solid #e0e3eb;
  color: #555;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-btn:hover {
  border-color: #667eea;
  color: #667eea;
  background: #f0f2ff;
}
.chip-enter-active { animation: chipIn 0.25s ease-out; }
.chip-leave-active { animation: chipOut 0.2s ease-in; position: absolute; }
.chip-move { transition: transform 0.25s ease; }
@keyframes chipIn {
  from { transform: scale(0.8); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
}
@keyframes chipOut {
  from { opacity: 1; transform: scale(1); }
  to   { opacity: 0; transform: scale(0.6); }
}

/* ── 输入区 ── */
.chat-input {
  display: flex;
  padding: 12px 14px;
  gap: 8px;
  background: white;
  border-top: 1px solid #f0f0f0;
}
.chat-input input {
  flex: 1;
  border: 1px solid #e0e3eb;
  border-radius: 22px;
  padding: 10px 16px;
  font-size: 13.5px;
  outline: none;
  transition: border-color 0.2s;
}
.chat-input input:focus { border-color: #667eea; }
.send-btn {
  width: 40px; height: 40px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #667eea, #764ba2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.send-btn:not(:disabled):hover { opacity: 0.85; }

/* ── 动画 ── */
.slide-up-enter-active { animation: slideUp 0.3s ease-out; }
.slide-up-leave-active { animation: slideUp 0.2s ease-in reverse; }
@keyframes slideUp {
  from { transform: translateY(20px) scale(0.95); opacity: 0; }
  to   { transform: translateY(0) scale(1); opacity: 1; }
}
.bounce-enter-active { animation: bounceIn 0.4s; }
.bounce-leave-active { animation: bounceIn 0.2s reverse; }
@keyframes bounceIn {
  0%   { transform: scale(0); opacity: 0; }
  50%  { transform: scale(1.1); }
  100% { transform: scale(1); opacity: 1; }
}
</style>
