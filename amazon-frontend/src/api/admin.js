import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const getUsers = (params) =>
    axios.get(`${BASE}/admin/users`, { params })

export const setUserStatus = (id, status) =>
    axios.put(`${BASE}/admin/users/${id}/status`, { status })

export const getAdminItems = (params) =>
    axios.get(`${BASE}/admin/items`, { params })

export const auditItem = (id, status) =>
    axios.put(`${BASE}/admin/items/${id}/audit`, { status })

export const getAdminOrders = (params) =>
    axios.get(`${BASE}/admin/orders`, { params })

export const getAdminStats = () =>
    axios.get(`${BASE}/admin/stats`)
