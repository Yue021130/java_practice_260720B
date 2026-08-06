<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activeModule = ref('login')
const results = ref({})
const loading = ref({})
const currentToken = ref('')
const currentLoginId = ref('')

const modules = [
  { key: 'login', title: '01. 登录认证', desc: '登录 / 注销 / Token / 指定 Token 登录' },
  { key: 'login-model', title: '02. 登录模型', desc: '单端 / 多端 / 同端互斥 / 记住我 / 七天免登' },
  { key: 'permission', title: '03. 权限认证', desc: '权限码 / 角色 / 注解鉴权 / 越级授权' },
  { key: 'route', title: '04. 路由拦截', desc: '用户 / 管理员 / 公开 / RESTful 方法鉴权' },
  { key: 'session', title: '05. Session 会话', desc: 'Account-Session / Token-Session / 自定义 Session' },
  { key: 'manage', title: '06. 踢人封禁', desc: '踢下线 / 强制注销 / 账号封禁 / 阶梯封禁' },
  { key: 'advanced', title: '07. 高级认证', desc: '二级认证 / 身份切换 / 多账号 / 加密' },
  { key: 'global', title: '08. 全局监听过滤', desc: 'Sa-Token 事件监听 / 全局过滤器' },
  { key: 'integration', title: '09. 集成扩展', desc: 'Redis / 前后端分离 / Token 续签' },
  { key: 'sso', title: '10. SSO 单点登录', desc: '服务端 / 客户端 / 单点注销 / 模式说明' },
  { key: 'oauth2', title: '11. OAuth2.0', desc: '授权码 / 密码 / 客户端 / 刷新 / 资源' },
  { key: 'jwt', title: '12. JWT & 临时 Token', desc: 'JWT 生成校验 / 临时 Token' },
  { key: 'signature', title: '13. API 签名', desc: '参数签名生成与校验（防篡改、防重放）' },
  { key: 'gateway', title: '14. 网关鉴权思路', desc: 'Gateway / ShenYu / Zuul 思路' },
  { key: 'rpc', title: '15. RPC 状态传递', desc: '上游取 Token / 下游恢复登录态' },
  { key: 'quick', title: '16. Quick 快速登录', desc: '手机号一键登录 / 扫码登录' }
]

const scenarios = {
  'login': [
    { key: 'do-login', title: '登录', desc: '指定账号 id 登录', method: 'post', params: [{ name: 'id', value: '10001' }], tip: 'StpUtil.login(id) 生成 Token 并写入 Cookie/Header。' },
    { key: 'logout', title: '注销', desc: '当前会话注销', method: 'post', tip: 'StpUtil.logout() 清除当前 Token 登录态。' },
    { key: 'is-login', title: '是否登录', desc: '查询当前会话登录状态', method: 'get', tip: 'StpUtil.isLogin()。' },
    { key: 'token-value', title: '获取 Token', desc: '获取当前 Token 值', method: 'get', tip: 'StpUtil.getTokenValue()。' },
    { key: 'login-id', title: '获取登录 ID', desc: '获取当前登录账号 id', method: 'get', tip: 'StpUtil.getLoginId()。' },
    { key: 'login-by-token', title: '指定 Token 登录', desc: '用已有 Token 恢复登录态', method: 'post', params: [{ name: 'tokenValue', value: '' }], tip: '常用于 SSO、前后端分离场景。' }
  ],
  'login-model': [
    { key: 'single', title: '单端登录', desc: '同账号仅一个设备在线', method: 'post', params: [{ name: 'id', value: '10001' }], tip: '需配合 sa-token.is-concurrent=false, is-share=true。' },
    { key: 'multi', title: '多端登录', desc: '同账号多设备同时在线', method: 'post', params: [{ name: 'id', value: '10001' }], tip: '需配合 sa-token.is-concurrent=true, is-share=false。' },
    { key: 'mutex', title: '同端互斥', desc: '相同 device 互斥，不同 device 共存', method: 'post', params: [{ name: 'id', value: '10001' }, { name: 'device', value: 'phone' }], tip: '通过 SaLoginModel.setDevice() 区分端类型。' },
    { key: 'remember', title: '记住我', desc: 'Token 30 天有效', method: 'post', params: [{ name: 'id', value: '10001' }], tip: 'SaLoginModel.setTimeout(30天)。' },
    { key: '7days', title: '七天免登', desc: 'Token 7 天有效', method: 'post', params: [{ name: 'id', value: '10001' }], tip: '常见于移动端长时效登录。' }
  ],
  'permission': [
    { key: 'login-with-perms', title: '登录并写入权限', desc: '登录后写入权限码与角色', method: 'post', params: [{ name: 'id', value: '10001' }, { name: 'perms', value: 'user:add,user:edit' }, { name: 'roles', value: 'user' }], tip: '本模块通过 StpInterface 从 Session 读取权限/角色。' },
    { key: 'check-perm', title: '代码校验权限', desc: '校验是否拥有指定权限码', method: 'get', params: [{ name: 'perm', value: 'user:add' }], tip: 'StpUtil.checkPermission("user:add")。' },
    { key: 'anno-admin', title: '注解鉴权 admin', desc: '@SaCheckPermission("admin")', method: 'get', tip: '无权限返回 403。' },
    { key: 'anno-and', title: '注解 AND 模式', desc: '需同时具备 a 和 b 权限', method: 'get', tip: '@SaCheckPermission(value={"a","b"}, mode=AND)。' },
    { key: 'anno-or', title: '注解 OR 模式', desc: '具备 a 或 b 任一权限即可', method: 'get', tip: '@SaCheckPermission(value={"a","b"}, mode=OR)。' },
    { key: 'check-role', title: '角色认证', desc: '@SaCheckRole("admin")', method: 'get', params: [{ name: 'role', value: 'admin' }], tip: '无角色返回 403。' },
    { key: 'grant', title: '越级授权', desc: '给当前会话临时加权限', method: 'post', params: [{ name: 'perm', value: 'admin' }], tip: '临时写入 Session，刷新后失效。' }
  ],
  'route': [
    { key: 'user-info', title: '用户模块', desc: '/api/route/user/info', method: 'get', tip: '路由拦截器校验 user 权限。' },
    { key: 'admin-info', title: '管理模块', desc: '/api/route/admin/info', method: 'get', tip: '路由拦截器校验 admin 权限。' },
    { key: 'public-info', title: '公开接口', desc: '/api/route/public/info', method: 'get', tip: '@SaIgnore 放行，无需登录。' },
    { key: 'res-get', title: 'RESTful GET', desc: '/api/route/res/1', method: 'get', tip: '按 GET 方法校验 res:read 权限。' },
    { key: 'res-post', title: 'RESTful POST', desc: '/api/route/res/1', method: 'post', tip: '按 POST 方法校验 res:write 权限。' },
    { key: 'res-delete', title: 'RESTful DELETE', desc: '/api/route/res/1', method: 'delete', tip: '按 DELETE 方法校验 res:delete 权限。' }
  ],
  'session': [
    { key: 'account-set', title: 'Account-Session 写', desc: '写入账号级 Session', method: 'post', params: [{ name: 'key', value: 'name' }, { name: 'value', value: 'alice' }], tip: 'StpUtil.getSession().set(key, value)。' },
    { key: 'account-get', title: 'Account-Session 读', desc: '读取账号级 Session', method: 'get', params: [{ name: 'key', value: 'name' }], tip: '同一账号多端共享 Account-Session。' },
    { key: 'token-set', title: 'Token-Session 写', desc: '写入 Token 级 Session', method: 'post', params: [{ name: 'key', value: 'traceId' }, { name: 'value', value: 't-123' }], tip: 'StpUtil.getTokenSession().set(key, value)。' },
    { key: 'token-get', title: 'Token-Session 读', desc: '读取 Token 级 Session', method: 'get', params: [{ name: 'key', value: 'traceId' }], tip: 'Token 级 Session 每个 Token 独立。' },
    { key: 'custom-set', title: '自定义 Session 写', desc: '自定义 Session ID 写数据', method: 'post', params: [{ name: 'key', value: 'biz' }, { name: 'value', value: 'data' }], tip: 'StpUtil.getSessionBySessionId("custom:biz")。' },
    { key: 'custom-get', title: '自定义 Session 读', desc: '自定义 Session ID 读数据', method: 'get', params: [{ name: 'key', value: 'biz' }], tip: '用于非登录态的业务缓存。' },
    { key: 'search', title: '会话查询', desc: '查询当前账号所有 Token', method: 'get', tip: 'StpUtil.getTokenValueListByLoginId()。' },
    { key: 'login-device-count', title: '登录设备数', desc: '当前账号在线设备统计', method: 'get', tip: '多端登录场景下统计在线数。' }
  ],
  'manage': [
    { key: 'kickout', title: '按账号踢下线', desc: 'StpUtil.kickout(id)', method: 'post', params: [{ name: 'id', value: '10001' }], tip: '踢下线后原 Token 失效。' },
    { key: 'kickout-by-token', title: '按 Token 踢下线', desc: 'StpUtil.kickoutByTokenValue(token)', method: 'post', params: [{ name: 'tokenValue', value: '' }], tip: '精确踢掉某个设备。' },
    { key: 'logout', title: '强制注销', desc: 'StpUtil.logout(id)', method: 'post', params: [{ name: 'id', value: '10001' }], tip: '清除账号所有登录状态。' },
    { key: 'disable', title: '账号封禁', desc: '封禁 300 秒', method: 'post', params: [{ name: 'id', value: '10001' }], tip: 'StpUtil.disable(id, 300)。' },
    { key: 'disable-category', title: '分类封禁', desc: '按业务分类封禁', method: 'post', params: [{ name: 'id', value: '10001' }, { name: 'seconds', value: '300' }], tip: 'StpUtil.disableLevel(id, category, 1, seconds)。' },
    { key: 'disable-level', title: '阶梯封禁', desc: '处罚次数越多时间越长', method: 'post', params: [{ name: 'id', value: '10001' }, { name: 'level', value: '1' }], tip: '按 level 累加封禁时长。' },
    { key: 'is-disable', title: '查询封禁状态', desc: '查询账号是否被封禁', method: 'get', params: [{ name: 'id', value: '10001' }], tip: 'StpUtil.isDisable(id) / getDisableTime(id)。' }
  ],
  'advanced': [
    { key: 'second-auth', title: '开启二级认证', desc: 'StpUtil.openSafe(service, 300)', method: 'post', params: [{ name: 'service', value: 'pay' }], tip: '支付等敏感操作前二次校验。' },
    { key: 'check-safe', title: '校验二级认证', desc: 'StpUtil.checkSafe(service)', method: 'get', params: [{ name: 'service', value: 'pay' }], tip: '未通过返回 402。' },
    { key: 'switch-to', title: '临时身份切换', desc: 'StpUtil.switchTo(id, {})', method: 'post', params: [{ name: 'id', value: '10002' }], tip: '在 lambda 内以他人身份执行逻辑。' },
    { key: 'mock', title: '模拟他人账号', desc: '读取指定账号 Session', method: 'post', params: [{ name: 'id', value: '10002' }, { name: 'key', value: 'name' }], tip: '运维场景查看他人数据。' },
    { key: 'login-admin', title: 'Admin 体系登录', desc: '多账号体系演示', method: 'post', params: [{ name: 'id', value: 'admin-1' }], tip: '通过自定义 StpLogic 实现 User/Admin 隔离。' },
    { key: 'admin-is-login', title: 'Admin 是否登录', desc: 'AdminStpUtil.isLogin()', method: 'get', tip: '查询 Admin 体系登录态。' },
    { key: 'encrypt', title: '密码加密', desc: 'MD5 / SHA256 / AES', method: 'post', params: [{ name: 'password', value: '123456' }], tip: 'SaSecureUtil 工具方法。' }
  ],
  'global': [
    { key: 'login', title: '登录触发监听', desc: '观察控制台登录事件日志', method: 'post', params: [{ name: 'id', value: '10001' }], tip: 'SaTokenListener.doLogin()。' },
    { key: 'logout', title: '注销触发监听', desc: '观察控制台注销事件日志', method: 'post', tip: 'SaTokenListener.doLogout()。' },
    { key: 'filter-test', title: '全局过滤器测试', desc: '查看响应头 X-Sa-Token-Practice', method: 'get', tip: 'SaServletFilter.setBeforeAuth()。' }
  ],
  'integration': [
    { key: 'dao-type', title: '持久化实现', desc: '查看当前 Sa-Token DAO', method: 'get', tip: '内存模式 / Redis 模式切换说明。' },
    { key: 'header-token', title: '前后端分离 Token', desc: '关闭 Cookie，使用 Header', method: 'post', params: [{ name: 'id', value: '10001' }, { name: 'closeCookie', value: 'true' }], tip: 'SaLoginModel.setIsLastingCookie(false)。' },
    { key: 'token-timeout', title: 'Token 有效期', desc: '查看 tokenTimeout / sessionTimeout', method: 'get', tip: 'StpUtil.getTokenTimeout()。' },
    { key: 'renew', title: '手动续签', desc: 'StpUtil.renewTimeout(7天)', method: 'post', tip: '常用于活跃用户续期。' },
    { key: 'public-info', title: '公开接口', desc: '跨域/过滤器放行测试', method: 'get', tip: '无需登录即可访问。' }
  ],
  'sso': [
    { key: 'do-login', title: 'SSO 服务端登录', desc: '统一认证并颁发令牌', method: 'post', params: [{ name: 'id', value: '10001' }], tip: '真实场景需跨域 Cookie/ticket 配合。' },
    { key: 'is-login', title: '查询 SSO 登录态', desc: '查询服务端全局登录态', method: 'get', params: [{ name: 'ssoToken', value: '' }], tip: 'SSO 服务端会话查询。' },
    { key: 'client1-info', title: '客户端 1 登录态', desc: '子系统 1 校验登录态', method: 'get', params: [{ name: 'client1Token', value: '' }], tip: '模拟子系统 1。' },
    { key: 'client2-info', title: '客户端 2 登录态', desc: '子系统 2 校验登录态', method: 'get', params: [{ name: 'client2Token', value: '' }], tip: '模拟子系统 2。' },
    { key: 'logout', title: 'SSO 单点注销', desc: '一处注销全端下线', method: 'post', params: [{ name: 'ssoToken', value: '' }], tip: '清除服务端与所有客户端登录态。' },
    { key: 'public-modes', title: 'SSO 三种模式', desc: '同域 / 跨域 / 跨 Redis', method: 'get', tip: '不同部署架构的 SSO 方案。' }
  ],
  'oauth2': [
    { key: 'authorize', title: '授权码模式 - 申请 code', desc: '模拟授权端点', method: 'get', params: [{ name: 'id', value: '10001' }, { name: 'scope', value: 'read' }], tip: '登录并返回授权码。' },
    { key: 'token', title: '授权码模式 - 换 token', desc: '用 code 换 access_token', method: 'post', params: [{ name: 'code', value: '' }], tip: 'OAuth2 标准令牌端点。' },
    { key: 'password-token', title: '密码模式', desc: '用户名密码换 token', method: 'post', params: [{ name: 'username', value: '10001' }, { name: 'password', value: '123456' }], tip: '高度信任客户端使用。' },
    { key: 'client-token', title: '客户端凭证模式', desc: 'client_id/client_secret 换 token', method: 'post', params: [{ name: 'clientId', value: 'client-app' }, { name: 'clientSecret', value: 'secret' }], tip: '服务端之间调用。' },
    { key: 'refresh', title: '刷新令牌', desc: 'refresh_token 换新的 access_token', method: 'post', params: [{ name: 'refreshToken', value: '' }], tip: 'access_token 过期后刷新。' },
    { key: 'userinfo', title: '受保护资源', desc: '带 access_token 访问资源', method: 'get', params: [{ name: 'accessToken', value: '' }], tip: '资源服务器校验 token。' },
    { key: 'openid', title: 'openid 模式', desc: '返回 openid 信息', method: 'get', params: [{ name: 'accessToken', value: '' }], tip: 'OAuth2 openid 扩展。' }
  ],
  'jwt': [
    { key: 'generate', title: '生成 JWT', desc: '教学用简化 JWT', method: 'post', params: [{ name: 'loginId', value: '10001' }], tip: 'Header.Payload.Signature 结构。' },
    { key: 'verify', title: '校验 JWT', desc: '校验 HMAC-SHA256 签名', method: 'post', params: [{ name: 'jwt', value: '' }], tip: '签名一致返回 valid=true。' },
    { key: 'temp-token', title: '临时 Token', desc: '60 秒短时效 Token', method: 'post', params: [{ name: 'id', value: '10001' }], tip: '适合邮件验证、支付确认。' },
    { key: 'modes', title: 'JWT 三种模式', desc: 'simple / mixin / stateless', method: 'get', tip: 'Sa-Token JWT 扩展的三种模式说明。' }
  ],
  'signature': [
    { key: 'generate', title: '生成签名', desc: '按规则生成 HMAC-SHA256 签名', method: 'post', params: [{ name: 'userId', value: '10001' }], tip: '参数按 key 排序 + timestamp + secret。' },
    { key: 'verify', title: '校验签名', desc: '校验时间戳与签名', method: 'post', params: [{ name: 'userId', value: '10001' }, { name: 'timestamp', value: '' }, { name: 'signature', value: '' }], tip: 'timestamp 在 300 秒内且签名正确才通过。' }
  ],
  'gateway': [
    { key: 'check', title: '网关登录态校验', desc: '从 Header 取 Token 校验', method: 'get', tip: '网关只校验是否登录，权限下沉到服务。' },
    { key: 'intro', title: '网关鉴权说明', desc: 'Gateway / ShenYu / Zuul', method: 'get', tip: '不同网关集成 Sa-Token 的思路。' }
  ],
  'rpc': [
    { key: 'upstream', title: '上游获取 Token', desc: '准备把 Token 放入 RPC 上下文', method: 'get', tip: 'Dubbo Attachment / gRPC Metadata / Feign Header。' },
    { key: 'downstream', title: '下游恢复登录态', desc: '从 RPC 上下文取 Token 恢复', method: 'post', params: [{ name: 'tokenValue', value: '' }], tip: 'StpUtil.setTokenValue(tokenValue)。' }
  ],
  'quick': [
    { key: 'phone-login', title: '手机号一键登录', desc: '手机号格式正确即登录', method: 'post', params: [{ name: 'phone', value: '13800138000' }], tip: '真实场景对接短信平台。' },
    { key: 'scan-login', title: '扫码登录', desc: '扫码 code 换取登录态', method: 'post', params: [{ name: 'scanCode', value: 'SCAN-OK' }], tip: '真实场景对接微信/企业微信扫码。' },
    { key: 'intro', title: 'Quick 登录说明', desc: '外部认证映射为 Sa-Token 登录态', method: 'get', tip: 'OAuth2 / 短信 / 扫码结果映射。' }
  ]
}

const paramValues = ref({})

function defaultParams(key) {
  const defs = {}
  for (const s of scenarios[activeModule.value]) {
    if (s.key === key) {
      for (const p of (s.params || [])) {
        defs[p.name] = p.value || ''
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
    let url = `/api/${activeModule.value}/${scenario.key}`
    // route 模块 RESTful 路径特殊处理
    if (activeModule.value === 'route' && scenario.key.startsWith('res-')) {
      url = '/api/route/res/1'
    }
    let res
    if (scenario.method === 'get') {
      res = await axios.get(url, { params })
    } else if (scenario.method === 'delete') {
      res = await axios.delete(url, { params })
    } else {
      res = await axios.post(url, null, { params })
    }
    results.value[cacheKey] = JSON.stringify(res.data, null, 2)
    updateCurrentLoginInfo(res.data)
  } catch (err) {
    const data = err.response ? err.response.data : { message: err.message }
    results.value[cacheKey] = '请求异常：\n' + JSON.stringify(data, null, 2)
  } finally {
    loading.value[cacheKey] = false
  }
}

function updateCurrentLoginInfo(data) {
  if (data && data.data) {
    const d = data.data
    if (d.tokenValue) currentToken.value = d.tokenValue
    if (d.loginId) currentLoginId.value = d.loginId
    if (d.adminToken) currentToken.value = d.adminToken
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

function fillToken(field, token) {
  for (const mod of Object.keys(scenarios)) {
    for (const s of scenarios[mod]) {
      const cacheKey = `${mod}-${s.key}`
      if (paramValues.value[cacheKey] && paramValues.value[cacheKey][field] !== undefined) {
        paramValues.value[cacheKey][field] = token
      }
    }
  }
}

setDefaultParams()
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>Sa-Token<br>全功能实践</h1>
      <div class="status-box">
        <div><strong>当前 Token:</strong></div>
        <div class="token">{{ currentToken || '未登录' }}</div>
        <div><strong>当前登录 ID:</strong> {{ currentLoginId || '-' }}</div>
        <button class="btn-small" @click="fillToken('tokenValue', currentToken)">填充 tokenValue</button>
        <button class="btn-small" @click="fillToken('ssoToken', currentToken)">填充 ssoToken</button>
      </div>
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
      <h2 class="module-title">{{ modules.find(m => m.key === activeModule).title }}</h2>
      <p class="module-desc">{{ modules.find(m => m.key === activeModule).desc }}</p>
      <div class="cards">
        <div v-for="s in scenarios[activeModule]" :key="s.key" class="card">
          <h3>{{ s.title }}</h3>
          <p>{{ s.desc }}</p>
          <div v-if="s.params" class="params">
            <label v-for="p in s.params" :key="p.name">
              {{ p.name }}:
              <input v-model="paramValues[`${activeModule}-${s.key}`][p.name]" />
            </label>
          </div>
          <button class="btn" @click="callScenario(s)" :disabled="loading[`${activeModule}-${s.key}`]">
            {{ loading[`${activeModule}-${s.key}`] ? '执行中...' : '运行实验' }}
          </button>
          <div v-if="s.tip" class="tip">💡 {{ s.tip }}</div>
          <pre v-if="results[`${activeModule}-${s.key}`]" class="result">{{ results[`${activeModule}-${s.key}`] }}</pre>
        </div>
      </div>
    </main>
  </div>
</template>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f7fa; }
.app { display: flex; min-height: 100vh; }
.sidebar { width: 260px; background: #1e293b; color: #fff; padding: 20px; position: fixed; height: 100vh; overflow-y: auto; }
.sidebar h1 { font-size: 20px; margin-bottom: 16px; line-height: 1.4; }
.status-box { background: #334155; padding: 12px; border-radius: 8px; margin-bottom: 16px; font-size: 12px; word-break: break-all; }
.status-box .token { color: #38bdf8; margin: 4px 0; }
.btn-small { background: #0ea5e9; color: #fff; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; margin-right: 6px; margin-top: 6px; font-size: 12px; }
.btn-small:hover { background: #0284c7; }
.menu-item { padding: 10px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 6px; font-size: 14px; transition: background .2s; }
.menu-item:hover { background: #334155; }
.menu-item.active { background: #0ea5e9; }
.content { margin-left: 260px; flex: 1; padding: 24px; }
.module-title { margin: 0 0 8px; font-size: 24px; color: #1e293b; }
.module-desc { color: #64748b; margin-bottom: 20px; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 20px; }
.card { background: #fff; border-radius: 12px; padding: 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.card h3 { margin: 0 0 8px; font-size: 16px; color: #0f172a; }
.card p { color: #475569; font-size: 13px; margin-bottom: 12px; }
.params { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
.params label { font-size: 13px; color: #334155; }
.params input { padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 6px; margin-left: 4px; width: 140px; }
.btn { background: #0ea5e9; color: #fff; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn:hover { background: #0284c7; }
.btn:disabled { background: #94a3b8; cursor: not-allowed; }
.tip { margin-top: 10px; font-size: 12px; color: #0369a1; background: #e0f2fe; padding: 8px; border-radius: 6px; }
.result { margin-top: 12px; background: #0f172a; color: #e2e8f0; padding: 12px; border-radius: 8px; font-size: 12px; overflow-x: auto; white-space: pre-wrap; word-break: break-all; }
</style>
