import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const getWarehouses = () =>
    axios.get(`${BASE}/warehouses`)

export const addWarehouse = (data) =>
    axios.post(`${BASE}/warehouses`, data)

export const updateWarehouse = (id, data) =>
    axios.put(`${BASE}/warehouses/${id}`, data)

export const deleteWarehouse = (id) =>
    axios.delete(`${BASE}/warehouses/${id}`)

export const setDefaultWarehouse = (id) =>
    axios.put(`${BASE}/warehouses/${id}/default`)
