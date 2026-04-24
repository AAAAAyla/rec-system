<template>
  <div class="notif-page">
    <div class="notif-header">
      <h2>消息通知</h2>
      <el-button text type="primary" @click="handleReadAll">全部已读</el-button>
    </div>
    <el-empty v-if="list.length === 0" description="暂无通知" />
    <div v-for="item in list" :key="item.id" class="notif-item" :class="{ unread: !item.is_read }">
      <div class="notif-content">
        <div class="notif-title">{{ item.title }}</div>
        <div class="notif-desc">{{ item.content }}</div>
        <div class="notif-time">{{ item.created_at }}</div>
      </div>
      <el-button v-if="!item.is_read" text size="small" @click="handleRead(item)">标记已读</el-button>
    </div>
    <el-pagination v-if="total > 20" layout="prev, pager, next" :total="total"
      :page-size="20" v-model:current-page="pageNum" @current-change="load" style="margin-top:20px" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifications, markRead, markAllRead } from '../../api/notification'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)

const load = async () => {
  const { data: res } = await getNotifications({ pageNum: pageNum.value, pageSize: 20 })
  if (res.code === 1) { list.value = res.data.rows; total.value = res.data.total }
}

const handleRead = async (item) => {
  await markRead(item.id)
  item.is_read = 1
}

const handleReadAll = async () => {
  await markAllRead()
  list.value.forEach(i => i.is_read = 1)
  ElMessage.success('已全部标为已读')
}

onMounted(load)
</script>

<style scoped>
.notif-page { max-width: 800px; margin: 0 auto; padding: 20px; }
.notif-header { display: flex; justify-content: space-between; align-items: center; }
.notif-item { display: flex; justify-content: space-between; align-items: center; padding: 16px; border-bottom: 1px solid #f0f0f0; }
.notif-item.unread { background: #f0f9ff; }
.notif-title { font-weight: 600; }
.notif-desc { font-size: 13px; color: #606266; margin-top: 4px; }
.notif-time { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
</style>
