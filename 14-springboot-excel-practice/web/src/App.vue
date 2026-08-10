<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const activeModule = ref('basic')
const results = ref({})
const loading = ref({})
const files = ref({})

const modules = [
  { key: 'basic', title: '01. 快速开始', desc: '最简单的导出 / 导入、核心 API 两行' },
  { key: 'annotation', title: '02. 注解与字段', desc: '列顺序、忽略字段、列宽行高、数字日期格式' },
  { key: 'style', title: '03. 样式与格式', desc: '表头/内容样式、条件高亮、合并单元格' },
  { key: 'mergehead', title: '04. 复杂表头', desc: '多级分组表头、纵向合并相同区域' },
  { key: 'bigdata', title: '05. 大数据量导出', desc: '分页边查边写、内存对比' },
  { key: 'validate', title: '06. 数据校验与错误反馈', desc: '逐行校验、问题清单回写、真实上传' },
  { key: 'listener', title: '07. 监听器与增量读取', desc: '流式逐行读取、按批落库' },
  { key: 'template', title: '08. 模板导出', desc: '模板填充、简单填充与列表填充' },
  { key: 'web', title: '09. Web 下载与导入实战', desc: '响应头规范、上传校验、权限防盗链' },
  { key: 'pitfall', title: '10. 常见坑与调优', desc: '10 个高频坑、EasyExcel vs POI' }
]

const scenarios = {
  basic: [
    { key: 'export-demo', title: '导出演示（JSON）', desc: '内存导出，返回列数/行数/文件大小', method: 'post', tip: '一行 EasyExcel.write(...).sheet(...).doWrite(list) 完成导出。' },
    { key: 'download', title: '下载员工名单.xlsx', desc: '真实导出，返回 xlsx 二进制流', type: 'download', tip: '下载后用 Excel 打开，看表头与 8 条数据。' },
    { key: 'import-demo', title: '导入演示（JSON）', desc: '内存生成样本再解析回 List，验证双向映射', method: 'post', tip: '导出的数据原样读回 → 注解双向映射成立。' },
    { key: 'overview', title: '核心概念速记', desc: 'EasyExcel 是什么 / 为什么用它 / 核心 API', method: 'get', tip: 'EasyExcel 只支持 .xlsx，不支持 .xls——面试常考。' }
  ],
  annotation: [
    { key: 'export-demo', title: '导出演示（JSON）', desc: '输出列顺序 vs 声明顺序 / 忽略字段', method: 'get', tip: '输出顺序由 order 决定，remark 被 @ExcelIgnore 掉。' },
    { key: 'download', title: '下载注解演示.xlsx', desc: '列重排 + 千分位月薪 + yyyy-MM-dd 日期', type: 'download', tip: '月薪带千分位、日期转 yyyy-MM-dd，都是注解的功劳。' },
    { key: 'import-demo', title: '导入演示（JSON）', desc: '读回文件，验证格式注解反向解析', method: 'post', tip: '导入按表头名匹配字段，@ExcelIgnore 字段不赋值。' },
    { key: 'explain', title: '注解总表（八股）', desc: '全部常用注解的作用与场景', method: 'get', tip: '一个 head 类同时定义「列怎么显示」与「对象怎么映射」。' }
  ],
  style: [
    { key: 'export-demo', title: '导出演示（JSON）', desc: '说明应用了哪些样式', method: 'get', tip: '浅蓝表头 + 全边框 + 高亮 + 合并，下载看效果。' },
    { key: 'download', title: '下载部门薪资报表.xlsx', desc: '样式完整的报表：高亮月薪>15000', type: 'download', tip: '月薪 > 15000 的行标红加粗；部门两两合并。' },
    { key: 'explain', title: '样式机制速记（八股）', desc: '注解 / 内置策略 / 自定义 Handler 三层', method: 'get', tip: '生产上「模板 + 预设样式」往往比代码调样式更省事。' }
  ],
  mergehead: [
    { key: 'export-demo', title: '导出演示（JSON）', desc: '表头层级与合并说明', method: 'get', tip: 'value 数组长度 = 表头级数，少的字段自动跨行合并。' },
    { key: 'download', title: '下载年度销售业绩表.xlsx', desc: '2 级分组表头，区域按 3 行合并', type: 'download', tip: '打开看「一季度/二季度」分组表头与区域合并。' },
    { key: 'explain', title: '复杂表头速记（八股）', desc: '多级 value / 三种合并手段', method: 'get', tip: '合并单元格影响筛选排序，生产上想清楚再用。' }
  ],
  bigdata: [
    { key: 'export-demo', title: '导出演示（JSON）', desc: '行数/页大小/耗时/文件大小', method: 'post', params: [{ name: 'rows', type: 'select', options: ['1000', '5000', '50000'] }], tip: '内存中始终只有一页数据：查一页、写一页、清一页。' },
    { key: 'download', title: '下载大数据.xlsx', desc: '分页边查边写导出 N 行', type: 'download', params: [{ name: 'rows', type: 'select', options: ['1000', '10000', '50000'] }], tip: '几十万行也能导，内存近似恒定。' },
    { key: 'compare', title: '内存对比（JSON）', desc: '全量 List vs 分页写的堆占用采样', method: 'post', params: [{ name: 'rows', type: 'select', options: ['1000', '10000', '50000'] }], tip: 'JVM 堆采样有波动，看趋势：A 方案堆里躺整个 List。' },
    { key: 'strategy', title: '大数据导出策略速记', desc: '边查边写 / POI 内存对比 / 优化要点', method: 'get', tip: '导出慢的瓶颈往往在 SQL 查询而不在写 Excel。' }
  ],
  validate: [
    { key: 'import-demo', title: '校验导入演示（JSON）', desc: '8 行样本：4 好 4 坏，返回行号与原因', method: 'post', tip: 'errorRows 里的 row 是用户在 Excel 里看到的行号。' },
    { key: 'sample-download', title: '下载样本文件', desc: '含 4 条坏数据的导入模板，可手动上传试', type: 'download', tip: '下载后可以再上传到「真实上传导入」体验完整链路。' },
    { key: 'import', title: '真实上传导入', desc: 'multipart 上传 .xlsx，走校验链路', type: 'upload', tip: '选一个 .xlsx 文件上传，返回合法行与错误行。' },
    { key: 'error-download', title: '下载问题清单.xlsx', desc: '错误回写：失败的行导出成问题清单', type: 'download', tip: '用户拿到问题清单照着改，而不是看一堆报错弹窗。' },
    { key: 'rules', title: '导入校验速记（八股）', desc: '三层校验 / 校验时机 / 事务边界', method: 'get', tip: '坏数据进库比不导入更可怕：先校验、再入库、坏行回写。' }
  ],
  listener: [
    { key: 'import-demo', title: '监听器导入演示（JSON）', desc: '250 行 → 流式读 → 每批 100 行落库', method: 'post', params: [{ name: 'rows', type: 'select', options: ['120', '250', '1000'] }], tip: '默认 batchSize=100：250 行 → 100/100/50 三批。' },
    { key: 'explain', title: '监听器机制速记（八股）', desc: '为何用监听器 / 回调方法 / 批量与断点', method: 'get', tip: 'doReadSync 把整个文件读成 List，几十万行会 OOM。' }
  ],
  template: [
    { key: 'template-download', title: '下载空白模板.xlsx', desc: '含 {占位符} 的销售订单模板', type: 'download', tip: '下载后用 Excel 改样式，样式零代码。' },
    { key: 'fill-demo', title: '模板填充演示（JSON）', desc: '说明占位符与填充结果', method: 'post', tip: '简单填充 {customer}，列表填充 {item.xxx}。' },
    { key: 'fill-download', title: '下载填充后的订单.xlsx', desc: '简单填充 + 列表填充完成的报表', type: 'download', tip: '对比空白模板看数据是怎么填进去的。' },
    { key: 'explain', title: '模板填充速记（八股）', desc: 'withTemplate / FillWrapper / forceNewRow', method: 'get', tip: '报表样式交给模板（UI 排好版），后端只填数据。' }
  ],
  web: [
    { key: 'download', title: '带规范响应头下载', desc: 'Content-Type / Content-Disposition 齐全', type: 'download', tip: '中文文件名走 RFC 5987 filename*，老浏览器也不乱码。' },
    { key: 'import', title: '真实上传导入', desc: 'multipart 上传，校验大小/类型后解析', type: 'upload', tip: '仅支持 .xlsx；超过 50MB 会被拒绝。' },
    { key: 'download-rule', title: '下载规范速记（八股）', desc: '响应头 / 中文文件名 / 权限 / 防盗链', method: 'get', tip: '下载接口必须鉴权，超大导出转异步 + 带有效期链接。' },
    { key: 'upload-limit', title: '上传限制说明', desc: 'multipart 配置 / 超大文件方案', method: 'get', tip: 'Spring 默认上传只有 1MB，本项目调到 50MB。' }
  ],
  pitfall: [
    { key: 'list', title: '10 个高频坑清单', desc: '现象 → 原因 → 解法', method: 'get', tip: '先看清单再动手，少走弯路。' },
    { key: 'poi-vs-easyexcel', title: 'EasyExcel vs POI 对比', desc: '内存 / 代码量 / 格式 / 场景', method: 'get', tip: '自己造轮子用 POI，业务导入导出直接用 EasyExcel。' },
    { key: 'head-mismatch-demo', title: '表头不匹配现场演示', desc: '列名差一个字，id 字段静默丢失', method: 'get', tip: '「员工工号」≠「员工编号」，导入数据静默丢失比报错更危险。' },
    { key: 'tuning', title: '调优要点（八股）', desc: '读 / 写 / 线程池 / 限流 / 超大文件', method: 'get', tip: '超大导入先落临时表，异步分批处理，别在请求里一把做完。' }
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

function buildUrl(scenario) {
  const cacheKey = `${activeModule.value}-${scenario.key}`
  const params = paramValues.value[cacheKey] || {}
  const qs = new URLSearchParams(params).toString()
  return `/api/${activeModule.value}/${scenario.key}${qs ? '?' + qs : ''}`
}

function downloadScenario(scenario) {
  // 直接开新标签页让浏览器处理下载，后端响应头里带好了文件名
  window.open(buildUrl(scenario), '_blank')
}

async function callScenario(scenario) {
  const cacheKey = `${activeModule.value}-${scenario.key}`
  loading.value[cacheKey] = true
  results.value[cacheKey] = ''

  try {
    const params = paramValues.value[cacheKey] || defaultParams(scenario.key)
    const url = `/api/${activeModule.value}/${scenario.key}`
    let res
    if (scenario.type === 'upload') {
      // 文件上传：multipart
      const file = files.value[cacheKey]
      if (!file) {
        results.value[cacheKey] = '请先选择要上传的 .xlsx 文件'
        return
      }
      const form = new FormData()
      form.append('file', file)
      res = await axios.post(url, form, { headers: { 'Content-Type': 'multipart/form-data' } })
    } else if (scenario.method === 'get') {
      res = await axios.get(url, { params })
    } else {
      res = await axios.post(url, null, { params })
    }
    results.value[cacheKey] = JSON.stringify(res.data, null, 2)
  } catch (err) {
    const data = err.response ? err.response.data : { message: err.message }
    results.value[cacheKey] = '请求异常：\n' + JSON.stringify(data, null, 2)
  } finally {
    loading.value[cacheKey] = false
  }
}

function onFileChange(e, cacheKey) {
  files.value[cacheKey] = e.target.files[0] || null
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
      <h1>Spring Boot +<br>EasyExcel 实践</h1>
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
                <option v-for="opt in p.options" :key="opt" :value="opt">{{ opt }}</option>
              </select>
              <input v-else v-model="paramValues[`${activeModule}-${s.key}`][p.name]" />
            </label>
          </div>
          <input
            v-if="s.type === 'upload'"
            class="file-input"
            type="file"
            accept=".xlsx"
            @change="onFileChange($event, `${activeModule}-${s.key}`)"
          />
          <button
            v-if="s.type === 'download'"
            class="btn btn-download"
            @click="downloadScenario(s)"
          >
            下载文件
          </button>
          <button
            v-else
            class="btn"
            :class="s.type === 'upload' ? 'btn-upload' : ''"
            @click="callScenario(s)"
            :disabled="loading[`${activeModule}-${s.key}`]"
          >
            {{ loading[`${activeModule}-${s.key}`] ? '执行中...' : (s.type === 'upload' ? '上传并解析' : '运行实验') }}
          </button>
          <div v-if="s.tip" class="tip">💡 {{ s.tip }}</div>
          <pre v-if="results[`${activeModule}-${s.key}`]" class="result">{{ results[`${activeModule}-${s.key}`] }}</pre>
        </div>
      </div>
    </main>
  </div>
</template>
