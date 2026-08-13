<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activeModule = ref('principle')
const results = ref({})
const loading = ref({})

const modules = [
  { key: 'principle', title: '01. 核心原理', desc: 'appid / appkey / 签名三要素' },
  { key: 'sign', title: '02. 签名计算', desc: 'Canonical String 9 字段 + HMAC-SHA256' },
  { key: 'verify', title: '03. 服务端验签', desc: '篡改任一字段 → 签名失败' },
  { key: 'timestamp', title: '04. 防重放-时间戳', desc: '±5 分钟窗口 / 过期拒绝' },
  { key: 'nonce', title: '05. 防重放-nonce', desc: '重复使用拒绝（SETNX+TTL）' },
  { key: 'body', title: '06. 请求体完整性', desc: 'Content-MD5 / HashedPayload' },
  { key: 'canonical', title: '07. 规范化', desc: 'query / headers 排序规则' },
  { key: 'simplified', title: '08. 简化版方案', desc: 'appid+timestamp+nonce+uri+params' },
  { key: 'interceptor', title: '09. 拦截器实战', desc: '@RequireSign + 受保护接口' },
  { key: 'summary', title: '10. 选型对比', desc: 'HMAC vs API Key vs JWT vs OAuth' }
]

const scenarios = {
  principle: [
    { key: 'elements', title: '三要素速记', desc: 'appid 公开 / appkey 不传输 / 签名是 HMAC', method: 'get', tip: 'appid 是你是谁，appkey 是暗号，签名是用暗号盖的章。' },
    { key: 'flow', title: '鉴权流程 6 步', desc: '组装 → 签名 → 发请求 → 查key → 重算 → 比对', method: 'get', tip: '签名只在客户端生成一次，服务端每次独立重算。' },
    { key: 'vs-apikey', title: '签名 vs 简单 API Key', desc: '为什么不能直接传 key', method: 'get', tip: 'API Key：泄露 / 可重放 / 无完整性；HMAC 三者全解。' },
    { key: 'explain', title: '原理速记（八股）', desc: '为什么大厂都选 HMAC-SHA256', method: 'get', tip: '机器对机器用 HMAC，人对机器用 JWT，第三方用 OAuth。' }
  ],
  sign: [
    { key: 'compute', title: '完整签名计算', desc: '9 字段 Canonical String + HMAC-SHA256', method: 'get', params: [{ name: 'method', type: 'select', options: ['GET', 'POST'] }, { name: 'uri', type: 'select', options: ['/api/v1/users', '/api/v1/order/query', '/api/v1/pay/notify'] }, { name: 'query', type: 'select', options: ['page=1&size=20', 'orderNo=20240701001&page=1', ''] }], tip: '把 canonicalString 喂给任意 HMAC-SHA256 工具，应得到相同签名。' },
    { key: 'canonical', title: 'Canonical String 拆解', desc: '每个字段怎么来、怎么拼', method: 'get', tip: '9 字段固定顺序 + \\n 分隔，两端必须一致。' },
    { key: 'verify-manual', title: '手工验签对照', desc: '给定 appkey / 待签串 / 签名，重算比对', method: 'get', params: [{ name: 'appKey' }, { name: 'toSign' }, { name: 'signature' }], tip: '重算一致 → 通过；不一致 → appkey 或待签串有差异。' },
    { key: 'explain', title: '签名算法速记（八股）', desc: '固定顺序 / 空字段规则', method: 'get', tip: '顺序、分隔符、空字段规则两端一致，签名才对得上。' }
  ],
  verify: [
    { key: 'demo', title: '验签全流程', desc: '正确签名通过 / 篡改某字段看失败', method: 'get', params: [{ name: 'tamper', type: 'select', options: ['none', 'body', 'timestamp', 'uri', 'query'] }], tip: '任一被签名覆盖的字段被篡改，客户端签名就失效。' },
    { key: 'explain', title: '验签逻辑速记（八股）', desc: '提取 → 时间戳 → nonce → appkey → 重算比对', method: 'get', tip: '先做廉价的拒绝，最后才做昂贵的 HMAC。' }
  ],
  timestamp: [
    { key: 'demo', title: '时间戳窗口校验', desc: 'now / -3600(过期) / +3600(未来)', method: 'get', params: [{ name: 'timestamp', type: 'select', options: ['now', '-3600', '+3600'] }], tip: '过期（老重放）与未来请求都会被拒。' },
    { key: 'explain', title: '时间戳防重放速记（八股）', desc: '与 nonce 的分工', method: 'get', tip: '时间戳挡老请求，nonce 挡窗口内重放，缺一不可。' }
  ],
  nonce: [
    { key: 'demo', title: 'nonce 去重演示', desc: '第一次占用成功，第二次被拒', method: 'get', params: [{ name: 'nonce' }], tip: '每次请求随机生成 nonce 且只用一次，重放立刻露馅。' },
    { key: 'explain', title: 'nonce 防重放速记（八股）', desc: 'SETNX + TTL', method: 'get', tip: 'nonce 是 HMAC 方案里唯一需要服务端存储的地方。' }
  ],
  body: [
    { key: 'demo', title: 'body 完整性演示', desc: '篡改 body（金额 20→9999）看失配', method: 'get', params: [{ name: 'tamper', type: 'select', options: ['false', 'true'] }], tip: 'body 参与签名后，改 body 就是改签名。' },
    { key: 'explain', title: '请求体完整性速记（八股）', desc: 'Content-MD5 vs HashedPayload', method: 'get', tip: 'HashedPayload 与 appkey 绑定，比 Content-MD5 更强。' }
  ],
  canonical: [
    { key: 'query-sort', title: 'QueryString 排序', desc: '乱序参数规范化后一致', method: 'get', tip: '不排序的话两端按不同顺序拼串，签名永远对不上。' },
    { key: 'headers-sort', title: 'Headers 排序', desc: '头名小写字典序拼接', method: 'get', tip: '不管客户端按什么顺序设头，服务端都算出同一个串。' },
    { key: 'uri-encoding', title: 'URI 规范化', desc: '路径编码规则', method: 'get', tip: 'URLEncoder 的 + 与 %20 是经典坑，两端规则必须一致。' },
    { key: 'explain', title: '规范化速记（八股）', desc: '为什么必须规范化 / 常见 bug', method: 'get', tip: '签名对不上的第一件事：把两端 canonicalString 打出来 diff。' }
  ],
  simplified: [
    { key: 'demo', title: '简化签名演示', desc: 'appid+timestamp+nonce+uri+排序参数', method: 'get', params: [{ name: 'uri', type: 'select', options: ['/api/v1/order/query', '/api/v1/user/info'] }, { name: 'params', type: 'select', options: ['orderNo=20240701001&page=1', 'userId=1001&fields=name,age', ''] }], tip: '比标准 9 字段版少一半字段，适合内部接口。' },
    { key: 'explain', title: '简化版速记（八股）', desc: '什么时候能简化 / 取舍', method: 'get', tip: '简化版 body 不参与签名，正式开放 API 建议标准版。' }
  ],
  interceptor: [
    { key: 'generate', title: '生成合法签名（模拟客户端）', desc: '返回 4 个可直接使用的 X- 头', method: 'get', tip: '把 4 个头带上访问 protected，验签通过 → 200。' },
    { key: 'secure-demo', title: '验签闭环演示', desc: '统一校验器：正常通过 / 篡改拒绝', method: 'get', params: [{ name: 'tamper', type: 'select', options: ['false', 'true'] }], tip: '与拦截器完全相同的代码路径，展示篡改 → 拒绝。' },
    { key: 'protected', title: '受保护接口（自动签名调用）', desc: '@RequireSign：点按自动生成签名并携带访问', method: 'get', signed: true, params: [{ name: 'data', type: 'select', options: ['用户数据', '订单数据'] }], tip: '未带合法签名 → 401；本卡片自动生成并携带签名 → 200。' },
    { key: 'explain', title: '拦截器实战速记（八股）', desc: '注解+拦截器机制 / 与过滤器、网关对比', method: 'get', tip: '单机用拦截器、微服务用网关：业务代码零侵入。' }
  ],
  summary: [
    { key: 'overview', title: '鉴权方案全景', desc: 'HMAC / JWT / OAuth / API Key 各自定位', method: 'get', tip: 'HMAC 只回答「调用方是谁、请求有没有被改」。' },
    { key: 'compare', title: '四种方案对比表', desc: '安全性 / 复杂度 / 适用场景', method: 'get', tip: '拿不准就选 HMAC 签名。' },
    { key: 'principles', title: '三个关键原则', desc: 'appkey 不传输 / 时间戳+nonce / 常量时间比对', method: 'get', tip: '三条原则 + body 参与签名 = 生产级底线。' },
    { key: 'pitfalls', title: '常见坑与调优', desc: '签名对不上的 5 大排查点', method: 'get', tip: 'query 忘了排序是最常见的原因。' },
    { key: 'explain', title: '总结速记（八股）', desc: '完整回答「接口鉴权怎么做」的 7 步', method: 'get', tip: '按 7 步走一遍，从原理到落地全覆盖。' }
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
    let config = { params }

    // 受保护接口：先调同模块 generate 拿签名头，再携带访问（模拟真实签名调用）
    if (scenario.signed) {
      const gen = await axios.get(`/api/${activeModule.value}/generate`)
      const headers = gen.data.data.headers || {}
      config.headers = {
        'X-App-Id': headers['X-App-Id'],
        'X-Timestamp': headers['X-Timestamp'],
        'X-Nonce': headers['X-Nonce'],
        'X-Signature': headers['X-Signature']
      }
    }

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
      <h1>HMAC-SHA256<br>接口签名鉴权实践</h1>
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
          <button
            class="btn"
            :class="s.btnClass || ''"
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
