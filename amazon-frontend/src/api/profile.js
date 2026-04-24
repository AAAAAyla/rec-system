import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const getProfile = () =>
    axios.get(`${BASE}/profile`)

export const updateProfile = (data) =>
    axios.put(`${BASE}/profile`, data)

export const changePassword = (data) =>
    axios.put(`${BASE}/profile/password`, data)

export const uploadFile = (file) => {
    const form = new FormData()
    form.append('file', file)
    return axios.post(`${BASE}/files/upload`, form, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}
