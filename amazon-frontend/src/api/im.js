import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const getSessions = (role = 'user') =>
    axios.get(`${BASE}/im/sessions`, { params: { role } })

export const createSession = (merchantId) =>
    axios.post(`${BASE}/im/sessions`, { merchantId })

export const getMessages = (sessionId, pageNum = 1, pageSize = 50) =>
    axios.get(`${BASE}/im/sessions/${sessionId}/messages`, { params: { pageNum, pageSize } })

export const sendMessage = (sessionId, senderType, content, type = 'text') =>
    axios.post(`${BASE}/im/messages`, { sessionId, senderType, content, type })

export const markRead = (sessionId, role = 'user') =>
    axios.put(`${BASE}/im/sessions/${sessionId}/read`, null, { params: { role } })
