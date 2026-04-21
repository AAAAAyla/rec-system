<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="brand">
        <el-icon class="brand-icon"><Goods /></el-icon>
        <span class="brand-name">AmazonRec</span>
      </div>
      <p class="brand-slogan">开启您的个性化购物探索之旅</p>

      <el-tabs v-model="activeTab" stretch @tab-change="clearForms">

        <el-tab-pane label="用户登录" name="login">
          <el-form
              :model="loginForm"
              :rules="loginRules"
              ref="loginRef"
              label-position="top"
          >
            <el-form-item prop="username">
              <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  :prefix-icon="User"
                  size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  show-password
                  size="large"
              />
            </el-form-item>
            <div class="form-footer">
              <el-button
                  type="warning"
                  class="action-btn"
                  :loading="loading"
                  @click="handleLogin(loginRef)"
              >
                登 录
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="免费注册" name="register">
          <el-form
              :model="regForm"
              :rules="regRules"
              ref="regRef"
              label-position="top"
          >
            <el-form-item prop="username">
              <el-input
                  v-model="regForm.username"
                  placeholder="设置用户名 (3-10位)"
                  :prefix-icon="User"
                  size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                  v-model="regForm.password"
                  type="password"
                  placeholder="设置密码 (至少6位)"
                  :prefix-icon="Lock"
                  show-password
                  size="large"
              />
            </el-form-item>
            <el-form-item prop="rePassword">
              <el-input
                  v-model="regForm.rePassword"
                  type="password"
                  placeholder="请再次输入密码"
                  :prefix-icon="CircleCheck"
                  show-password
                  size="large"
              />
            </el-form-item>
            <div class="form-footer">
              <el-button
                  type="primary"
                  class="action-btn register-btn"
                  :loading="loading"
                  @click="handleRegister(regRef)"
              >
                注 册 账 户
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="auth-footer">
        登录即表示您同意我们的服务条款
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Goods, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '../store/userStore'
const userStore = useUserStore()
// --- 变量定义 ---
const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)

const loginRef = ref()
const regRef = ref()

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', rePassword: '' })

// --- 校验规则 ---

// 校验两次密码是否一致
const validatePass2 = (rule, value, callback) => {
  if (value !== regForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于 6 位', trigger: 'blur' }
  ],
  rePassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ]
}

// --- 逻辑处理 ---

const clearForms = () => {
  if (loginRef.value) loginRef.value.resetFields()
  if (regRef.value) regRef.value.resetFields()
}

// 登录逻辑
const handleLogin = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await axios.post('http://localhost:8080/login', loginForm)
        if (res.data.code === 1) {
          const { token, userInfo } = res.data.data
          userStore.setLogin(token, userInfo)
          ElMessage.success('登录成功')
          router.push('/home')
        } else {
          ElMessage.error(res.data.msg)
        }
      } catch (err) {
        ElMessage.error('服务器连接失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 注册逻辑
const handleRegister = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await axios.post('http://localhost:8080/register', {
          username: regForm.username,
          password: regForm.password
        })
        if (res.data.code === 1) {
          ElMessage.success('注册成功，请直接登录')
          activeTab.value = 'login'
          loginForm.username = regForm.username
        } else {
          ElMessage.error(res.data.msg)
        }
      } catch (err) {
        ElMessage.error('注册失败，请检查网络')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.auth-container {
  height: 100vh;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  display: flex;
  justify-content: center;
  align-items: center;
}

.auth-card {
  width: 420px;
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.05);
}

.brand {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.brand-icon {
  font-size: 32px;
  color: #ff9900;
}

.brand-name {
  font-size: 28px;
  font-weight: 900;
  color: #131921;
}

.brand-slogan {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 30px;
}

.form-footer {
  margin-top: 20px;
}

.action-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  font-weight: bold;
}

/* 亚马逊风格按钮 */
.el-button--warning {
  background-color: #f0c14b;
  border-color: #a88734;
  color: #111;
}

.register-btn {
  background-color: #232f3e;
  border-color: #131921;
  color: white;
}

.auth-footer {
  text-align: center;
  margin-top: 25px;
  font-size: 12px;
  color: #888;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: bold;
  height: 50px;
}

:deep(.el-tabs__active-bar) {
  background-color: #f0c14b;
}

:deep(.el-tabs__item.is-active) {
  color: #131921;
}
</style>