<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="admin-aside">
      <div class="admin-logo">管理后台</div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/admin/stats"><el-icon><DataAnalysis /></el-icon>数据统计</el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/merchants"><el-icon><Shop /></el-icon>商家审核</el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><Menu /></el-icon>分类管理</el-menu-item>
        <el-menu-item index="/admin/items"><el-icon><Goods /></el-icon>商品审核</el-menu-item>
        <el-menu-item index="/admin/orders"><el-icon><Document /></el-icon>订单监控</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.admin-layout { height: 100vh; }
.admin-aside { background: #304156; }
.admin-logo { color: #fff; font-size: 18px; font-weight: bold; padding: 20px; text-align: center; border-bottom: 1px solid #3a4a5b; }
.admin-aside .el-menu { border-right: none; background: #304156; }
.admin-aside .el-menu-item { color: #bfcbd9; }
.admin-aside .el-menu-item:hover, .admin-aside .el-menu-item.is-active { background: #263445; color: #409eff; }
</style>
