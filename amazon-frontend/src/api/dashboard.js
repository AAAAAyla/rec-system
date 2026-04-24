import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const getDashboardStats = () =>
    axios.get(`${BASE}/merchant/dashboard/stats`)
