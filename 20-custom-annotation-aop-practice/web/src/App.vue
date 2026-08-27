<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activeModule = ref('log')
const results = ref({})
const loading = ref({})
const role = ref('admin')

const modules = [
  { key: 'log', title: '01. 操作日志', desc: '@LogOperation 自动记录方法调用' },
  { key: 'permission', title: '02. 权限校验', desc: '@RequirePermission 拦截无权限请求' },
  { key: 'rateLimit', title: '03. 接口限流', desc: '@RateLimit 控制访问频率' },
  { key: 'masking', title: '04. 数据脱敏', desc: '@DataMasking 对敏感字段脱敏' },
  { key: 'timing', title: '05. 耗时监控', desc: '@Timing 计算方法执行耗时' },
  { key: 'combine', title: '06. 注解组合', desc: '多个注解叠加使用' },
  { key: 'builtin', title: '07. 内置注解', desc: '@Override / @Deprecated / @SuppressWarnings' },
  { key: 'inherited', title: '08. @Inherited', desc: '子类继承父类上的注解' },
  { key: 'repeatable', title: '09. @Repeatable', desc: '同一个注解多次使用' },
  { key: 'validate', title: '10. 参数校验', desc: '@Valid + Bean Validation' },
  { key: 'explain', title: '11. 八股速记', desc: '核心考点与坑点清单' }
]

const scenarios = {
  log: [
    { key: 'log', title: '查询用户（记录日志）', method: 'get', url: '/api/demo/log?id=1', tip: '观察后端控制台输出的 JSON 格式操作日志。' }
  ],
  permission: [
    { key: 'admin', title: 'admin 接口', method: 'get', url: '/api/demo/permission/admin', needRole: true, tip: 'X-Role=admin 时通过，其他角色返回 403。' },
    { key: 'user', title: 'user:view 接口', method: 'get', url: '/api/demo/permission/user', needRole: true, tip: 'admin 和 user 角色都能访问。' }
  ],
  rateLimit: [
    { key: 'rate-limit', title: '限流接口（1秒2次）', method: 'get', url: '/api/demo/rate-limit', tip: '连续快速点击，第3次会触发 429 限流。' }
  ],
  masking: [
    { key: 'masking', title: '单对象脱敏', method: 'get', url: '/api/demo/masking', tip: 'phone / email / idCard 会被替换为 ****。' },
    { key: 'masking-list', title: '列表脱敏', method: 'get', url: '/api/demo/masking-list', tip: '集合中的每个对象都会脱敏。' }
  ],
  timing: [
    { key: 'timing', title: '耗时监控', method: 'get', url: '/api/demo/timing', tip: '后端日志会打印方法执行耗时。' }
  ],
  combine: [
    { key: 'combine', title: '组合注解', method: 'get', url: '/api/demo/combine', needRole: true, tip: '同时经过日志、权限、限流、耗时四个切面。' }
  ],
  explain: [
    { key: 'explain', title: '八股速记', method: 'get', url: '/api/demo/explain', tip: '本专题所有知识点速查。' }
  ],
  builtin: [
    { key: 'builtin', title: '内置注解演示', method: 'get', url: '/api/demo/builtin', tip: '观察 @Override / @Deprecated / @SuppressWarnings 的用法与说明。' }
  ],
  inherited: [
    { key: 'inherited', title: '@Inherited 演示', method: 'get', url: '/api/demo/inherited', tip: '验证子类是否继承了父类上的 @InheritedMarker。' }
  ],
  repeatable: [
    { key: 'audit', title: '@Repeatable 演示', method: 'get', url: '/api/demo/audit', tip: '读取方法上的多个 @Audit 注解。' }
  ],
  validate: [
    { key: 'valid', title: '合法用户', desc: '校验通过 200', method: 'post', url: '/api/demo/validate', body: { name: '张三', phone: '13800138001', email: 'zhangsan@example.com', idCard: '110101199001011234' }, tip: '所有字段符合约束，返回 code=200。' },
    { key: 'invalid', title: '非法用户', desc: '校验失败 400', method: 'post', url: '/api/demo/validate', body: { name: '', phone: '123', email: 'invalid', idCard: 'xxx' }, tip: '字段不符合约束，全局异常处理器返回 code=400。' }
  ]
}

async function callScenario(scenario) {
  const cacheKey = `${activeModule.value}-${scenario.key}`
  loading.value[cacheKey] = true
  results.value[cacheKey] = ''

  try {
    const config = scenario.needRole ? { headers: { 'X-Role': role.value } } : {}
    let res
    if (scenario.method === 'get') {
      res = await axios.get(scenario.url, config)
    } else {
      res = await axios.post(scenario.url, scenario.body || {}, config)
    }
    results.value[cacheKey] = JSON.stringify(res.data, null, 2)
  } catch (err) {
    const data = err.response ? err.response.data : { message: err.message }
    results.value[cacheKey] = '请求异常：\n' + JSON.stringify(data, null, 2)
  } finally {
    loading.value[cacheKey] = false
  }
}
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>自定义注解 + AOP<br>高阶玩法实战</h1>
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

      <div class="role-bar" v-if="activeModule === 'permission' || activeModule === 'combine'">
        <label>
          当前角色（X-Role）:
          <select v-model="role">
            <option value="admin">admin</option>
            <option value="user">user</option>
            <option value="anonymous">anonymous</option>
          </select>
        </label>
      </div>

      <div class="cards">
        <div v-for="s in scenarios[activeModule]" :key="s.key" class="card">
          <h3>{{ s.title }}</h3>
          <div class="url">{{ s.method.toUpperCase() }} {{ s.url }}</div>
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
