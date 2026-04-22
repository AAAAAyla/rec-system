<template>
  <div class="audit-page">
    <div class="page-title">
      <el-icon><Shop /></el-icon> 商家申请审核
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已拒绝" name="2" />
    </el-tabs>

    <el-table :data="merchants" v-loading="loading" stripe border>
      <el-table-column label="店铺名称" prop="shopName" min-width="140" />
      <el-table-column label="联系电话" prop="contactPhone" width="130" />
      <el-table-column label="营业执照" width="100">
        <template #default="{ row }">
          <a v-if="row.licenseUrl" :href="row.licenseUrl" target="_blank" style="color:#409eff">查看</a>
          <span v-else style="color:#bbb">无</span>
        </template>
      </el-table-column>
      <el-table-column label="店铺简介" prop="shopDesc" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="180" fixed="right" v-if="activeTab === '0'">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="approve(row)">通过</el-button>
          <el-button type="danger" size="small" @click="openReject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="pageNum"
      :total="total"
      :page-size="pageSize"
      layout="total, prev, pager, next"
      style="margin-top:16px;display:flex;justify-content:flex-end"
      @current-change="load"
    />

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectDialog" title="填写拒绝原因" width="440px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请填写拒绝原因（将发送给申请人）"
      />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="operating" @click="doReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Shop } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMerchants, auditMerchant } from '../../api/merchant'

const merchants   = ref([])
const loading     = ref(false)
const operating   = ref(false)
const total       = ref(0)
const pageNum     = ref(1)
const pageSize    = 10
const activeTab   = ref('0')
const rejectDialog = ref(false)
const rejectReason = ref('')
const currentRow   = ref(null)

const STATUS = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已拒绝', type: 'danger' },
  3: { label: '已封禁', type: 'danger' },
}
const statusLabel = (s) => STATUS[s]?.label || '未知'
const statusType  = (s) => STATUS[s]?.type  || 'info'

const load = async () => {
  loading.value = true
  try {
    const { data: res } = await listMerchants(parseInt(activeTab.value), pageNum.value, pageSize)
    if (res.code === 1) {
      merchants.value = res.data.rows
      total.value     = res.data.total
    }
  } finally { loading.value = false }
}

const onTabChange = () => { pageNum.value = 1; load() }

const approve = async (row) => {
  await ElMessageBox.confirm(`确认通过「${row.shopName}」的入驻申请？`, '审核确认')
  operating.value = true
  try {
    const { data: res } = await auditMerchant(row.id, 1, '')
    if (res.code === 1) { ElMessage.success('已通过审核'); load() }
    else ElMessage.error(res.msg)
  } finally { operating.value = false }
}

const openReject = (row) => {
  currentRow.value  = row
  rejectReason.value = ''
  rejectDialog.value = true
}

const doReject = async () => {
  if (!rejectReason.value.trim()) return ElMessage.warning('请填写拒绝原因')
  operating.value = true
  try {
    const { data: res } = await auditMerchant(currentRow.value.id, 2, rejectReason.value)
    if (res.code === 1) { ElMessage.success('已拒绝申请'); rejectDialog.value = false; load() }
    else ElMessage.error(res.msg)
  } finally { operating.value = false }
}

onMounted(load)
</script>

<style scoped>
.audit-page { max-width: 1100px; margin: 20px auto; padding: 0 16px; }
.page-title { font-size: 22px; font-weight: bold; margin-bottom: 20px; display: flex; align-items: center; gap: 8px; }
</style>
