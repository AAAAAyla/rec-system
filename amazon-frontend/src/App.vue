<template>
    <div class="common-layout">
        <el-header class="amazon-header">
            <div class="logo">AmazonRec</div>
            <el-input v-model="searchQuery" placeholder="搜索商品..." class="search-input">
                <template #append>
                    <el-button :icon="Search" />
                </template>
            </el-input>
        </el-header>

        <el-main>
            <el-row :gutter="20">
                <el-col v-for="item in productList" :key="item.id" :xs="12" :sm="8" :md="6" :lg="4">
                    <el-card class="product-card" :body-style="{ padding: '10px' }">
                        <div class="image-container">
                            <img :src="item.image_url" class="product-image" @error="handleImageError" />
                        </div>
                        <div class="product-info">
                            <div class="product-title">{{ item.title }}</div>
                            <el-rate v-model="item.mockRate" disabled show-score text-color="#ff9900" />
                            <div class="price">
                                <span class="currency">$</span>
                                <span class="amount">{{ (Math.random() * 100).toFixed(2) }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <div class="pagination">
                <el-pagination
                        layout="prev, pager, next"
                        :total="total"
                        v-model:current-page="currentPage"
                        @current-change="loadProducts"
                />
            </div>
        </el-main>
    </div>
</template>

<script setup>
    import { ref, onMounted } from 'vue'
    import { Search } from '@element-plus/icons-vue'
    import axios from 'axios'

    const productList = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const searchQuery = ref('')

    // 从后端加载商品数据
    const loadProducts = async () => {
        try {
            const res = await axios.get(`http://localhost:8080/items/page?pageNum=${currentPage.value}&pageSize=12`)
            if (res.data.code === 1) {
                productList.value = res.data.data.rows.map(item => ({
                    ...item,
                    mockRate: 4 + Math.random() // 随机生成一个评分展示
                }))
                total.value = res.data.data.total
            }
        } catch (err) {
            console.error("加载失败，请检查后端是否开启及跨域配置", err)
        }
    }

    const handleImageError = (e) => {
        e.target.src = 'https://via.placeholder.com/150?text=No+Image' // 图片加载失败时的占位图
    }

    onMounted(() => {
        loadProducts()
    })
</script>

<style scoped>
    .amazon-header { background-color: #131921; display: flex; align-items: center; padding: 10px 20px; }
    .logo { color: white; font-weight: bold; font-size: 24px; margin-right: 20px; }
    .search-input { width: 500px; }
    .product-card { margin-bottom: 20px; height: 350px; cursor: pointer; transition: 0.3s; }
    .product-card:hover { transform: translateY(-5px); }
    .image-container { height: 200px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
    .product-image { max-width: 100%; max-height: 100%; object-fit: contain; }
    .product-title { font-size: 14px; height: 40px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin: 10px 0; }
    .price { color: #B12704; font-size: 18px; margin-top: 5px; }
    .currency { font-size: 12px; vertical-align: top; }
    .pagination { margin-top: 30px; display: flex; justify-content: center; }
</style>