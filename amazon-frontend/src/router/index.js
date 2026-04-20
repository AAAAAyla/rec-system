// amazon-frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

// 买家端页面
import Home       from '../views/buyer/Home.vue'
import Login      from '../views/Login.vue'
import Search     from '../views/buyer/Search.vue'
import ProductDetail from '../views/buyer/ProductDetail.vue'
import Cart       from '../views/buyer/Cart.vue'
import Checkout   from '../views/buyer/Checkout.vue'
import Orders     from '../views/buyer/Orders.vue'
import OrderDetail from '../views/buyer/OrderDetail.vue'
import Addresses  from '../views/buyer/Addresses.vue'
import MerchantApply from '../views/buyer/MerchantApply.vue'

// 商家端页面
import MerchantLayout  from '../views/merchant/MerchantLayout.vue'
import MerchantDashboard from '../views/merchant/Dashboard.vue'
import MerchantItems   from '../views/merchant/Items.vue'
import ItemEdit        from '../views/merchant/ItemEdit.vue'
import MerchantOrders  from '../views/merchant/Orders.vue'
import MerchantShop    from '../views/merchant/Shop.vue'

const routes = [
  // ── 公共 ──────────────────────────────────────────
  { path: '/login',   component: Login,   meta: { public: true } },

  // ── 买家端 ─────────────────────────────────────────
  { path: '/',        redirect: '/home' },
  { path: '/home',    component: Home },
  { path: '/search',  component: Search },
  { path: '/product/:id', component: ProductDetail },
  { path: '/cart',    component: Cart },
  { path: '/checkout', component: Checkout },
  { path: '/orders',  component: Orders },
  { path: '/orders/:id', component: OrderDetail },
  { path: '/addresses', component: Addresses },
  { path: '/merchant/apply', component: MerchantApply },

  // ── 商家端（嵌套路由） ────────────────────────────
  {
    path: '/merchant',
    component: MerchantLayout,
    meta: { requiresMerchant: true },
    children: [
      { path: '',          redirect: 'dashboard' },
      { path: 'dashboard', component: MerchantDashboard },
      { path: 'items',     component: MerchantItems },
      { path: 'items/new', component: ItemEdit },
      { path: 'items/:id/edit', component: ItemEdit },
      { path: 'orders',    component: MerchantOrders },
      { path: 'shop',      component: MerchantShop },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')
  const user = userStr ? JSON.parse(userStr) : null

  if (!to.meta.public && !token) {
    return next('/login')
  }
  if (to.meta.requiresMerchant && user?.role !== 1) {
    return next('/merchant/apply')
  }
  next()
})

export default router
