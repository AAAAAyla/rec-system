<template>
  <div>
    <h2>商品审核</h2>
    <el-tabs v-model="status" @tab-change="load">
      <el-tab-pane label="待审核" name="2" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已下架" name="0" />
    </el-tabs>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="图片" width="80">
        <template #default="{ row }">
          <el-image :src="row.image_url" style="width:50px;height:50px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="商品名称" />
      <el-table-column prop="shop_name" label="店铺" width="140" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column label="操作" width="180" v-if="status === '2'">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="audit(row.id, 1)">通过</el-button>
          <el-button type="danger" size="small" @click="audit(row.id, 0)">拒绝</el-button>
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
import { getAdminItems, auditItem } from '../../api/admin'

const list = ref([]), total = ref(0), pageNum = ref(1), status = ref('2')

const load = async () => {
  const { data: res } = await getAdminItems({ status: status.value, pageNum: pageNum.value, pageSize: 20 })
  if (res.code === 1) { list.value = res.data.rows; total.value = res.data.total }
}

const audit = async (id, s) => {
  await auditItem(id, s)
  ElMessage.success(s === 1 ? '已通过' : '已拒绝')
  load()
}

onMounted(load)
</script>
