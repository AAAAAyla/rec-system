import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const getNotifications = (params) =>
    axios.get(`${BASE}/notifications`, { params })

export const getUnreadCount = () =>
    axios.get(`${BASE}/notifications/unread-count`)

export const markRead = (id) =>
    axios.put(`${BASE}/notifications/${id}/read`)

export const markAllRead = () =>
    axios.put(`${BASE}/notifications/read-all`)
