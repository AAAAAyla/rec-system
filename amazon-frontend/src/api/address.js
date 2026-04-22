import axios from 'axios'

const BASE = 'http://localhost:8080'

function authHeader() {
    return { headers: { Authorization: localStorage.getItem('token') } }
}

export const getAddresses = () =>
    axios.get(`${BASE}/addresses`, authHeader())

export const addAddress = (data) =>
    axios.post(`${BASE}/addresses`, data, authHeader())

export const updateAddress = (id, data) =>
    axios.put(`${BASE}/addresses/${id}`, data, authHeader())

export const deleteAddress = (id) =>
    axios.delete(`${BASE}/addresses/${id}`, authHeader())

export const setDefaultAddress = (id) =>
    axios.put(`${BASE}/addresses/${id}/default`, {}, authHeader())