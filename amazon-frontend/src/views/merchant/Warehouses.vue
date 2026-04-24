<template>
  <div>
    <div class="page-title">发货地管理</div>
    <el-button type="primary" @click="openDialog()" style="margin-bottom:16px">新增仓库</el-button>
    <el-table :data="list" border>
      <el-table-column prop="name" label="仓库名称" />
      <el-table-column label="地址">
        <template #default="{ row }">{{ row.province }}{{ row.city }}{{ row.district }}{{ row.detail }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column label="默认" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.is_default" type="success">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text type="warning" size="small" @click="handleDefault(row.id)" v-if="!row.is_default">设为默认</el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editId ? '编辑仓库' : '新增仓库'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="省"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="区"><el-input v-model="form.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getWarehouses, addWarehouse, updateWarehouse, deleteWarehouse, setDefaultWarehouse } from '../../api/warehouse'

const list = ref([])
const showDialog = ref(false)
const editId = ref(null)
const form = ref({ name: '', province: '', city: '', district: '', detail: '', phone: '' })

const load = async () => {
  const { data: res } = await getWarehouses()
  if (res.code === 1) list.value = res.data
}

const openDialog = (row) => {
  if (row) {
    editId.value = row.id
    form.value = { name: row.name, province: row.province, city: row.city, district: row.district, detail: row.detail, phone: row.phone }
  } else {
    editId.value = null
    form.value = { name: '', province: '', city: '', district: '', detail: '', phone: '' }
  }
  showDialog.value = true
}

const handleSave = async () => {
  const { data: res } = editId.value
    ? await updateWarehouse(editId.value, form.value)
    : await addWarehouse(form.value)
  if (res.code === 1) { ElMessage.success('保存成功'); showDialog.value = false; load() }
  else ElMessage.error(res.msg)
}

const handleDelete = async (id) => {
  await deleteWarehouse(id); ElMessage.success('已删除'); load()
}

const handleDefault = async (id) => {
  await setDefaultWarehouse(id); ElMessage.success('已设为默认'); load()
}

onMounted(load)
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: bold; margin-bottom: 20px; }
</style>
