<template>
  <div>
    <h2>用户管理</h2>
    <el-input v-model="kw" placeholder="搜索用户名/手机号" style="width:300px;margin-bottom:16px" clearable @clear="load" @keyup.enter="load">
      <template #append><el-button @click="load">搜索</el-button></template>
    </el-input>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">{{ { 0:'买家',1:'商家',2:'管理员' }[row.role] }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" text type="danger" size="small" @click="toggle(row, 0)">封禁</el-button>
          <el-button v-else text type="success" size="small" @click="toggle(row, 1)">解封</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination layout="prev, pager, next" :total="total" :page-size="20"
      v-model:current-page="pageNum" @current-change="load" style="margin-top:16px" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, setUserStatus } from '../../api/admin'

const list = ref([]), total = ref(0), pageNum = ref(1), kw = ref('')

const load = async () => {
  const { data: res } = await getUsers({ kw: kw.value, pageNum: pageNum.value, pageSize: 20 })
  if (res.code === 1) { list.value = res.data.rows; total.value = res.data.total }
}

const toggle = async (row, status) => {
  await setUserStatus(row.id, status)
  row.status = status
  ElMessage.success(status === 1 ? '已解封' : '已封禁')
}

onMounted(load)
</script>
