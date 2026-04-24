<template>
  <div class="cat-page">
    <div class="page-title">分类管理</div>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>一级分类</span>
          <el-button type="primary" size="small" @click="openAdd(0)">新增一级分类</el-button>
        </div>
      </template>

      <el-table :data="tree" row-key="id" border default-expand-all>
        <el-table-column label="图标" width="60">
          <template #default="{ row }">{{ row.iconUrl || row.icon_url || '—' }}</template>
        </el-table-column>
        <el-table-column label="名称" prop="name" min-width="160" />
        <el-table-column label="层级" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.level === 1 ? '' : 'success'">{{ row.level === 1 ? '一级' : '二级' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" width="80" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.level === 1" type="primary" text size="small" @click="openAdd(row.id)">添加子分类</el-button>
            <el-button type="warning" text size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="doDelete(row.id)">
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.iconUrl" placeholder="如 📚 或图片URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
const tree = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const editId = ref(null)
const form = ref({ name: '', iconUrl: '', sort: 0, parentId: 0 })

const load = async () => {
  const { data: res } = await axios.get(`${BASE}/categories/tree`)
  if (res.code === 1) {
    tree.value = (res.data || []).map(c => ({
      ...c,
      children: (c.children || []).map(sub => ({ ...sub, children: undefined }))
    }))
  }
}

const openAdd = (parentId) => {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', iconUrl: '', sort: 0, parentId }
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.value = { name: row.name, iconUrl: row.iconUrl || row.icon_url || '', sort: row.sort || 0, parentId: row.parentId || row.parent_id || 0 }
  dialogVisible.value = true
}

const doSave = async () => {
  if (!form.value.name.trim()) return ElMessage.warning('请输入名称')
  saving.value = true
  try {
    const { data: res } = isEdit.value
      ? await axios.put(`${BASE}/categories/${editId.value}`, form.value)
      : await axios.post(`${BASE}/categories`, form.value)
    if (res.code === 1) {
      ElMessage.success(isEdit.value ? '已更新' : '已创建')
      dialogVisible.value = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  } finally { saving.value = false }
}

const doDelete = async (id) => {
  const { data: res } = await axios.delete(`${BASE}/categories/${id}`)
  if (res.code === 1) { ElMessage.success('已删除'); load() }
  else ElMessage.error(res.msg)
}

onMounted(load)
</script>

<style scoped>
.cat-page { max-width: 900px; margin: 20px auto; padding: 0 16px; }
.page-title { font-size: 22px; font-weight: bold; margin-bottom: 20px; }
</style>
