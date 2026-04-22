import axios from 'axios'

const BASE = 'http://localhost:8080'

function authHeader() {
    return { headers: { Authorization: localStorage.getItem('token') } }
}

// ── 买家 ───────────────────────────────────────────
export const createOrder = (data) =>
    axios.post(`${BASE}/orders`, data, authHeader())

export const mockPay = (orderNo) =>
    axios.post(`${BASE}/orders/pay`, { orderNo }, authHeader())

export const getMyOrders = (params) =>
    axios.get(`${BASE}/orders`, { params, ...authHeader() })

export const getOrderDetail = (id) =>
    axios.get(`${BASE}/orders/${id}`, authHeader())

export const cancelOrder = (id, reason) =>
    axios.put(`${BASE}/orders/${id}/cancel`, { reason }, authHeader())

export const confirmReceive = (id) =>
    axios.put(`${BASE}/orders/${id}/confirm`, {}, authHeader())

export const applyRefund = (id) =>
    axios.post(`${BASE}/orders/${id}/refund`, {}, authHeader())

// ── 商家 ───────────────────────────────────────────
export const getMerchantOrders = (params) =>
    axios.get(`${BASE}/orders/merchant`, { params, ...authHeader() })

export const shipOrder = (id, data) =>
    axios.put(`${BASE}/orders/${id}/ship`, data, authHeader())

export const agreeRefund = (id) =>
    axios.put(`${BASE}/orders/${id}/refund/agree`, {}, authHeader())