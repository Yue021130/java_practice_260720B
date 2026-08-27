<script setup>
import { ref, reactive } from 'vue'
import axios from './api/request.js'

const activeModule = ref('basic')
const results = ref({})
const loading = ref({})

const modules = [
  { key: 'basic', title: '01. 基础返回', desc: '单个 / 列表 / 分页，看自动包装结构' },
  { key: 'valid', title: '02. 参数校验', desc: '@Valid 失败统一返回 code=400' },
  { key: 'exception', title: '03. 业务异常', desc: 'BusinessException 也走 Result' },
  { key: 'wrap', title: '04. 自动包装特例', desc: 'String、手动包装、避免重复' },
  { key: 'download', title: '05. 文件下载', desc: '@IgnoreResultWrap 跳过包装' },
  { key: 'explain', title: '06. 八股速记', desc: '核心考点与坑点清单' }
]

const scenarios = {
  basic: [
    { key: 'detail', title: '查询单个用户', desc: '返回 Result<UserVO>', method: 'get', url: '/user/101', tip: 'Controller 返回 UserVO，自动包一层 Result。' },
    { key: 'detail-with-msg', title: '查询单个用户（自定义提示）', desc: 'ResultFactory.success(msg, data)', method: 'get', url: '/user/detail-with-msg/101', tip: '演示 success(msg, data) 重载，返回 Result<UserVO>。' },
    { key: 'list', title: '查询用户列表', desc: '返回 Result<List<UserVO>>', method: 'get', url: '/user/list', tip: 'Controller 返回 List，自动包一层 Result。' },
    { key: 'page', title: '分页查询', desc: '返回 Result<PageResult<UserVO>>', method: 'get', url: '/user/page?pageNum=1&pageSize=3', tip: 'Controller 返回 PageResult，自动包一层 Result。' }
  ],

  valid: [
    { key: 'valid-create', title: '合法表单提交', desc: 'code=0', method: 'post', url: '/user/create', body: { name: '周八', age: 25, email: 'zhouba@example.com', phone: '13300133003', password: '123456' }, tip: '所有字段符合校验规则。' },
    { key: 'invalid-create', title: '非法表单提交', desc: 'code=400', method: 'post', url: '/user/create', body: { name: '', age: -1, email: 'invalid', phone: '123', password: '123' }, tip: '观察全局异常处理器返回的统一错误。' }
  ],
  exception: [
    { key: 'not-found', title: '业务异常示例', desc: 'code=404', method: 'get', url: '/user/not-found', tip: '主动抛 BusinessException，验证异常统一处理。' },
    { key: 'update-missing', title: '更新不存在用户', desc: 'code=404', method: 'post', url: '/user/update', body: { id: 99999, name: '不存在', age: 20, email: 'no@example.com', phone: '13300133004', password: '123456' }, tip: 'Service 层抛异常，全局处理。' }
  ],
  wrap: [
    { key: 'raw-string', title: 'String 推荐做法', desc: '直接返回 Result<String>', method: 'get', url: '/user/raw-string', tip: '原文推荐：不要让 Controller 返回裸 String，而是返回 ResultFactory.success("已发送")。' },
    { key: 'raw-string-bare', title: 'String 跳过包装', desc: '@IgnoreResultWrap 返回纯文本', method: 'get', url: '/user/raw-string-bare', tip: '另一种稳妥做法：明确跳过统一包装，直接返回 text/plain。' },
    { key: 'manual-wrap', title: '手动包装示例', desc: 'Controller 直接返回 Result', method: 'get', url: '/user/manual-wrap/101', tip: '验证不会 Result 套 Result。' }
  ],
  download: [
    { key: 'download', title: '文件下载', desc: '跳过统一包装', method: 'get', url: '/user/download', responseType: 'blob', tip: '验证 Content-Type 是文件流，body 里没有 code 字段。' }
  ],
  explain: [
    { key: 'explain', title: '八股速记', desc: '核心考点与坑点清单', method: 'get', url: '/user/explain', tip: '本专题所有知识点速查。' }
  ]
}

const rawResults = reactive({})

async function callScenario(scenario) {
  const cacheKey = `${activeModule.value}-${scenario.key}`
  loading.value[cacheKey] = true
  results.value[cacheKey] = ''
  rawResults[cacheKey] = null

  try {
    const config = scenario.responseType ? { responseType: scenario.responseType } : {}
    let res
    if (scenario.method === 'get') {
      res = await axios.get(scenario.url, config)
    } else {
      res = await axios.post(scenario.url, scenario.body || {}, config)
    }

    rawResults[cacheKey] = res

    // 文件下载场景：展示 blob 信息
    if (scenario.responseType === 'blob') {
      results.value[cacheKey] = `下载成功，Content-Type: ${res.headers['content-type']}\n文件大小: ${res.data.size || res.data.length} bytes`
    } else {
      results.value[cacheKey] = JSON.stringify(res, null, 2)
    }
  } catch (err) {
    const data = err.response ? err.response.data : { message: err.message }
    rawResults[cacheKey] = data
    results.value[cacheKey] = '请求异常：\n' + JSON.stringify(data, null, 2)
  } finally {
    loading.value[cacheKey] = false
  }
}

function showRaw(cacheKey) {
  const raw = rawResults[cacheKey]
  if (raw && raw.config) {
    alert(`实际请求 URL: ${raw.config.url}\n响应状态: ${raw.status}\n完整响应 data 已打印到控制台`)
    console.log('raw response:', raw)
  } else {
    alert('暂无原始响应数据')
  }
}
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>统一返回结果<br>封装实战</h1>
      <div
        v-for="m in modules"
        :key="m.key"
        :class="['menu-item', { active: activeModule === m.key }]"
        @click="activeModule = m.key"
      >
        {{ m.title }}
      </div>
    </aside>
    <main class="content">
      <h2 class="module-title">
        {{ modules.find(m => m.key === activeModule).title }}
      </h2>
      <p class="module-desc">{{ modules.find(m => m.key === activeModule).desc }}</p>
      <div class="cards">
        <div v-for="s in scenarios[activeModule]" :key="s.key" class="card">
          <h3>{{ s.title }}</h3>
          <p>{{ s.desc }}</p>
          <div class="url">{{ s.method.toUpperCase() }} {{ s.url }}</div>
          <div v-if="s.body" class="body-preview">
            请求体：{{ JSON.stringify(s.body) }}
          </div>
          <button
            class="btn"
            @click="callScenario(s)"
            :disabled="loading[`${activeModule}-${s.key}`]"
          >
            {{ loading[`${activeModule}-${s.key}`] ? '执行中...' : '运行实验' }}
          </button>
          <button
            v-if="rawResults[`${activeModule}-${s.key}`]"
            class="btn btn-secondary"
            @click="showRaw(`${activeModule}-${s.key}`)"
          >
            看原始响应
          </button>
          <div v-if="s.tip" class="tip">💡 {{ s.tip }}</div>
          <pre v-if="results[`${activeModule}-${s.key}`]" class="result">{{ results[`${activeModule}-${s.key}`] }}</pre>
        </div>
      </div>
    </main>
  </div>
</template>
