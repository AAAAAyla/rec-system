// amazon-frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

// 买家端页面(只导入已存在的组件)
import Home       from '../views/buyer/Home.vue'
import Login      from '../views/Login.vue'
import Search     from '../views/buyer/Search.vue'
import ProductDetail from '../views/buyer/ProductDetail.vue'
import Cart       from '../views/buyer/Cart.vue'

const routes = [
  // ── 公共 ──────────────────────────────────────────
  { path: '/login',   component: Login,   meta: { public: true } },

  // ── 买家端 ─────────────────────────────────────────
  { path: '/',        redirect: '/home' },
  { path: '/home',    component: Home },
  { path: '/search',  component: Search },
  { path: '/product/:id', component: ProductDetail },
  { path: '/cart',    component: Cart }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 如果访问非公开页面且未登录，跳转到登录页
  if (!to.meta.public && !token) {
    return next('/login')
  }
  
  next()
})

export default router
