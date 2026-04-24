<!-- amazon-frontend/src/views/buyer/Search.vue -->
<template>
  <div class="search-page">

    <el-row :gutter="20">

      <!-- 左侧筛选栏 -->
      <el-col :span="5">
        <el-card class="filter-card">
          <div class="filter-title">商品分类</div>
          <el-tree
              :data="categoryTree"
              :props="{ label: 'name', children: 'children' }"
              @node-click="onCategoryClick"
              highlight-current
              style="margin-bottom:20px"
          />

          <div class="filter-title">价格区间</div>
          <div class="price-range">
            <el-input v-model="filter.minPrice" placeholder="最低价" size="small" />
            <span style="margin:0 8px;color:#909399">—</span>
            <el-input v-model="filter.maxPrice" placeholder="最高价" size="small" />
          </div>
          <el-button
              size="small"
              type="primary"
              style="width:100%;margin-top:12px"
              @click="doSearch"
          >
            筛选
          </el-button>

          <el-divider />

          <div class="filter-title">排序方式</div>
          <el-radio-group v-model="filter.sort" @change="doSearch" style="display:flex;flex-direction:column;gap:8px">
            <el-radio value="default">综合排序</el-radio>
            <el-radio value="price_asc">价格从低到高</el-radio>
            <el-radio value="price_desc">价格从高到低</el-radio>
            <el-radio value="sales">销量优先</el-radio>
          </el-radio-group>
        </el-card>
      </el-col>

      <!-- 右侧搜索结果 -->
      <el-col :span="19">

        <!-- 结果信息栏 -->
        <div class="result-bar">
          <span class="result-info">
            搜索「<b>{{ route.query.kw }}</b>」，共找到
            <b style="color:#f56c6c">{{ total }}</b> 件商品
          </span>
          <el-button
              v-if="filter.categoryId || filter.minPrice || filter.maxPrice"
              text
              type="danger"
              @click="resetFilter"
          >
            清除筛选
          </el-button>
        </div>

        <!-- 商品列表 -->
        <div v-loading="loading">
          <el-row :gutter="16" v-if="items.length > 0">
            <el-col
                v-for="item in items"
                :key="item.id"
                :xs="12" :sm="8" :md="6"
                style="margin-bottom:16px"
            >
              <el-card
                  class="product-card"
                  :body-style="{ padding: '10px' }"
                  @click="router.push(`/product/${item.id}`)"
              >
                <div class="image-wrap">
                  <img
                      :src="item.imageUrl"
                      class="product-img"
                      @error="e => e.target.src = fallbackImg"
                  />
                </div>
                <div class="product-title" :title="item.title">{{ item.title }}</div>
                <div class="product-price">¥{{ item.price }}</div>
                <div class="product-sales">已售 {{ item.salesCount }} 件</div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 空状态 -->
          <el-empty
              v-else
              description="没有找到相关商品，换个关键词试试"
              style="margin-top:60px"
          />
        </div>

        <!-- 分页 -->
        <el-pagination
            v-if="total > 0"
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[20, 40, 60]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top:20px;justify-content:center;display:flex"
            @current-change="doSearch"
            @size-change="doSearch"
        />

      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchItems } from '../../api/item'
import { getCategoryTree } from '../../api/category'

const route  = useRoute()
const router = useRouter()

const items        = ref([])
const total        = ref(0)
const loading      = ref(false)
const categoryTree = ref([])
const pageNum      = ref(1)
const pageSize     = ref(20)
const fallbackImg  = 'https://via.placeholder.com/150?text=No+Image'

const filter = ref({
  sort:       'default',
  categoryId: null,
  minPrice:   '',
  maxPrice:   '',
})

// 执行搜索
const doSearch = async () => {
  loading.value = true
  try {
    const params = {
      kw:         route.query.kw || '',
      sort:       filter.value.sort,
      categoryId: filter.value.categoryId || undefined,
      minPrice:   filter.value.minPrice   || undefined,
      maxPrice:   filter.value.maxPrice   || undefined,
      pageNum:    pageNum.value,
      pageSize:   pageSize.value,
    }
    const { data: res } = await searchItems(params)
    if (res.code === 1) {
      items.value = res.data.rows
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

// 点击分类树节点
const onCategoryClick = (node) => {
  filter.value.categoryId = node.id
  pageNum.value = 1
  doSearch()
}

// 清除筛选
const resetFilter = () => {
  filter.value.categoryId = null
  filter.value.minPrice   = ''
  filter.value.maxPrice   = ''
  pageNum.value = 1
  doSearch()
}

// URL 里 kw 变化时重新搜索（比如从导航栏连续搜索两次）
watch(() => route.query.kw, () => {
  pageNum.value = 1
  doSearch()
})

onMounted(async () => {
  const { data: res } = await getCategoryTree()
  if (res.code === 1) categoryTree.value = res.data

  if (route.query.categoryId) {
    filter.value.categoryId = parseInt(route.query.categoryId)
  }
  doSearch()
})
</script>

<style scoped>
.search-page { padding: 20px; }

.filter-card { position: sticky; top: 20px; }
.filter-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}
.price-range { display: flex; align-items: center; }

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}
.result-info { font-size: 14px; color: #606266; }

.product-card {
  cursor: pointer;
  transition: 0.25s;
  height: 300px;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.12);
}
.image-wrap {
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #f9f9f9;
  border-radius: 4px;
}
.product-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.product-title {
  font-size: 13px;
  color: #303133;
  margin: 8px 0 4px;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.product-price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}
.product-sales {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>