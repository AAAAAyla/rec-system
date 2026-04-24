<template>
  <div class="profile-page">
    <h2>个人中心</h2>
    <el-card>
      <el-form :model="form" label-width="100px">
        <el-form-item label="头像">
          <div class="avatar-wrap">
            <el-avatar :size="80" :src="form.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
            <el-upload :show-file-list="false" :http-request="handleUpload" accept="image/*" style="margin-left:16px">
              <el-button size="small">更换头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="用户名"><el-input :value="form.username" disabled /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" placeholder="设置昵称" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="绑定手机号" /></el-form-item>
        <el-form-item label="角色">
          <el-tag>{{ roleMap[form.role] || '买家' }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:20px">
      <template #header>修改密码</template>
      <el-form :model="pwdForm" label-width="100px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
        <el-form-item>
          <el-button type="warning" @click="changePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:20px">
      <template #header>快捷入口</template>
      <el-space>
        <el-button @click="$router.push('/addresses')">收货地址管理</el-button>
        <el-button @click="$router.push('/orders')">我的订单</el-button>
        <el-button @click="$router.push('/favorites')">我的收藏</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, changePassword, uploadFile } from '../../api/profile'

const roleMap = { 0: '买家', 1: '商家', 2: '管理员' }
const form = ref({ username: '', nickname: '', phone: '', avatar: '', role: 0 })
const pwdForm = ref({ oldPassword: '', newPassword: '' })

onMounted(async () => {
  const { data: res } = await getProfile()
  if (res.code === 1) Object.assign(form.value, res.data)
})

const handleUpload = async ({ file }) => {
  const { data: res } = await uploadFile(file)
  if (res.code === 1) {
    const base = import.meta.env.VITE_API_BASE || 'http://localhost:8080'
    form.value.avatar = base + res.data
    ElMessage.success('头像上传成功')
  }
}

const saveProfile = async () => {
  const { data: res } = await updateProfile(form.value)
  res.code === 1 ? ElMessage.success('保存成功') : ElMessage.error(res.msg)
}

const changePwd = async () => {
  if (!pwdForm.value.newPassword || pwdForm.value.newPassword.length < 6) {
    return ElMessage.warning('新密码至少 6 位')
  }
  const { data: res } = await changePassword(pwdForm.value)
  if (res.code === 1) {
    ElMessage.success(res.data || '修改成功')
    pwdForm.value = { oldPassword: '', newPassword: '' }
  } else {
    ElMessage.error(res.msg)
  }
}
</script>

<style scoped>
.profile-page { max-width: 700px; margin: 0 auto; padding: 20px; }
.avatar-wrap { display: flex; align-items: center; }
</style>
