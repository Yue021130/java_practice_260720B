<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activeModule = ref('userprofile')
const results = ref({})
const loading = ref({})

const modules = [
  { key: 'userprofile', title: '01. 用户画像聚合', desc: 'Optional 解包用户 → Stream 聚合订单' },
  { key: 'report', title: '02. 订单报表统计', desc: 'Optional 默认时间范围 → Stream 分组汇总' },
  { key: 'permission', title: '03. 菜单权限树', desc: 'Optional.flatMap → Stream 递归建树' },
  { key: 'dataclean', title: '04. 批量数据清洗', desc: 'Optional 单字段清洗 → Stream 批量过滤' },
  { key: 'sku', title: '05. SKU 最优价格', desc: 'Optional 解包商品 → Stream min/max 取极值' },
  { key: 'notification', title: '06. 消息通知过滤', desc: 'Optional.ifPresent 审计 → Stream 纯过滤' },
  { key: 'excelimport', title: '07. Excel 导入校验', desc: 'Optional 链式校验 → Stream 错误聚合' },
  { key: 'paging', title: '08. 分页再加工', desc: 'Optional 解包分页列表 → Stream 排序转换' },
  { key: 'pitfall', title: '09. 反模式对比', desc: '错误写法 vs 正确写法对照' }
]

const scenarios = {
  userprofile: [
    { key: 'aggregate', title: '聚合用户画像', desc: 'VIP + 邮箱有效才聚合订单', method: 'get', params: [{ name: 'userId', type: 'select', options: ['1', '2', '3', '4', '5', '99'] }], tip: '观察 userId=1(VIP) 与 userId=2(邮箱为空) 的降级差异。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: 'Optional 解包对象，Stream 聚合集合。' }
  ],
  report: [
    { key: 'summary', title: '订单汇总报表', desc: '最近 N 天汇总 + Top3 用户', method: 'get', params: [{ name: 'days', type: 'select', options: ['', '7', '30', '365'] }], tip: 'days 为空时使用配置文件默认值 30。' },
    { key: 'by-status', title: '按状态分组', desc: ' groupingBy 分组计数与求和', method: 'get', tip: '感受 groupingBy + reducing 的黄金组合。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: 'Optional 处理可空参数默认值。' }
  ],
  permission: [
    { key: 'tree', title: '菜单权限树', desc: '按角色构建树形菜单', method: 'get', params: [{ name: 'roleCode', type: 'select', options: ['', 'admin', 'user', 'guest'] }], tip: 'admin 看全部，user 只看订单，guest 只看用户列表。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: 'Java 8 没有 Optional.stream()，用 flatMap 展平。' }
  ],
  dataclean: [
    { key: 'clean', title: '批量数据清洗', desc: '清洗脏数据，显式丢弃错误行', method: 'get', params: [{ name: 'maxRows', type: 'select', options: ['', '3', '5', '10'] }], tip: '观察哪些行因字段非法被丢弃。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: 'ETL 流程中 Optional 单字段 + Stream 批量是标准组合。' }
  ],
  sku: [
    { key: 'best-price', title: 'SKU 最优价格', desc: '过滤有效 SKU 后取最低价', method: 'get', params: [{ name: 'productId', type: 'select', options: ['1', '2', '999'] }], tip: 'productId=1 有有效 SKU；999 展示 orElseGet 兜底。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: 'orElseGet 适合默认值需要计算的场景。' }
  ],
  notification: [
    { key: 'filter', title: '消息通知过滤', desc: '按用户/类型/已读/时间过滤', method: 'get', params: [{ name: 'userId', type: 'select', options: ['', '1', '2', '3'] }, { name: 'type', type: 'select', options: ['', 'PROMOTION', 'ORDER'] }], tip: 'userId/type 为空时查全部；观察日志区审计输出。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: 'Stream 中间操作无副作用，副作用用 ifPresent 隔离。' }
  ],
  excelimport: [
    { key: 'validate', title: 'Excel 导入校验', desc: '逐行校验并聚合错误', method: 'get', tip: ' Mock 数据里第 2、4 行会校验失败。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: '批量导入不要一行报错就中断，要收集全部错误。' }
  ],
  paging: [
    { key: 'transform', title: '分页结果再加工', desc: '分页后过滤、排序、字段转换', method: 'get', params: [{ name: 'page', type: 'select', options: ['1', '2', '10'] }, { name: 'size', type: 'select', options: ['3', '5', '100'] }], tip: 'page=10 会越界，观察 Optional 如何安全返回空列表。' },
    { key: 'explain', title: '八股速记', desc: '本场景核心考点', method: 'get', tip: 'Optional.ofNullable(list).filter(非空).map(stream...).orElse(emptyList())' }
  ],
  pitfall: [
    { key: 'wrong-vs-right', title: '错误 vs 正确写法', desc: '4 组反模式对照', method: 'get', tip: '重点看 orElse 与 orElseGet 的调用次数差异。' },
    { key: 'explain', title: '八股速记', desc: '反模式清单与最佳实践', method: 'get', tip: '面试常问：Optional 能做什么、不能做什么。' }
  ]
}

const paramValues = ref({})

function defaultParams(key) {
  const defs = {}
  for (const s of scenarios[activeModule.value]) {
    if (s.key === key) {
      for (const p of (s.params || [])) {
        defs[p.name] = p.options ? p.options[0] : ''
      }
    }
  }
  return defs
}

async function callScenario(scenario) {
  const cacheKey = `${activeModule.value}-${scenario.key}`
  loading.value[cacheKey] = true
  results.value[cacheKey] = ''

  try {
    const params = paramValues.value[cacheKey] || defaultParams(scenario.key)
    const url = `/api/${activeModule.value}/${scenario.key}`
    const config = { params }

    let res
    if (scenario.method === 'get') {
      res = await axios.get(url, config)
    } else {
      res = await axios.post(url, null, config)
    }
    results.value[cacheKey] = JSON.stringify(res.data, null, 2)
  } catch (err) {
    const data = err.response ? err.response.data : { message: err.message }
    results.value[cacheKey] = '请求异常：\n' + JSON.stringify(data, null, 2)
  } finally {
    loading.value[cacheKey] = false
  }
}

function setDefaultParams() {
  for (const s of scenarios[activeModule.value]) {
    const cacheKey = `${activeModule.value}-${s.key}`
    if (!paramValues.value[cacheKey]) {
      paramValues.value[cacheKey] = defaultParams(s.key)
    }
  }
}

setDefaultParams()
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>Optional + Stream<br>真实业务场景实践</h1>
      <div
        v-for="m in modules"
        :key="m.key"
        :class="['menu-item', { active: activeModule === m.key }]"
        @click="activeModule = m.key; setDefaultParams()"
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
          <div v-if="s.params" class="params">
            <label v-for="p in s.params" :key="p.name">
              {{ p.name }}:
              <select v-if="p.type === 'select'" v-model="paramValues[`${activeModule}-${s.key}`][p.name]">
                <option v-for="opt in p.options" :key="opt" :value="opt">{{ opt === '' ? '（空）' : opt }}</option>
              </select>
              <input v-else v-model="paramValues[`${activeModule}-${s.key}`][p.name]" />
            </label>
          </div>
          <button
            class="btn"
            @click="callScenario(s)"
            :disabled="loading[`${activeModule}-${s.key}`]"
          >
            {{ loading[`${activeModule}-${s.key}`] ? '执行中...' : '运行实验' }}
          </button>
          <div v-if="s.tip" class="tip">💡 {{ s.tip }}</div>
          <pre v-if="results[`${activeModule}-${s.key}`]" class="result">{{ results[`${activeModule}-${s.key}`] }}</pre>
        </div>
      </div>
    </main>
  </div>
</template>
