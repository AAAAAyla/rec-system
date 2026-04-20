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
          <el-button v-if="!addr.isDefault" text type="success" @click="setDefault(addr.id)">
            设为默认
          </el-button>
          <el-button text type="primary" @click="openForm(addr)">编辑</el-button>
          <el-button text type="danger" @click="del(addr.id)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑地址弹窗 -->
    <el-dialog v-model="showDialog" :title="editingAddr ? '编辑地址' : '新增地址'" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="收件人" prop="name">
          <el-input v-model="form.name" placeholder="请输入收件人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" prop="province">
          <div class="region-selects">
            <el-select v-model="form.province" placeholder="省" @change="form.city='';form.district=''">
              <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
            </el-select>
            <el-select v-model="form.city" placeholder="市" @change="form.district=''">
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="form.district" placeholder="区/县">
              <el-option v-for="d in districts" :key="d" :label="d" :value="d" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="街道、门牌号等详细地址" />
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

// 简化版省市区数据（实际项目引入完整 JSON）
const REGION = {
  '广东省': { '广州市': ['天河区','越秀区','海珠区','番禺区'], '深圳市': ['南山区','福田区','宝安区'] },
  '北京市': { '北京市': ['朝阳区','海淀区','西城区','东城区'] },
  '上海市': { '上海市': ['浦东新区','黄浦区','静安区','徐汇区'] },
  '福建省': { '厦门市': ['思明区','湖里区','集美区'], '福州市': ['鼓楼区','台江区','仓山区'] },
}

const addresses  = ref([])
const loading    = ref(false)
const saving     = ref(false)
const showDialog = ref(false)
const editingAddr = ref(null)
const formRef    = ref()

const form = ref({
  name: '', phone: '', province: '', city: '', district: '', detail: '', isDefaultBool: false
})

const provinces = computed(() => Object.keys(REGION))
const cities    = computed(() => form.value.province ? Object.keys(REGION[form.value.province] || {}) : [])
const districts = computed(() => (form.value.province && form.value.city)
  ? (REGION[form.value.province]?.[form.value.city] || []) : [])

const rules = {
  name:     [{ required: true, message: '请输入收件人' }],
  phone:    [{ required: true, message: '请输入手机号' },
             { pattern: /^1\d{10}$/, message: '手机号格式不正确' }],
  province: [{ required: true, message: '请选择省份' }],
  detail:   [{ required: true, message: '请输入详细地址' }],
}

async function load() {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/addresses')
    if (res.data.code === 1) addresses.value = res.data.data
  } finally { loading.value = false }
}

function openForm(addr = null) {
  editingAddr.value = addr
  if (addr) {
    form.value = { ...addr, isDefaultBool: addr.isDefault === 1 }
  } else {
    form.value = { name:'', phone:'', province:'', city:'', district:'', detail:'', isDefaultBool: false }
  }
  showDialog.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  const payload = { ...form.value, isDefault: form.value.isDefaultBool ? 1 : 0 }
  try {
    const res = editingAddr.value
      ? await axios.put(`http://localhost:8080/addresses/${editingAddr.value.id}`, payload)
      : await axios.post('http://localhost:8080/addresses', payload)
    if (res.data.code === 1) {
      ElMessage.success(editingAddr.value ? '修改成功' : '添加成功')
      showDialog.value = false
      load()
    } else { ElMessage.error(res.data.msg) }
  } finally { saving.value = false }
}

async function setDefault(id) {
  const res = await axios.put(`http://localhost:8080/addresses/${id}/default`)
  if (res.data.code === 1) { ElMessage.success('已设为默认地址'); load() }
}

async function del(id) {
  await ElMessageBox.confirm('确定删除该地址吗？', '删除', { type: 'warning' })
  const res = await axios.delete(`http://localhost:8080/addresses/${id}`)
  if (res.data.code === 1) { ElMessage.success('已删除'); load() }
}

onMounted(load)
</script>

<style scoped>
.addr-page { max-width: 800px; margin: 20px auto; padding: 0 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; font-size: 22px; font-weight: bold; margin-bottom: 20px; }
.addr-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 2px solid #eee; border-radius: 8px; padding: 16px 20px; margin-bottom: 12px; transition: 0.2s; }
.addr-card.is-default { border-color: #67c23a; }
.addr-info { flex: 1; }
.addr-name { font-size: 15px; font-weight: bold; margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.addr-phone { font-weight: normal; color: #666; font-size: 14px; }
.addr-detail { font-size: 14px; color: #666; }
.addr-actions { display: flex; gap: 4px; flex-shrink: 0; }
.region-selects { display: flex; gap: 8px; }
.region-selects .el-select { flex: 1; }
</style>
