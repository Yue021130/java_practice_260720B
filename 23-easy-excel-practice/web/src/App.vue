<template>
  <div class="app">
    <header>
      <h1>Excel 导入导出实战</h1>
      <p>Easypoi 导入校验 + EasyExcel 自定义 Converter 完整复现</p>
    </header>

    <main>
      <!-- 1. Easypoi 导入模板 -->
      <section class="card">
        <h2>1. 下载导入模板</h2>
        <button @click="downloadTemplate">下载系统用户模板</button>
        <p class="tip">模板包含：姓名、部门编码、角色编码、手机号、邮箱、性别、工号。</p>
      </section>

      <!-- 2. Easypoi 基础导入 -->
      <section class="card">
        <h2>2. Easypoi 基础导入</h2>
        <input type="file" accept=".xlsx,.xls" @change="onBasicFileChange" />
        <button @click="importBasic" :disabled="!basicFile">基础导入</button>
        <p class="tip">无校验，直接解析为 SysUserImport 列表返回。</p>
      </section>

      <!-- 3. Easypoi 带校验导入 -->
      <section class="card">
        <h2>3. Easypoi 带校验导入（业务校验 + 错误日志）</h2>
        <input type="file" accept=".xlsx,.xls" @change="onVerifyFileChange" />
        <button @click="importVerify" :disabled="!verifyFile">带校验导入</button>
        <p class="tip">校验必填、手机号/邮箱格式、数据库唯一性；错误行生成错误日志 Excel。</p>
      </section>

      <!-- 4. Easypoi Map 导入 -->
      <section class="card">
        <h2>4. Easypoi Map 方式导入</h2>
        <input type="file" accept=".xlsx,.xls" @change="onMapFileChange" />
        <button @click="importMap" :disabled="!mapFile">Map 导入</button>
        <p class="tip">不定义实体类，直接读取为 Map&lt;String, Object&gt;。</p>
      </section>

      <!-- 5. Easypoi 组内重复校验 -->
      <section class="card">
        <h2>5. Easypoi 组内重复校验（ThreadLocal）</h2>
        <input type="file" accept=".xlsx,.xls" @change="onDuplicateFileChange" />
        <button @click="importDuplicate" :disabled="!duplicateFile">重复校验导入</button>
        <p class="tip">同一批次内出现重复姓名会被标记为失败。</p>
      </section>

      <!-- 6. EasyExcel 导出 -->
      <section class="card">
        <h2>6. EasyExcel 导出商品（自定义 WhetherConverter）</h2>
        <button @click="initProducts">初始化商品数据</button>
        <button @click="exportProducts">导出商品 Excel</button>
        <p class="tip">先初始化数据，再导出；演示 1/0 与 是/否 的字段转换。</p>
      </section>

      <!-- 7. 八股速记 -->
      <section class="card">
        <h2>7. 八股速记</h2>
        <button @click="loadExplain">加载核心考点</button>
      </section>

      <div class="result">
        <h3>结果</h3>
        <pre v-if="result">{{ JSON.stringify(result, null, 2) }}</pre>
        <p v-if="error" class="error">{{ error }}</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from './api/excel.js'

const result = ref(null)
const error = ref('')

const basicFile = ref(null)
const verifyFile = ref(null)
const mapFile = ref(null)
const duplicateFile = ref(null)

function reset() {
  result.value = null
  error.value = ''
}

async function call(name, fn) {
  reset()
  try {
    const res = await fn()
    // 统一响应包装：{ code, message, data }
    if (res && res.code !== undefined && res.code !== 200) {
      throw new Error(res.message || '业务失败')
    }
    result.value = res && res.data !== undefined ? res.data : res
  } catch (e) {
    error.value = e.message
  }
}

function onBasicFileChange(e) {
  basicFile.value = e.target.files[0]
}
function onVerifyFileChange(e) {
  verifyFile.value = e.target.files[0]
}
function onMapFileChange(e) {
  mapFile.value = e.target.files[0]
}
function onDuplicateFileChange(e) {
  duplicateFile.value = e.target.files[0]
}

function downloadTemplate() {
  reset()
  api.downloadTemplate().catch(e => { error.value = e.message })
}

function importBasic() {
  call('importBasic', () => api.importBasic(basicFile.value))
}

function importVerify() {
  call('importVerify', () => api.importVerify(verifyFile.value))
}

function importMap() {
  call('importMap', () => api.importMap(mapFile.value))
}

function importDuplicate() {
  call('importDuplicate', () => api.importDuplicate(duplicateFile.value))
}

function initProducts() {
  call('initProducts', () => api.initProducts())
}

function exportProducts() {
  reset()
  api.exportProducts().catch(e => { error.value = e.message })
}

function loadExplain() {
  call('explain', () => api.explain())
}
</script>

<style>
* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background: #f5f7fa;
  color: #333;
}
.app { max-width: 900px; margin: 0 auto; padding: 20px; }
header { text-align: center; margin-bottom: 24px; }
header h1 { margin: 0; font-size: 28px; }
header p { color: #666; margin-top: 6px; }

.card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.card h2 {
  font-size: 18px;
  margin-bottom: 12px;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}
.card input[type="file"] {
  display: block;
  margin-bottom: 10px;
}
.card button {
  padding: 6px 16px;
  border: none;
  background: #409eff;
  color: #fff;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
}
.card button:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}
.tip {
  color: #999;
  font-size: 13px;
  margin-top: 10px;
}

.result {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.result pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}
.error { color: #f56c6c; }
</style>
