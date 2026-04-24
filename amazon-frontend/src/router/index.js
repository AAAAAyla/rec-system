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
import Profile        from '../views/buyer/Profile.vue'
import Favorites      from '../views/buyer/Favorites.vue'
import BuyerIm        from '../views/buyer/Im.vue'
import Notifications  from '../views/buyer/Notifications.vue'

import MerchantLayout    from '../views/merchant/MerchantLayout.vue'
import MerchantDashboard from '../views/merchant/Dashboard.vue'
import MerchantItems     from '../views/merchant/Items.vue'
import ItemEdit          from '../views/merchant/ItemEdit.vue'
import MerchantOrders    from '../views/merchant/Orders.vue'
import MerchantShop      from '../views/merchant/Shop.vue'
import MerchantWarehouses from '../views/merchant/Warehouses.vue'
import MerchantCoupons   from '../views/merchant/Coupons.vue'
import MerchantIm        from '../views/merchant/Im.vue'

import AdminLayout   from '../views/admin/AdminLayout.vue'
import MerchantAudit from '../views/admin/MerchantAudit.vue'
import AdminUsers    from '../views/admin/Users.vue'
import AdminItems    from '../views/admin/Items.vue'
import AdminStats      from '../views/admin/Stats.vue'
import AdminOrders     from '../views/admin/Orders.vue'
import AdminCategories from '../views/admin/Categories.vue'

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
  { path: '/profile',        component: Profile },
  { path: '/favorites',      component: Favorites },
  { path: '/im',             component: BuyerIm },
  { path: '/notifications',  component: Notifications },

  {
    path: '/admin',
    component: AdminLayout,
    meta: { role: 2 },
    children: [
      { path: '',          redirect: '/admin/stats' },
      { path: 'stats',     component: AdminStats },
      { path: 'users',     component: AdminUsers },
      { path: 'merchants',  component: MerchantAudit },
      { path: 'categories', component: AdminCategories },
      { path: 'items',     component: AdminItems },
      { path: 'orders',    component: AdminOrders },
    ]
  },

  {
    path: '/merchant',
    component: MerchantLayout,
    meta: { roles: [1, 2] },
    children: [
      { path: '',           redirect: '/merchant/dashboard' },
      { path: 'dashboard',  component: MerchantDashboard },
      { path: 'items',      component: MerchantItems },
      { path: 'items/new',  component: ItemEdit },
      { path: 'items/:id/edit', component: ItemEdit },
      { path: 'orders',     component: MerchantOrders },
      { path: 'shop',       component: MerchantShop },
      { path: 'warehouses', component: MerchantWarehouses },
      { path: 'coupons',    component: MerchantCoupons },
      { path: 'im',         component: MerchantIm },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return next('/login')

  const requiredRole = to.meta.role
  const requiredRoles = to.meta.roles
  if (requiredRole != null || requiredRoles) {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      if (requiredRoles) {
        if (!requiredRoles.includes(user.role)) return next('/home')
      } else if (user.role !== requiredRole) {
        return next('/home')
      }
    } catch { return next('/home') }
  }

  next()
})

export default router