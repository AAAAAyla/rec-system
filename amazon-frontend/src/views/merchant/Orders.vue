<!-- amazon-frontend/src/views/merchant/Orders.vue -->
<template>
  <div>
    <div class="page-title">订单管理</div>

    <el-tabs v-model="activeTab" @tab-change="onTab">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="退款中" name="5" />
    </el-tabs>

    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column label="订单号" width="180" prop="orderNo" />
      <el-table-column label="商品" min-width="200">
        <template #default="{ row }">
          <div v-for="it in row.orderItems" :key="it.id" style="font-size:13px">
            {{ it.title }} x{{ it.quantity }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><b style="color:#f56c6c">¥{{ row.payAmount }}</b></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="170" prop="createTime" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status===1" type="primary" size="small" @click="openShip(row)">发货</el-button>
          <el-button v-if="row.status===5" type="success" size="small" @click="doAgree(row)">同意退款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum" :total="total" :page-size="pageSize"
        layout="total, prev, pager, next"
        style="margin-top:16px;display:flex;justify-content:flex-end"
        @current-change="load"
    />

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialog" title="填写物流信息" width="450px">
      <el-form label-width="80px">
        <el-form-item label="快递公司">
          <el-select v-model="shipForm.expressCompany" placeholder="选择快递公司">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通快递" value="圆通快递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="京东物流" value="京东物流" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="shipForm.trackingNo" placeholder="输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialog = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantOrders, shipOrder, agreeRefund } from '../../api/order'

const orders = ref([]), loading = ref(false), total = ref(0)
const pageNum = ref(1), pageSize = 10, activeTab = ref('all')
const shipDialog = ref(false), shipping = ref(false)
const shipForm = ref({ expressCompany: '', trackingNo: '' })
const currentOrder = ref(null)

const STATUS_MAP = { 0:'待付款',1:'待发货',2:'待收货',3:'已完成',4:'已取消',5:'退款中',6:'已退款' }
const TYPE_MAP = { 0:'warning',1:'primary',2:'',3:'success',4:'info',5:'danger',6:'info' }
const statusText = (s) => STATUS_MAP[s] || '未知'
const statusType = (s) => TYPE_MAP[s] || 'info'

const load = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (activeTab.value !== 'all') params.status = parseInt(activeTab.value)
    const { data: res } = await getMerchantOrders(params)
    if (res.code === 1) { orders.value = res.data.rows; total.value = res.data.total }
  } finally { loading.value = false }
}

const onTab = () => { pageNum.value = 1; load() }

const openShip = (order) => {
  currentOrder.value = order
  shipForm.value = { expressCompany: '', trackingNo: '' }
  shipDialog.value = true
}

const doShip = async () => {
  if (!shipForm.value.expressCompany || !shipForm.value.trackingNo) {
    return ElMessage.warning('请填写完整物流信息')
  }
  shipping.value = true
  try {
    const { data: res } = await shipOrder(currentOrder.value.id, shipForm.value)
    if (res.code === 1) { ElMessage.success('发货成功'); shipDialog.value = false; load() }
    else ElMessage.error(res.msg)
  } finally { shipping.value = false }
}

const doAgree = async (order) => {
  await ElMessageBox.confirm('确认同意退款？金额将原路返回', '退款确认')
  const { data: res } = await agreeRefund(order.id)
  if (res.code === 1) { ElMessage.success('已同意退款'); load() }
  else ElMessage.error(res.msg)
}

onMounted(load)
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: bold; color: #303133; margin-bottom: 16px; }
</style>
