<template>
  <div>
    <div class="page-title">优惠券管理</div>
    <el-button type="primary" @click="showCreate = true" style="margin-bottom:16px">创建优惠券</el-button>

    <el-table :data="list" border>
      <el-table-column prop="title" label="名称" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ row.type === 'discount' ? '折扣券' : '满减券' }}</template>
      </el-table-column>
      <el-table-column label="优惠" width="120">
        <template #default="{ row }">
          {{ row.type === 'discount' ? (row.discount * 10) + '折' : '减' + row.discount + '元' }}
        </template>
      </el-table-column>
      <el-table-column prop="min_amount" label="门槛金额" width="100" />
      <el-table-column label="库存" width="120">
        <template #default="{ row }">{{ row.remain_count }} / {{ row.total_count }}</template>
      </el-table-column>
      <el-table-column prop="expire_time" label="过期时间" width="180" />
    </el-table>

    <el-dialog v-model="showCreate" title="创建优惠券" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="amount">满减券</el-radio>
            <el-radio value="discount">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.type === 'discount' ? '折扣(0-1)' : '减免金额'">
          <el-input-number v-model="form.discount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="门槛金额"><el-input-number v-model="form.minAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="发放数量"><el-input-number v-model="form.totalCount" :min="1" /></el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="form.expireTime" type="datetime" placeholder="选择过期时间" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createCoupon, getMerchantCoupons } from '../../api/coupon'

const list = ref([])
const showCreate = ref(false)
const form = ref({ title: '', type: 'amount', discount: 10, minAmount: 50, totalCount: 100, expireTime: '' })

const load = async () => {
  const { data: res } = await getMerchantCoupons()
  if (res.code === 1) list.value = res.data
}

const handleCreate = async () => {
  const { data: res } = await createCoupon(form.value)
  if (res.code === 1) { ElMessage.success('创建成功'); showCreate.value = false; load() }
  else ElMessage.error(res.msg)
}

onMounted(load)
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: bold; margin-bottom: 20px; }
</style>
