// amazon-frontend/src/store/userStore.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const user  = ref(JSON.parse(localStorage.getItem('user') || 'null'))

    function setLogin(t, u) {
        token.value = t
        user.value  = u
        localStorage.setItem('token', t)
        localStorage.setItem('user', JSON.stringify(u))
    }

    function updateUser(partial) {
        if (user.value) {
            user.value = { ...user.value, ...partial }
            localStorage.setItem('user', JSON.stringify(user.value))
        }
    }

    function logout() {
        token.value = ''
        user.value  = null
        localStorage.removeItem('token')
        localStorage.removeItem('user')
    }

    const isMerchant = computed(() => user.value?.role === 1)
    const isAdmin    = computed(() => user.value?.role === 2)
    const isLoggedIn = computed(() => !!token.value)

    return { token, user, setLogin, updateUser, logout, isMerchant, isAdmin, isLoggedIn }
})