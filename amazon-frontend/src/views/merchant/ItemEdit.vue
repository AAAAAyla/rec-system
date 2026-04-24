<!-- amazon-frontend/src/views/merchant/ItemEdit.vue -->
<template>
  <div>
    <div class="page-title">{{ isEdit ? '编辑商品' : '发布商品' }}</div>

    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">

        <el-form-item label="商品名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入商品名称" maxlength="100" show-word-limit>
            <template #append>
              <el-button @click="autoClassify" :loading="classifying" type="primary" plain>
                AI 识别分类
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="商品分类" prop="categoryId">
          <el-cascader
              v-model="form.categoryId"
              :options="categoryTree"
              :props="{ value: 'id', label: 'name', children: 'children', emitPath: false }"
              placeholder="请选择分类"
              style="width:300px"
          />
          <span v-if="aiCatHint" style="margin-left:8px;color:#67c23a;font-size:12px">{{ aiCatHint }}</span>
        </el-form-item>

        <el-form-item label="主图URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="请输入主图URL" />
          <div v-if="form.imageUrl" style="margin-top:8px">
            <el-image :src="form.imageUrl" style="width:100px;height:100px;border-radius:4px" fit="cover" />
          </div>
        </el-form-item>

        <el-form-item label="默认价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
          <span style="margin-left:8px;color:#909399;font-size:12px">（SKU 未配置时展示此价格）</span>
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="请输入商品描述"
          />
        </el-form-item>

        <!-- SKU 配置 -->
        <el-form-item label="SKU 规格">
          <div class="sku-section">
            <el-table :data="skus" border style="margin-bottom:12px">
              <el-table-column label="规格描述" min-width="200">
                <template #default="{ row }">
                  <el-input v-model="row.specJson" placeholder='如：{"颜色":"红","尺寸":"M"}' />
                </template>
              </el-table-column>
              <el-table-column label="价格" width="140">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :min="0.01" :precision="2" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="库存" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.stock" :min="0" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button type="danger" text size="small" @click="skus.splice($index, 1)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button plain :icon="Plus" @click="addSku">添加规格</el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">
            {{ isEdit ? '保存修改' : '提交发布' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>

      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { publishItem, updateItem, getItemDetail } from '../../api/item'
import { getCategoryTree } from '../../api/category'

const route = useRoute()
const router = useRouter()
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const isEdit  = computed(() => !!route.params.id)
const formRef = ref()
const submitting = ref(false)
const classifying = ref(false)
const aiCatHint = ref('')

const form = ref({
  title: '',
  categoryId: null,
  imageUrl: '',
  price: 0.01,
  description: '',
})

const skus = ref([])
const categoryTree = ref([])

const rules = {
  title:      [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类',     trigger: 'change' }],
  price:      [{ required: true, message: '请输入价格',     trigger: 'blur' }],
}

const addSku = () => {
  skus.value.push({ specJson: '', price: 0.01, stock: 0 })
}

const autoClassify = async () => {
  if (!form.value.title?.trim()) {
    ElMessage.warning('请先输入商品名称')
    return
  }
  classifying.value = true
  aiCatHint.value = ''
  try {
    const { data: res } = await axios.post(`${BASE}/categories/auto-classify`,
        { title: form.value.title },
        { headers: { Authorization: localStorage.getItem('token') } })
    if (res.code === 1 && res.data?.categoryId) {
      form.value.categoryId = res.data.categoryId
      const name = findCatName(res.data.categoryId, categoryTree.value)
      aiCatHint.value = name ? `AI 推荐：${name}` : 'AI 已选择分类'
      ElMessage.success('AI 已自动识别分类')
    } else {
      ElMessage.warning(res.msg || '无法自动识别，请手动选择')
    }
  } catch {
    ElMessage.error('AI 分类识别失败')
  } finally {
    classifying.value = false
  }
}

const findCatName = (id, tree) => {
  for (const node of tree) {
    if (node.id === id) return node.name
    if (node.children) {
      const found = findCatName(id, node.children)
      if (found) return found
    }
  }
  return null
}

const submit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { item: form.value, skus: skus.value }
    const { data: res } = isEdit.value
        ? await updateItem(route.params.id, payload)
        : await publishItem(payload)

    if (res.code === 1) {
      ElMessage.success(isEdit.value ? '修改成功' : '发布成功，等待审核')
      router.push('/merchant/items')
    } else {
      ElMessage.error(res.msg)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载分类树
  const { data: catRes } = await getCategoryTree()
  if (catRes.code === 1) categoryTree.value = catRes.data

  // 编辑模式：回填数据
  if (isEdit.value) {
    const { data: res } = await getItemDetail(route.params.id)
    if (res.code === 1) {
      const item = res.data
      form.value = {
        title:       item.title,
        categoryId:  item.categoryId,
        imageUrl:    item.imageUrl,
        price:       item.price,
        description: item.description,
      }
      skus.value = item.skus || []
    }
  }
})
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: bold; color: #303133; margin-bottom: 20px; }
.sku-section { width: 100%; }
</style>