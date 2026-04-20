import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
    // 1. 状态：购物车里的商品
    const items = ref([])

    // 2. 计算属性：商品总数（用于导航栏红点）
    const totalCount = computed(() => {
        return items.value.reduce((sum, item) => sum + item.quantity, 0)
    })

    // 3. 计算属性：总金额
    const totalPrice = computed(() => {
        return items.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
    })

    // 4. 动作：添加商品
    const addToCart = (product) => {
        const existing = items.value.find(i => i.id === product.id)
        if (existing) {
            existing.quantity++
        } else {
            items.value.push({ ...product, quantity: 1 })
        }
    }

    return { items, totalCount, totalPrice, addToCart }
})