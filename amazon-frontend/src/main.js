import { createApp } from 'vue'
import { createPinia } from 'pinia' // 导入 Pinia
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'

// 1. 导入页面组件
import Home from './Home.vue'
import Login from './Login.vue'
import ProductDetail from './ProductDetail.vue'

// 2. 配置路由
const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/home' },
        { path: '/login', component: Login },
        { path: '/home', component: Home },
        { path: '/product/:id', name: 'ProductDetail', component: ProductDetail }
    ]
})

// 路由守卫：登录校验
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.path !== '/login' && !token) {
        next('/login')
    } else {
        next()
    }
})

// 3. 配置 Axios 拦截器
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = token
    }
    return config
})

axios.interceptors.response.use(res => res, error => {
    if (error.response && error.response.status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
    }
    return Promise.reject(error)
})

// 4. 创建并挂载应用
const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(createPinia()) // 激活 Pinia
app.use(ElementPlus)
app.use(router)

app.mount('#app')