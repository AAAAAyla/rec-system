// amazon-frontend/src/store/cartStore.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
    const items = ref(JSON.parse(localStorage.getItem('cart') || '[]'))

    const totalCount = computed(() =>
        items.value.reduce((s, i) => s + i.quantity, 0)
    )

    const totalPrice = computed(() =>
        items.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2)
    )

    function save() {
        localStorage.setItem('cart', JSON.stringify(items.value))
    }

    function addToCart(product) {
        // product: { itemId, skuId, title, price, imageUrl, specJson, quantity }
        const key = `${product.itemId}_${product.skuId}`
        const existing = items.value.find(i => `${i.itemId}_${i.skuId}` === key)
        if (existing) {
            existing.quantity += product.quantity || 1
        } else {
            items.value.push({ ...product, quantity: product.quantity || 1 })
        }
        save()
    }

    function updateQuantity(index, qty) {
        if (qty < 1) return
        items.value[index].quantity = qty
        save()
    }

    function removeItem(index) {
        items.value.splice(index, 1)
        save()
    }

    function clearChecked(checkedIndexes) {
        // 下单成功后清除已勾选的商品
        items.value = items.value.filter((_, i) => !checkedIndexes.includes(i))
        save()
    }

    return { items, totalCount, totalPrice, addToCart, updateQuantity, removeItem, clearChecked }
})