import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import axios from 'axios'
import router from './router'
import { useUserStore } from './store/userStore'

const app = createApp(App)
const pinia = createPinia()

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 必须先激活 Pinia，interceptor 内才能调用 useUserStore()
app.use(pinia)
app.use(ElementPlus)
app.use(router)

// 请求拦截器：自动附加 token
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = token
    }
    return config
})

// 响应拦截器：401 时同时清 Pinia store + localStorage，再跳登录
axios.interceptors.response.use(res => res, error => {
    if (error.response && error.response.status === 401) {
        const userStore = useUserStore()
        userStore.logout()          // 同时清 Pinia 内存状态和 localStorage
        router.push('/login')
    }
    return Promise.reject(error)
})

app.mount('#app')