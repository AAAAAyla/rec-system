<!-- amazon-frontend/src/views/merchant/Items.vue -->
<template>
  <div>
    <div class="page-header">
      <span class="page-title">商品管理</span>
      <el-button type="primary" :icon="Plus" @click="router.push('/merchant/items/new')">发布商品</el-button>
    </div>

    <el-card>
      <el-table :data="items" v-loading="loading" stripe>
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" style="width:50px;height:50px;border-radius:4px">
              <template #error><div style="width:50px;height:50px;background:#f5f5f5;border-radius:4px" /></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="价格" width="100">
          <template #default="{ row }"><span style="color:#f56c6c;font-weight:bold">¥{{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="salesCount" label="销量" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':row.status===2?'warning':'info'" size="small">
              {{ row.status===1?'在售':row.status===2?'待审核':'已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/merchant/items/${row.id}/edit`)">编辑</el-button>
            <el-button size="small" :type="row.status===1?'warning':'success'" @click="toggle(row)">
              {{ row.status===1?'下架':'上架' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="pageNum" :total="total" :page-size="pageSize"
          layout="total, prev, pager, next"
          style="margin-top:16px;display:flex;justify-content:flex-end"
          @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantItems, changeItemStatus } from '../../api/item'

const router = useRouter()
const items = ref([]), loading = ref(false), total = ref(0), pageNum = ref(1), pageSize = 10

const load = async () => {
  loading.value = true
  try {
    const { data: res } = await getMerchantItems({ pageNum: pageNum.value, pageSize })
    if (res.code === 1) { items.value = res.data.rows; total.value = res.data.total }
  } finally { loading.value = false }
}

const toggle = async (row) => {
  const s = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${s?'上架':'下架'}「${row.title}」？`, '提示')
  const { data: res } = await changeItemStatus(row.id, s)
  if (res.code === 1) { ElMessage.success(s?'已上架':'已下架'); row.status = s }
}

onMounted(load)
</script>

<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-title { font-size:22px; font-weight:bold; color:#303133; }
</style>