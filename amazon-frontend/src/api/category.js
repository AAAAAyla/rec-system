// amazon-frontend/src/api/category.js
import axios from 'axios'

const BASE = 'http://localhost:8080'

/** 获取三级分类树 */
export const getCategoryTree = () =>
    axios.get(`${BASE}/categories/tree`)