<template>
  <div class="im-page">
    <div class="im-sidebar">
      <div class="sidebar-header">消息</div>
      <div v-for="s in sessions" :key="s.id" class="session-item"
           :class="{ active: currentSession?.id === s.id }" @click="selectSession(s)">
        <div class="session-avatar merchant-av">{{ (s.chat_name || '商')[0] }}</div>
        <div class="session-info">
          <div class="session-name">{{ s.chat_name || '商家' }}</div>
          <div class="session-msg">{{ s.last_message || '暂无消息' }}</div>
        </div>
        <el-badge v-if="s.unread_user > 0" :value="s.unread_user" class="session-badge" />
      </div>
      <el-empty v-if="sessions.length === 0" description="暂无会话" :image-size="60" />
    </div>

    <div class="im-chat">
      <template v-if="currentSession">
        <div class="chat-top-bar">
          <span class="chat-top-name">{{ currentSession.chat_name || '商家' }}</span>
          <span class="chat-top-hint">在线</span>
        </div>
        <div class="chat-messages" ref="msgContainer">
          <!-- 时间分隔 -->
          <template v-for="(msg, idx) in messages" :key="msg.id">
            <div v-if="shouldShowTime(idx)" class="time-divider">
              {{ formatTime(msg.created_at) }}
            </div>
            <div :class="['msg-row', msg.sender_type === 'user' ? 'is-self' : 'is-other']">
              <div class="msg-avatar" :class="msg.sender_type === 'user' ? 'user-av' : 'merchant-av'">
                {{ msg.sender_type === 'user' ? (myName?.[0] || '我') : (currentSession.chat_name?.[0] || '商') }}
              </div>
              <div class="msg-body">
                <div class="msg-bubble">{{ msg.content }}</div>
              </div>
            </div>
          </template>
        </div>
        <div class="chat-input-bar">
          <el-input
            v-model="inputMsg"
            placeholder="输入消息..."
            @keyup.enter="send"
            :autosize="{ minRows: 1, maxRows: 3 }"
            type="textarea"
            resize="none"
          />
          <el-button type="primary" @click="send" :disabled="!inputMsg.trim()" class="send-btn">
            发送
          </el-button>
        </div>
      </template>
      <div v-else class="empty-chat">
        <div class="empty-icon">💬</div>
        <div class="empty-text">选择一个会话开始聊天</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { getSessions, getMessages, sendMessage, markRead } from '../../api/im'
import { useUserStore } from '../../store/userStore'

const userStore = useUserStore()
const myName = computed(() => userStore.user?.username || '我')

const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputMsg = ref('')
const msgContainer = ref(null)
let ws = null

onMounted(async () => {
  const { data: res } = await getSessions('user')
  if (res.code === 1) sessions.value = res.data

  const token = localStorage.getItem('token')
  const base = (import.meta.env.VITE_API_BASE || 'http://localhost:8080').replace('http', 'ws')
  ws = new WebSocket(`${base}/ws/im?token=${token}`)
  ws.onmessage = (e) => {
    const msg = JSON.parse(e.data)
    if (currentSession.value && msg.sessionId == currentSession.value.id) {
      messages.value.push(msg)
      scrollToBottom()
    }
    loadSessions()
  }
})

const loadSessions = async () => {
  const { data: res } = await getSessions('user')
  if (res.code === 1) sessions.value = res.data
}

const selectSession = async (s) => {
  currentSession.value = s
  const { data: res } = await getMessages(s.id, 1, 200)
  if (res.code === 1) messages.value = res.data.rows
  markRead(s.id, 'user')
  s.unread_user = 0
  scrollToBottom()
}

const send = async () => {
  if (!inputMsg.value.trim()) return
  const { data: res } = await sendMessage(currentSession.value.id, 'user', inputMsg.value)
  if (res.code === 1) {
    messages.value.push(res.data)
    inputMsg.value = ''
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => { if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight })
}

const shouldShowTime = (idx) => {
  if (idx === 0) return true
  const curr = new Date(messages.value[idx].created_at).getTime()
  const prev = new Date(messages.value[idx - 1].created_at).getTime()
  return curr - prev > 5 * 60 * 1000
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const today = new Date()
  if (d.toDateString() === today.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.im-page { display: flex; height: calc(100vh - 120px); border: 1px solid #e4e7ed; border-radius: 12px; overflow: hidden; background: #fff; }

/* ── 侧边栏 ── */
.im-sidebar { width: 300px; border-right: 1px solid #ebeef5; overflow-y: auto; background: #fafbfc; }
.sidebar-header { padding: 16px 20px; font-size: 18px; font-weight: 700; color: #303133; border-bottom: 1px solid #ebeef5; }
.session-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; cursor: pointer; border-bottom: 1px solid #f5f5f5;
  position: relative; transition: background 0.15s;
}
.session-item:hover { background: #f0f7ff; }
.session-item.active { background: #ecf5ff; border-left: 3px solid #409eff; }
.session-avatar {
  width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: #fff;
}
.merchant-av { background: linear-gradient(135deg, #f093fb, #f5576c); }
.user-av { background: linear-gradient(135deg, #667eea, #764ba2); }
.session-info { flex: 1; min-width: 0; }
.session-name { font-weight: 600; font-size: 14px; color: #303133; }
.session-msg { font-size: 12px; color: #909399; margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-badge { position: absolute; top: 12px; right: 16px; }

/* ── 聊天区 ── */
.im-chat { flex: 1; display: flex; flex-direction: column; background: #f8f9fb; }
.chat-top-bar {
  padding: 14px 24px; background: #fff; border-bottom: 1px solid #ebeef5;
  display: flex; align-items: baseline; gap: 8px;
}
.chat-top-name { font-size: 16px; font-weight: 700; color: #303133; }
.chat-top-hint { font-size: 12px; color: #67c23a; }

.chat-messages { flex: 1; overflow-y: auto; padding: 20px 24px; }
.chat-messages::-webkit-scrollbar { width: 5px; }
.chat-messages::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 4px; }

.time-divider {
  text-align: center; font-size: 11px; color: #b0b3b8; margin: 16px 0 8px;
  position: relative;
}

/* ── 消息行 ── */
.msg-row { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 14px; }
.msg-row.is-self { flex-direction: row-reverse; }

.msg-avatar {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #fff;
}

.msg-body { max-width: 65%; }
.msg-bubble {
  padding: 10px 16px; border-radius: 18px; font-size: 14px; line-height: 1.6;
  word-break: break-word; white-space: pre-wrap;
}
.is-other .msg-bubble {
  background: #fff; color: #303133; border-top-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.is-self .msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2); color: #fff;
  border-top-right-radius: 4px;
}

/* ── 输入框 ── */
.chat-input-bar {
  display: flex; gap: 10px; padding: 14px 20px;
  border-top: 1px solid #ebeef5; background: #fff; align-items: flex-end;
}
.chat-input-bar .el-textarea { flex: 1; }
.chat-input-bar .send-btn { height: 36px; border-radius: 18px; padding: 0 24px; }

/* ── 空态 ── */
.empty-chat {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-text { font-size: 15px; color: #909399; }
</style>
