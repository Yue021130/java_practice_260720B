<template>
  <div>
    <h3>菜单树（Hutool TreeUtil）</h3>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>树形结构</span>
            <el-button style="float: right;" type="primary" size="small" @click="loadTree">刷新</el-button>
          </template>
          <el-tree :data="treeData" :props="defaultProps" node-key="id" default-expand-all />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>新增菜单</span>
          </template>
          <el-form :model="form" label-width="80px">
            <el-form-item label="父菜单">
              <el-select v-model="form.parentId" placeholder="选择父菜单" style="width: 100%;">
                <el-option label="顶级菜单" :value="0" />
                <el-option v-for="m in flatList" :key="m.id" :label="m.name" :value="m.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="菜单名称">
              <el-input v-model="form.name" placeholder="请输入菜单名称" />
            </el-form-item>
            <el-form-item label="路径">
              <el-input v-model="form.path" placeholder="例如 /system/user" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="图标">
              <el-input v-model="form.icon" placeholder="例如 User" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAdd">新增</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMenuTree, getMenuList, addMenu } from '../api/menu.js'

const treeData = ref([])
const flatList = ref([])
const form = ref({ parentId: 0, name: '', path: '', sort: 0, icon: '' })

const defaultProps = {
  children: 'children',
  label: 'name'
}

async function loadTree() {
  treeData.value = await getMenuTree()
}

async function loadFlatList() {
  flatList.value = await getMenuList()
}

async function handleAdd() {
  if (!form.value.name) {
    ElMessage.warning('菜单名称不能为空')
    return
  }
  await addMenu(form.value)
  ElMessage.success('新增成功')
  form.value = { parentId: 0, name: '', path: '', sort: 0, icon: '' }
  await loadTree()
  await loadFlatList()
}

onMounted(() => {
  loadTree()
  loadFlatList()
})
</script>
