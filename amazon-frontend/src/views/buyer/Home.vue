<template>
  <div class="home-page">

    <!-- 分类导航条 -->
    <div class="category-bar">
      <div class="cat-scroll">
        <button
          :class="['cat-chip', { active: activeCat === null }]"
          @click="selectCategory(null)"
        >
          <span class="cat-icon">🏠</span>
          全部
        </button>
        <button
          v-for="cat in categories" :key="cat.id"
          :class="['cat-chip', { active: activeCat === cat.id }]"
          @click="selectCategory(cat.id)"
        >
          <span class="cat-icon">{{ cat.iconUrl || cat.icon_url || '📦' }}</span>
          {{ cat.name }}
        </button>
      </div>
      <!-- 二级子分类 -->
      <div v-if="activeChildren.length > 0" class="sub-cat-scroll">
        <button
          v-for="sub in activeChildren" :key="sub.id"
          :class="['sub-chip', { active: activeSubCat === sub.id }]"
          @click="selectSubCategory(sub.id)"
        >
          {{ sub.name }}
        </button>
      </div>
    </div>

    <!-- 推荐区（登录用户） -->
    <section v-if="userStore.isLoggedIn && recommendList.length > 0" class="section">
      <div class="section-header">
        <span class="section-title">猜你喜欢</span>
      </div>
      <div class="scroll-row">
        <div class="scroll-track">
          <div v-for="item in recommendList" :key="'rec-' + item.id" class="scroll-card" @click="goToDetail(item)">
            <img :src="item.image_url" class="scroll-img" @error="handleImageError" />
            <div class="scroll-info">
              <div class="scroll-title">{{ item.title }}</div>
              <div class="scroll-price">￥{{ item.price }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 当前分类商品（横向滑动） -->
    <section v-if="catProducts.length > 0" class="section">
      <div class="section-header">
        <span class="section-title">{{ activeCatName }}</span>
        <el-button text type="primary" @click="viewAll">查看全部 →</el-button>
      </div>
      <div class="scroll-row">
        <div class="scroll-track">
          <div v-for="item in catProducts" :key="'cat-' + item.id" class="scroll-card" @click="goToDetail(item)">
            <img :src="item.imageUrl || item.image_url" class="scroll-img" @error="handleImageError" />
            <div class="scroll-info">
              <div class="scroll-title">{{ item.title }}</div>
              <div class="scroll-price">￥{{ item.price }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 全部商品 -->
    <section class="section">
      <div class="section-header">
        <span class="section-title">全部商品</span>
      </div>

      <el-row :gutter="16" v-if="productList.length > 0">
        <el-col v-for="item in productList" :key="item.id" :xs="12" :sm="8" :md="6" :lg="4">
          <div class="product-card" @click="goToDetail(item)">
            <div class="img-wrap">
              <img :src="item.imageUrl || item.image_url" @error="handleImageError" />
            </div>
            <div class="card-body">
              <div class="card-title">{{ item.title }}</div>
              <div class="card-price">￥{{ item.price }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-empty v-else-if="!loading" description="暂无商品" />
      <div v-if="loading" class="loading-tip">加载中...</div>

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        class="pagination"
        @current-change="loadProducts"
      />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/userStore'

const router    = useRouter()
const userStore = useUserStore()
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const categories   = ref([])
const activeCat    = ref(null)
const activeSubCat = ref(null)
const catProducts  = ref([])
const productList  = ref([])
const recommendList = ref([])
const loading  = ref(false)
const pageNum  = ref(1)
const pageSize = ref(20)
const total    = ref(0)

const activeChildren = computed(() => {
  if (!activeCat.value) return []
  const cat = categories.value.find(c => c.id === activeCat.value)
  return cat?.children || []
})

const activeCatName = computed(() => {
  if (activeSubCat.value) {
    const parent = categories.value.find(c => c.id === activeCat.value)
    const sub = parent?.children?.find(s => s.id === activeSubCat.value)
    return sub ? sub.name : '精选'
  }
  const cat = categories.value.find(c => c.id === activeCat.value)
  return cat ? cat.name : '精选'
})

const effectiveCatId = computed(() => activeSubCat.value || activeCat.value)

const loadCategories = async () => {
  try {
    const { data: res } = await axios.get(`${BASE}/categories/tree`)
    if (res.code === 1 && res.data?.length > 0) {
      categories.value = res.data
      selectCategory(res.data[0].id)
    }
  } catch {}
}

const selectCategory = async (catId) => {
  activeCat.value = catId
  activeSubCat.value = null
  await loadCatProducts(catId)
}

const selectSubCategory = async (subId) => {
  activeSubCat.value = subId
  await loadCatProducts(subId)
}

const loadCatProducts = async (catId) => {
  if (!catId) { catProducts.value = []; return }
  try {
    const { data: res } = await axios.get(`${BASE}/items/search`, {
      params: { categoryId: catId, pageNum: 1, pageSize: 10 }
    })
    if (res.code === 1) {
      catProducts.value = res.data.rows || []
    }
  } catch {}
}

const viewAll = () => {
  router.push({ path: '/search', query: { categoryId: effectiveCatId.value } })
}

const loadProducts = async () => {
  loading.value = true
  try {
    const { data: res } = await axios.get(`${BASE}/items/search`, {
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
    })
    if (res.code === 1) {
      productList.value = res.data.rows || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    ElMessage.error('加载商品失败')
  } finally {
    loading.value = false
  }
}

const loadRecommendations = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const { data: res } = await axios.get(`${BASE}/items/recommend`)
    if (res.code === 1 && Array.isArray(res.data?.rows)) {
      recommendList.value = res.data.rows
    }
  } catch {}
}

const goToDetail = (item) => router.push(`/product/${item.id}`)
const handleImageError = (e) => { if (e?.target) e.target.src = 'https://via.placeholder.com/150?text=No+Image' }

onMounted(() => {
  loadCategories()
  loadProducts()
  loadRecommendations()
})
</script>

<style scoped>
.home-page { max-width: 1200px; margin: 0 auto; padding: 0 16px; }

/* ── 分类导航 ── */
.category-bar {
  padding: 12px 0;
  border-bottom: 1px solid #eee;
  background: white;
  position: sticky;
  top: 60px;
  z-index: 10;
}
.cat-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 0 4px 4px;
  scrollbar-width: none;
}
.cat-scroll::-webkit-scrollbar { display: none; }
.cat-chip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #e0e0e0;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #555;
  transition: all 0.2s;
  white-space: nowrap;
}
.cat-chip:hover { border-color: #409eff; color: #409eff; }
.cat-chip.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: transparent;
}
.cat-icon { font-size: 16px; }

/* ── 二级分类 ── */
.sub-cat-scroll {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding: 6px 4px 2px;
  scrollbar-width: none;
}
.sub-cat-scroll::-webkit-scrollbar { display: none; }
.sub-chip {
  flex-shrink: 0;
  padding: 4px 14px;
  border-radius: 14px;
  border: 1px solid #ddd;
  background: #fafafa;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  transition: all 0.2s;
  white-space: nowrap;
}
.sub-chip:hover { border-color: #409eff; color: #409eff; }
.sub-chip.active { background: #409eff; color: white; border-color: transparent; }

/* ── Section ── */
.section { margin: 20px 0; }
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-title { font-size: 20px; font-weight: bold; color: #111; }

/* ── 横向滑动 ── */
.scroll-row { overflow: hidden; }
.scroll-track {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
}
.scroll-track::-webkit-scrollbar { display: none; }
.scroll-card {
  flex-shrink: 0;
  width: 160px;
  background: white;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  scroll-snap-align: start;
}
.scroll-card:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }
.scroll-img { width: 160px; height: 160px; object-fit: cover; display: block; background: #f5f5f5; }
.scroll-info { padding: 8px 10px; }
.scroll-title {
  font-size: 12.5px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.scroll-price { font-size: 15px; color: #e44; font-weight: 600; margin-top: 4px; }

/* ── 商品卡片网格 ── */
.product-card {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 16px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: transform 0.2s, box-shadow 0.2s;
}
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.img-wrap {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  padding: 10px;
}
.img-wrap img { max-width: 100%; max-height: 100%; object-fit: contain; }
.card-body { padding: 10px 12px; }
.card-title {
  font-size: 13.5px;
  color: #007185;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}
.card-title:hover { color: #c45500; text-decoration: underline; }
.card-price { font-size: 18px; color: #B12704; font-weight: bold; margin-top: 6px; }
.pagination { margin: 20px 0; display: flex; justify-content: center; }
.loading-tip { text-align: center; color: #999; padding: 20px; }
</style>
