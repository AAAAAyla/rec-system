import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const createCoupon = (data) =>
    axios.post(`${BASE}/coupons`, data)

export const getMerchantCoupons = () =>
    axios.get(`${BASE}/coupons/merchant`)

export const getAvailableCoupons = (merchantId) =>
    axios.get(`${BASE}/coupons/available`, { params: { merchantId } })

export const claimCoupon = (couponId) =>
    axios.post(`${BASE}/coupons/${couponId}/claim`)

export const getMyCoupons = (status = 'unused') =>
    axios.get(`${BASE}/coupons/mine`, { params: { status } })
