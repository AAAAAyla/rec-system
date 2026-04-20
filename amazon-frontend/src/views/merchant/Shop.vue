<!-- amazon-frontend/src/views/merchant/Shop.vue -->
<template>
  <div>
    <div class="page-title">店铺设置</div>

    <el-card v-loading="loading">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">

        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="店铺简介">
          <el-input
              v-model="form.shopDesc"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="审核状态">
          <el-tag :type="statusType">{{ statusText }}</el-tag>
          <span v-if="merchantInfo?.rejectReason" style="margin-left:8px;color:#f56c6c;font-size:13px">
            拒绝原因：{{ merchantInfo.rejectReason }}
          </span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">保存设置</el-button>
        </el-form-item>

      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyMerchantInfo, updateShopInfo } from '../../api/merchant'

const loading    = ref(false)
const submitting = ref(false)
const formRef    = ref()
const merchantInfo = ref(null)

const form = ref({
  shopName:     '',
  shopDesc:     '',
  contactPhone: '',
})

const rules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
}

const statusType = computed(() => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'danger' }
  return map[merchantInfo.value?.status] || 'info'
})

const statusText = computed(() => {
  const map = { 0: '待审核', 1: '正常营业', 2: '审核拒绝', 3: '已封禁' }
  return map[merchantInfo.value?.status] || '未知'
})

const submit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const { data: res } = await updateShopInfo(form.value)
    if (res.code === 1) ElMessage.success('保存成功')
    else ElMessage.error(res.msg)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const { data: res } = await getMyMerchantInfo()
    if (res.code === 1) {
      merchantInfo.value = res.data
      form.value.shopName     = res.data.shopName     || ''
      form.value.shopDesc     = res.data.shopDesc     || ''
      form.value.contactPhone = res.data.contactPhone || ''
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: bold; color: #303133; margin-bottom: 20px; }
</style>