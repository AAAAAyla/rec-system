import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'

// 导入刚才新建的两个页面
import Home from './Home.vue'
import Login from './Login.vue'

// ================= 1. 配置路由 =================
const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/login', component: Login },
        { path: '/home', component: Home }
    ]
})

// 路由守卫：如果没手环（未登录），强制去登录页
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.path !== '/login' && !token) {
        next('/login')
    } else {
        next()
    }
})

// ================= 2. 配置 Axios 全局拦截器 =================
// 每次发请求前，自动把手环塞进 Header
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = token
    }
    return config
})

// 如果后端报错说手环过期了 (401)，自动退回登录页
axios.interceptors.response.use(res => res, error => {
    if (error.response && error.response.status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
    }
    return Promise.reject(error)
})

// ================= 3. 挂载 Vue =================
const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(ElementPlus)
app.use(router) // 启用路由
app.mount('#app')