<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const activeModule = ref('basic')
const results = ref({})
const loading = ref({})
const modeInfo = ref(null)

const modules = [
  { key: 'basic', title: '01. 基础邮件', desc: '纯文本、多收件人、抄送密送、最近发送记录' },
  { key: 'html', title: '02. 富文本 HTML', desc: 'HTML 内容、表格布局、链接按钮' },
  { key: 'attachment', title: '03. 附件', desc: 'CSV / PNG 附件、附件大小限制' },
  { key: 'inline', title: '04. 内联图片', desc: 'cid 内联图片、内联 vs 外链' },
  { key: 'template', title: '05. Thymeleaf 模板', desc: 'welcome / order 模板渲染' },
  { key: 'async', title: '06. 异步发送', desc: '@Async 后台发送、任务状态、线程池指标' },
  { key: 'retry', title: '07. 失败重试', desc: '指数退避重试、重试策略' },
  { key: 'schedule', title: '08. 定时与批量', desc: '批量、延迟任务、Quartz、@Scheduled 心跳' },
  { key: 'event', title: '11. 事件监听 @EventListener', desc: 'MailSentEvent、统计/日志/异步通知监听器' },
  { key: 'header', title: '09. 邮件头与编码', desc: '自定义头、RFC 2047 主题编码' },
  { key: 'pitfall', title: '10. 常见坑与调优', desc: '乱码、附件名、超时、SPF/DKIM' }
]

const scenarios = {
  basic: [
    { key: 'text', title: '纯文本邮件', desc: '最简单的 MimeMessageHelper.setText(content)', method: 'post', params: [{ name: 'to' }, { name: 'subject' }, { name: 'content' }], tip: 'setFrom / setTo / setSubject / setText 四步构造一封邮件。' },
    { key: 'multiple', title: '多收件人 + 抄送 + 密送', desc: 'setTo / setCc / setBcc，逗号分隔多个地址', method: 'post', params: [{ name: 'to' }, { name: 'cc' }, { name: 'bcc' }, { name: 'subject' }], tip: 'Bcc 收件人互相不可见，适合群发但保护隐私。' },
    { key: 'recent', title: '最近发送记录', desc: '查看内存中最近 50 封邮件的结构', method: 'get', tip: 'simulate 模式也能看到主题/收件人/大小/耗时，方便对比。' },
    { key: 'mode', title: '发送模式与配置', desc: '查看当前 simulate / real 模式与 SMTP 配置', method: 'get', tip: '切到 real 前确认 host/port/授权码；QQ 用 465+SSL，163 用 25+STARTTLS。' }
  ],
  html: [
    { key: 'send', title: '发送 HTML 邮件', desc: 'setText(html, true) 富文本，含表格与按钮', method: 'post', params: [{ name: 'to' }, { name: 'subject' }, { name: 'username' }, { name: 'amount' }], tip: 'HTML 邮件用 table 布局兼容性最好；颜色/字体显式指定。' },
    { key: 'example', title: '示例 HTML', desc: '查看本模块使用的 HTML 源码', method: 'get', tip: '对照源码理解正文结构与内联样式。' }
  ],
  attachment: [
    { key: 'csv', title: 'CSV 文本附件', desc: '内存动态生成 CSV，addAttachment(InputStreamSource)', method: 'post', params: [{ name: 'to' }, { name: 'subject' }, { name: 'rows', type: 'select', options: ['3', '5', '10'] }], tip: '附件不一定要磁盘文件：ByteArrayResource 内存生成即可。' },
    { key: 'image', title: 'PNG 图片附件', desc: '二进制附件，addAttachment(DataSource)', method: 'post', params: [{ name: 'to' }, { name: 'subject' }], tip: 'DataSource 自带 content type，SMTP 层自动 Base64。' },
    { key: 'limitations', title: '附件大小限制', desc: '各邮箱服务商配额说明', method: 'get', tip: '超大文件走对象存储 + 下载链接，不要塞进 MimeMessage。' }
  ],
  inline: [
    { key: 'send', title: '发送内联图片邮件', desc: 'addInline(cid, DataSource) + <img src="cid:xxx">', method: 'post', params: [{ name: 'to' }, { name: 'subject' }], tip: 'cid 名与 src 引用必须完全一致，否则图片不显示。' },
    { key: 'compare', title: '内联 vs 外链图片', desc: '两种做法优劣对比', method: 'get', tip: '小图用内联，大图用外链/附件。' }
  ],
  template: [
    { key: 'welcome', title: '欢迎模板邮件', desc: '渲染 templates/mail/welcome.html', method: 'post', params: [{ name: 'to' }, { name: 'username' }, { name: 'platform' }], tip: '模板与代码分离，改文案不用动 Java。' },
    { key: 'order', title: '订单模板邮件', desc: '渲染 order.html，演示 th:each 列表', method: 'post', params: [{ name: 'to' }, { name: 'customer' }], tip: '商品明细用 th:each 遍历数组渲染。' },
    { key: 'variables', title: '模板变量说明', desc: '两个模板各自使用的变量清单', method: 'get', tip: '生产可把模板入库并支持国际化多语言。' }
  ],
  async: [
    { key: 'send', title: '异步发送邮件', desc: '接口立即返回 taskId，后台线程池执行', method: 'post', params: [{ name: 'to' }, { name: 'subject' }], tip: '邮件是 IO 操作，异步化避免阻塞请求线程。' },
    { key: 'status', title: '查询异步状态', desc: 'PENDING → RUNNING → SENT / FAILED', method: 'get', params: [{ name: 'taskId' }], tip: '先用「异步发送」拿到 taskId 再查询。' },
    { key: 'pool', title: '线程池指标', desc: 'mailExecutor 活跃线程 / 队列 / 完成任务', method: 'get', tip: '有界队列 + CallerRuns 拒绝策略，任务不丢。' }
  ],
  retry: [
    { key: 'send', title: '带重试的发送', desc: 'failTimes 模拟前 N 次失败，退避后成功', method: 'post', params: [{ name: 'to' }, { name: 'failTimes', type: 'select', options: ['0', '1', '2'] }, { name: 'maxRetries', type: 'select', options: ['1', '2', '3'] }, { name: 'backoff', type: 'select', options: ['exponential', 'fixed'] }], tip: '指数退避：500ms → 1s → 2s，给 SMTP 恢复时间。' },
    { key: 'strategy', title: '重试策略说明', desc: '固定 / 指数退避 / 抖动与重试纪律', method: 'get', tip: '必须设最大重试次数，并做监控告警。' }
  ],
  schedule: [
    { key: 'batch', title: '批量发送', desc: '循环发送 count 封并统计成败', method: 'post', params: [{ name: 'to' }, { name: 'subjectPrefix' }, { name: 'count', type: 'select', options: ['3', '5', '10'] }], tip: '逐封 try-catch，一封失败不拖垮整批。' },
    { key: 'register', title: '注册延迟任务', desc: 'delaySeconds 秒后自动发送', method: 'post', params: [{ name: 'to' }, { name: 'subject' }, { name: 'delaySeconds', type: 'select', options: ['0', '3', '10'] }], tip: '用 ScheduledExecutorService 做一次性延迟任务。' },
    { key: 'list', title: '延迟任务列表', desc: '查看已登记的延迟发送任务与状态', method: 'get', tip: '注册后刷新本页查看 PENDING → SENT。' },
    { key: 'heartbeat', title: '@Scheduled 心跳说明', desc: '周期任务开关与真实用法', method: 'get', tip: '把 schedule-demo 设为 true，每分钟发一封心跳邮件。' },
    { key: 'quartz-register', title: 'Quartz 注册 Cron 任务', desc: '用 Cron 表达式注册定时发邮件任务', method: 'post', params: [{ name: 'jobName' }, { name: 'cron' }, { name: 'to' }, { name: 'subject' }], tip: '演示用 0/30 * * * * ?（每 30 秒），观察任务在 simulate 模式下周期性记录邮件。' },
    { key: 'quartz-list', title: 'Quartz 任务列表', desc: '查看触发器状态与下次触发时间', method: 'get', tip: 'PAUSED=暂停，NORMAL=运行中；nextFireTime 为下次触发时间。' },
    { key: 'quartz-pause', title: 'Quartz 暂停任务', desc: '暂停后不再触发但保留定义', method: 'post', params: [{ name: 'jobName' }], tip: '配合 30 秒任务体验暂停/恢复。' },
    { key: 'quartz-resume', title: 'Quartz 恢复任务', desc: '恢复被暂停的任务', method: 'post', params: [{ name: 'jobName' }], tip: '恢复后按原 cron 继续触发。' },
    { key: 'quartz-delete', title: 'Quartz 删除任务', desc: '删除任务及其触发器', method: 'post', params: [{ name: 'jobName' }], tip: '删除后不再出现在任务列表。' },
    { key: 'quartz-explain', title: 'Quartz 概念速记', desc: 'Job / Trigger / Scheduler / Cron 示例 / 与 @Scheduled 对比', method: 'get', tip: '多实例/不丢任务用 Quartz，单机简单场景用 @Scheduled。' }
  ],
  event: [
    { key: 'send', title: '发送触发事件链路', desc: '发送成功发布 MailSentEvent，多个监听器响应', method: 'post', params: [{ name: 'to' }, { name: 'subject' }], tip: '看控制台：日志/统计监听同步响应，站内通知异步响应。' },
    { key: 'publish-demo', title: '手动发布事件', desc: '发布成功/失败事件观察监听器', method: 'post', tip: '失败事件也会被监听，供统计/告警使用。' },
    { key: 'stats', title: '事件统计', desc: '监听器聚合的发送成功/失败统计', method: 'get', tip: '按场景标签统计，生产落 Redis/时序库做监控。' },
    { key: 'listeners', title: '监听器清单', desc: '扫描容器内所有 @EventListener 方法', method: 'get', tip: '看每个监听器的事件类型、是否异步、condition 条件。' },
    { key: 'explain', title: '事件监听知识点', desc: '@EventListener / condition / @Async / @TransactionalEventListener', method: 'get', tip: '事件解耦发送与后续处理，新增处理只需新增监听器。' }
  ],
  header: [
    { key: 'send', title: '发送自定义头邮件', desc: 'X-Priority / Reply-To / X-Mailer', method: 'post', params: [{ name: 'to' }, { name: 'subject' }, { name: 'replyTo' }, { name: 'priority', type: 'select', options: ['1', '3', '5'] }], tip: '中文主题由 JavaMail 自动 RFC 2047 编码。' },
    { key: 'encoding', title: 'RFC 2047 主题编码', desc: '对比自动编码与手动 Base64 编码', method: 'post', params: [{ name: 'subject' }], tip: '邮件头含非 ASCII 必须编码成 =?UTF-8?B?...?= 形式。' },
    { key: 'rules', title: '常用邮件头速查', desc: '各邮件头含义与反垃圾配置', method: 'get', tip: '真实域名要配 SPF/DKIM/DMARC 防进垃圾箱。' }
  ],
  pitfall: [
    { key: 'list', title: '常见坑清单', desc: '10 个高频问题的原因与解决方案', method: 'get', tip: '先看清单再动手，少走弯路。' },
    { key: 'plain-vs-html', title: 'HTML 当纯文本演示', desc: 'setText(content, true/false) 差异', method: 'get', tip: '忘了传 true，标签会原样显示在正文里。' },
    { key: 'tuning', title: '超时与调优参数', desc: 'SMTP 超时 / TLS / 工程化要点', method: 'get', tip: '不设超时，SMTP 不可达时请求会卡死。' }
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
    let res
    if (scenario.method === 'get') {
      res = await axios.get(url, { params })
    } else {
      const payload = scenario.body || null
      res = await axios.post(url, payload, { params })
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

onMounted(async () => {
  try {
    const res = await axios.get('/api/basic/mode')
    modeInfo.value = res.data.data
  } catch (e) {
    modeInfo.value = null
  }
})
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>Spring Boot<br>邮件服务实践</h1>
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
        <span v-if="modeInfo" :class="['mode-banner', modeInfo.simulate ? 'simulate' : 'real']">
          {{ modeInfo.simulate ? '模拟发送' : '真实发送' }} · {{ modeInfo.from }}
        </span>
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
