<template>
  <div class="apply-page">
    <el-page-header @back="$router.push('/home')" title="返回首页" class="header">
      <template #content>申请入驻商家</template>
    </el-page-header>

    <!-- 已有申请：展示状态 -->
    <el-card v-if="myInfo" class="status-card">
      <div class="status-header">
        <el-icon size="40" :color="statusColor"><Shop /></el-icon>
        <div>
          <div class="status-shop-name">{{ myInfo.shopName }}</div>
          <el-tag :type="statusType" size="large">{{ statusLabel }}</el-tag>
          <div v-if="myInfo.rejectReason" class="reject-reason">
            拒绝原因：{{ myInfo.rejectReason }}
          </div>
        </div>
      </div>

      <el-descriptions :column="2" border style="margin-top:20px">
        <el-descriptions-item label="店铺简介">{{ myInfo.shopDesc || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ myInfo.contactPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="营业执照">
          <a v-if="myInfo.licenseUrl" :href="myInfo.licenseUrl" target="_blank">查看执照</a>
          <span v-else>未上传</span>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ myInfo.createTime }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:20px" v-if="myInfo.status === 1">
        <el-button type="primary" @click="$router.push('/merchant')">进入商家后台</el-button>
      </div>
      <div style="margin-top:20px" v-if="myInfo.status === 2">
        <el-button type="warning" @click="myInfo = null">重新申请</el-button>
      </div>
    </el-card>

    <!-- 未申请：填写申请表 -->
    <el-card v-else class="form-card">
      <div class="form-intro">
        <el-icon size="24" color="#409eff"><InfoFilled /></el-icon>
        <span>填写信息后，管理员将在 1-3 个工作日内审核您的申请</span>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px" style="margin-top:20px">
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="店铺简介">
          <el-input
            v-model="form.shopDesc"
            type="textarea"
            :rows="3"
            placeholder="简单介绍您的店铺，最多 200 字"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>

        <el-form-item label="营业执照 URL" prop="licenseUrl">
          <el-input
            v-model="form.licenseUrl"
            placeholder="请输入营业执照图片地址（上传到图床后填写）"
          />
          <div class="field-hint">
            <el-icon><InfoFilled /></el-icon>
            暂不支持直接上传，请先将执照图片上传至图床（如 sm.ms、imgbb），填写图片链接
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="submit">
            提交申请
          </el-button>
          <el-button size="large" @click="$router.push('/home')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Shop, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { applyMerchant, getMyMerchantInfo } from '../../api/merchant'

const router    = useRouter()
const formRef   = ref()
const submitting = ref(false)
const myInfo    = ref(null)

const form = ref({
  shopName:     '',
  shopDesc:     '',
  contactPhone: '',
  licenseUrl:   '',
})

const rules = {
  shopName:     [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  licenseUrl:   [{ required: true, message: '请提供营业执照链接', trigger: 'blur' }],
}

const STATUS = {
  0: { label: '待审核', type: 'warning', color: '#e6a23c' },
  1: { label: '审核通过 - 正常营业', type: 'success', color: '#67c23a' },
  2: { label: '审核拒绝', type: 'danger', color: '#f56c6c' },
  3: { label: '已封禁', type: 'danger', color: '#f56c6c' },
}
const statusLabel = computed(() => STATUS[myInfo.value?.status]?.label || '未知')
const statusType  = computed(() => STATUS[myInfo.value?.status]?.type  || 'info')
const statusColor = computed(() => STATUS[myInfo.value?.status]?.color || '#909399')

const submit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const { data: res } = await applyMerchant(form.value)
    if (res.code === 1) {
      ElMessage.success('申请已提交，等待管理员审核')
      loadMyInfo()
    } else {
      ElMessage.error(res.msg)
    }
  } finally {
    submitting.value = false
  }
}

const loadMyInfo = async () => {
  try {
    const { data: res } = await getMyMerchantInfo()
    if (res.code === 1) myInfo.value = res.data
  } catch {
    // 未申请过，正常忽略
  }
}

onMounted(loadMyInfo)
</script>

<style scoped>
.apply-page { max-width: 800px; margin: 20px auto; padding: 0 16px; }
.header { margin-bottom: 20px; }
.status-card, .form-card { margin-top: 20px; }
.status-header { display: flex; align-items: center; gap: 20px; }
.status-shop-name { font-size: 20px; font-weight: bold; margin-bottom: 8px; }
.reject-reason { color: #f56c6c; font-size: 13px; margin-top: 8px; }
.form-intro { display: flex; align-items: center; gap: 8px; color: #409eff; font-size: 14px; background: #ecf5ff; padding: 10px 14px; border-radius: 6px; }
.field-hint { font-size: 12px; color: #909399; margin-top: 6px; display: flex; align-items: center; gap: 4px; }
</style>
