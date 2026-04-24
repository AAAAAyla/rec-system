<template>
  <div>
    <h2>订单监控</h2>
    <el-tabs v-model="status" @tab-change="load">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
      <el-tab-pane label="售后中" name="5" />
    </el-tabs>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="order_no" label="订单号" width="180" />
      <el-table-column prop="buyer_name" label="买家" width="120" />
      <el-table-column prop="shop_name" label="店铺" width="140" />
      <el-table-column prop="total_amount" label="金额" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag>{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="create_time" label="下单时间" width="180" />
    </el-table>
    <el-pagination layout="prev, pager, next" :total="total" :page-size="20"
      v-model:current-page="pageNum" @current-change="load" style="margin-top:16px" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminOrders } from '../../api/admin'

const statusMap = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '售后中', 6: '已退款' }
const list = ref([]), total = ref(0), pageNum = ref(1), status = ref('')

const load = async () => {
  const params = { pageNum: pageNum.value, pageSize: 20 }
  if (status.value !== '') params.status = status.value
  const { data: res } = await getAdminOrders(params)
  if (res.code === 1) { list.value = res.data.rows; total.value = res.data.total }
}

onMounted(load)
</script>
