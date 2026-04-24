import axios from 'axios'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const getFavorites = (params) =>
    axios.get(`${BASE}/favorites`, { params })

export const addFavorite = (itemId) =>
    axios.post(`${BASE}/favorites/${itemId}`)

export const removeFavorite = (itemId) =>
    axios.delete(`${BASE}/favorites/${itemId}`)

export const checkFavorite = (itemId) =>
    axios.get(`${BASE}/favorites/check/${itemId}`)
