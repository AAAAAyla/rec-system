// amazon-frontend/src/api/item.js
import axios from 'axios'

const BASE = 'http://localhost:8080'

// ── 公开接口（无需登录） ──────────────────────────────

/** 搜索商品 */
export const searchItems = (params) =>
    axios.get(`${BASE}/items/search`, { params })
// 用法：searchItems({ kw: '手机', categoryId: 10, sort: 'price_asc', pageNum: 1, pageSize: 20 })

/** 商品详情（含 SKU） */
export const getItemDetail = (id) =>
    axios.get(`${BASE}/items/${id}`)

/** 获取推荐列表 */
export const getRecommend = () =>
    axios.get(`${BASE}/items/recommend`)

// ── 商家接口（需要登录且是认证商家） ────────────────

/** 商家自己的商品列表 */
export const getMerchantItems = (params) =>
    axios.get(`${BASE}/items/merchant/list`, { params })
// 用法：getMerchantItems({ pageNum: 1, pageSize: 10 })

/** 发布新商品 */
export const publishItem = (data) =>
    axios.post(`${BASE}/items`, data)
// 用法：publishItem({ item: { title, categoryId, price, ... }, skus: [...] })

/** 编辑商品 */
export const updateItem = (id, data) =>
    axios.put(`${BASE}/items/${id}`, data)

/** 上架 / 下架：status 1=上架 0=下架 */
export const changeItemStatus = (id, status) =>
    axios.put(`${BASE}/items/${id}/status`, { status })

/** 修改 SKU 库存/价格 */
export const updateSku = (skuId, data) =>
    axios.put(`${BASE}/items/skus/${skuId}`, data)
// 用法：updateSku(skuId, { stock: 100, price: 99.00 })