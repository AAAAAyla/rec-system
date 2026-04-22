// amazon-frontend/src/api/merchant.js
import axios from 'axios'

const BASE = 'http://localhost:8080'

// ── 商家入驻 ─────────────────────────────────────────

/** 提交入驻申请 */
export const applyMerchant = (data) =>
    axios.post(`${BASE}/merchant/apply`, data)
// 用法：applyMerchant({ shopName: '我的店', contactPhone: '138...' })

/** 查询自己的商家信息（含审核状态） */
export const getMyMerchantInfo = () =>
    axios.get(`${BASE}/merchant/me`)

/** 更新店铺信息 */
export const updateShopInfo = (data) =>
    axios.put(`${BASE}/merchant/shop`, data)

// ── 管理员审核（后台用） ──────────────────────────────

/** 查看商家列表（管理员）status: 0=待审核 1=通过 2=拒绝 */
export const listMerchants = (status = 0, pageNum = 1, pageSize = 10) =>
    axios.get(`${BASE}/merchant/list`, { params: { status, pageNum, pageSize } })

/** 审核商家：status 1=通过 2=拒绝 */
export const auditMerchant = (merchantId, status, rejectReason = '') =>
    axios.put(`${BASE}/merchant/audit/${merchantId}`, { status, rejectReason })