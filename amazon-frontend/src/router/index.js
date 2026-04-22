import { createRouter, createWebHistory } from 'vue-router'

import Home           from '../views/buyer/Home.vue'
import Login          from '../views/Login.vue'
import Search         from '../views/buyer/Search.vue'
import ProductDetail  from '../views/buyer/ProductDetail.vue'
import Cart           from '../views/buyer/Cart.vue'
import Checkout       from '../views/buyer/Checkout.vue'
import Orders         from '../views/buyer/Orders.vue'
import OrderDetail    from '../views/buyer/OrderDetail.vue'
import Addresses      from '../views/buyer/Addresses.vue'
import MerchantApply  from '../views/buyer/MerchantApply.vue'

import MerchantLayout    from '../views/merchant/MerchantLayout.vue'
import MerchantDashboard from '../views/merchant/Dashboard.vue'
import MerchantItems     from '../views/merchant/Items.vue'
import ItemEdit          from '../views/merchant/ItemEdit.vue'
import MerchantOrders    from '../views/merchant/Orders.vue'
import MerchantShop      from '../views/merchant/Shop.vue'

import MerchantAudit from '../views/admin/MerchantAudit.vue'

const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/',      redirect: '/home' },
  { path: '/home',  component: Home },
  { path: '/search', component: Search },
  { path: '/product/:id', component: ProductDetail },
  { path: '/cart',           component: Cart },
  { path: '/checkout',       component: Checkout },
  { path: '/orders',         component: Orders },
  { path: '/orders/:id',     component: OrderDetail },
  { path: '/addresses',      component: Addresses },
  { path: '/merchant-apply', component: MerchantApply },

  // 管理员路由
  { path: '/admin/merchants', component: MerchantAudit },

  {
    path: '/merchant',
    component: MerchantLayout,
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

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return next('/login')
  next()
})

export default router