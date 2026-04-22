<!-- amazon-frontend/src/views/buyer/Addresses.vue -->
<template>
  <div class="addr-page">
    <div class="page-header">
      收货地址管理
      <el-button type="primary" @click="openForm()">+ 新增地址</el-button>
    </div>

    <div v-loading="loading" class="addr-list">
      <el-empty v-if="!addresses.length && !loading" description="还没有收货地址" />

      <div v-for="addr in addresses" :key="addr.id"
           :class="['addr-card', { 'is-default': addr.isDefault }]">
        <div class="addr-info">
          <div class="addr-name">
            {{ addr.name }}
            <span class="addr-phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault" size="small" type="success">默认</el-tag>
          </div>
          <div class="addr-detail">
            {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}
          </div>
        </div>
        <div class="addr-actions">
          <el-button v-if="!addr.isDefault" text type="success" @click="doSetDefault(addr.id)">设为默认</el-button>
          <el-button text type="primary" @click="openForm(addr)">编辑</el-button>
          <el-button text type="danger" @click="del(addr.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="showDialog" :title="editingAddr ? '编辑地址' : '新增地址'" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="收件人" prop="name">
          <el-input v-model="form.name" placeholder="收件人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" prop="province">
          <div style="display:flex;gap:8px">
            <el-input v-model="form.province" placeholder="省" style="flex:1" />
            <el-input v-model="form.city" placeholder="市" style="flex:1" />
            <el-input v-model="form.district" placeholder="区" style="flex:1" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="街道门牌号" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefaultBool" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../../api/address'

const route       = useRoute()
const router      = useRouter()
const addresses   = ref([])
const loading     = ref(false)
const saving      = ref(false)
const showDialog  = ref(false)
const editingAddr = ref(null)
const formRef     = ref()

const form = ref({ name:'', phone:'', province:'', city:'', district:'', detail:'', isDefaultBool: false })

const rules = {
  name:     [{ required: true, message: '请输入收件人' }],
  phone:    [{ required: true, message: '请输入手机号' }],
  province: [{ required: true, message: '请输入省份' }],
  detail:   [{ required: true, message: '请输入详细地址' }],
}

async function load() {
  loading.value = true
  try {
    const { data: res } = await getAddresses()
    if (res.code === 1) addresses.value = res.data
  } finally { loading.value = false }
}

function openForm(addr = null) {
  editingAddr.value = addr
  form.value = addr
      ? { ...addr, isDefaultBool: addr.isDefault === 1 }
      : { name:'', phone:'', province:'', city:'', district:'', detail:'', isDefaultBool: false }
  showDialog.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  const payload = { ...form.value, isDefault: form.value.isDefaultBool ? 1 : 0 }
  try {
    const { data: res } = editingAddr.value
        ? await updateAddress(editingAddr.value.id, payload)
        : await addAddress(payload)
    if (res.code === 1) {
      ElMessage.success('保存成功')
      showDialog.value = false
      load()
      // 如果从结算页进入，保存后返回结算页
      if (route.query.from === 'checkout') {
        router.push('/checkout')
      }
    } else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function doSetDefault(id) {
  const { data: res } = await setDefaultAddress(id)
  if (res.code === 1) { ElMessage.success('已设为默认'); load() }
}

async function del(id) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  const { data: res } = await deleteAddress(id)
  if (res.code === 1) { ElMessage.success('已删除'); load() }
}

onMounted(load)
</script>

<style scoped>
.addr-page { max-width: 800px; margin: 20px auto; padding: 0 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; font-size: 22px; font-weight: bold; margin-bottom: 20px; }
.addr-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 2px solid #eee; border-radius: 8px; padding: 16px 20px; margin-bottom: 12px; }
.addr-card.is-default { border-color: #67c23a; }
.addr-info { flex: 1; }
.addr-name { font-size: 15px; font-weight: bold; margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.addr-phone { font-weight: normal; color: #666; font-size: 14px; }
.addr-detail { font-size: 14px; color: #666; }
.addr-actions { display: flex; gap: 4px; flex-shrink: 0; }
</style>