<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activeModule = ref('hierarchy')
const results = ref({})
const loading = ref({})

const modules = [
  { key: 'hierarchy', title: '01. 异常体系与分类', desc: 'Throwable 家谱、checked/unchecked、自定义业务异常' },
  { key: 'basics', title: '02. 异常基础语法', desc: 'try-catch-finally、try-with-resources、异常链' },
  { key: 'common', title: '03. 常见异常场景', desc: 'NPE、CCE、NumberFormat、CME、OOM 等高频异常' },
  { key: 'advanced', title: '04. 异常进阶特性', desc: '多 catch、Lambda checked、Stream 异常、Suppressed' },
  { key: 'spring', title: '05. Spring 全局异常处理', desc: '@ControllerAdvice、参数校验、业务错误码' },
  { key: 'concurrency', title: '06. 并发中的异常', desc: '子线程异常、Future、CompletableFuture、线程池' },
  { key: 'bestpractice', title: '07. 最佳实践与反模式', desc: '吞异常、流程控制、fail-fast、日志、事务' }
]

const scenarios = {
  hierarchy: [
    { key: 'family', title: 'Throwable 家谱', desc: '查看 Error / Exception / RuntimeException 层级', method: 'get', tip: '所有异常都继承 Throwable；Exception 分为 checked 和 unchecked（RuntimeException）。' },
    { key: 'checked-unchecked', title: 'Checked vs Unchecked', desc: '演示 checked 必须声明，unchecked 隐式抛出', method: 'post', params: [{ name: 'checked', type: 'select', options: ['true', 'false'] }], tip: 'checked exception 是方法签名的一部分，unchecked 则不是。' },
    { key: 'custom-exception', title: '自定义业务异常', desc: '携带错误码与 cause 的业务异常', method: 'post', params: [{ name: 'throwWithCause', type: 'select', options: ['true', 'false'] }], tip: '自定义异常继承 RuntimeException，可携带错误码并保留 cause。' },
    { key: 'when-to-use', title: '何时用 checked/unchecked', desc: '选型建议', method: 'get', tip: '可恢复场景用 checked；编程错误用 unchecked。Effective Java 推荐优先 unchecked。' }
  ],
  basics: [
    { key: 'execution-order', title: 'try-catch-finally 执行顺序', desc: 'normal / catch / uncaught / return 四种场景', method: 'post', params: [{ name: 'scenario', type: 'select', options: ['normal', 'catch', 'uncaught', 'return'] }], tip: 'finally 在 return/throw 之前执行；finally 中的 return 会覆盖 try 的 return。' },
    { key: 'finally-override', title: 'finally 覆盖返回值或异常', desc: 'finally 中 return/throw 对 catch 的覆盖', method: 'post', params: [{ name: 'withReturn', type: 'select', options: ['true', 'false'] }], tip: 'finally 中的 return 会吞掉 catch 里的 throw，非常危险。' },
    { key: 'try-with-resources', title: 'try-with-resources', desc: 'AutoCloseable 自动关闭与 Suppressed 异常', method: 'post', params: [{ name: 'businessFail', type: 'select', options: ['false', 'true'] }, { name: 'closeFail', type: 'select', options: ['false', 'true'] }], tip: '资源按打开逆序关闭；close 异常被挂到业务异常的 suppressed 上。' },
    { key: 'exception-chain', title: '异常链 cause', desc: '低层异常转业务异常时保留 cause', method: 'post', tip: '跨层捕获异常后抛新异常，务必传入 cause，便于排查根因。' },
    { key: 'mask-sensitive', title: '异常信息脱敏', desc: '内部日志完整，对外脱敏', method: 'post', tip: '对外接口不要返回敏感信息或完整堆栈，内部日志记录完整异常。' },
    { key: 'finally-not-execute', title: 'finally 不执行的极端情况', desc: 'System.exit / JVM 崩溃 / 线程被杀', method: 'get', tip: 'finally 不是绝对保险，关键清理应配合 shutdownHook 等机制。' }
  ],
  common: [
    { key: 'npe', title: 'NPE 防御', desc: '自动拆箱、Optional、equals 反写', method: 'post', tip: '用 Objects.requireNonNull、Optional、常量放前等方式防御 NPE。' },
    { key: 'class-cast', title: 'ClassCastException', desc: '泛型擦除与 instanceof', method: 'post', tip: '泛型只存在于编译期，运行时强转前用 instanceof。' },
    { key: 'number-format', title: 'NumberFormatException', desc: 'parse 与 BigDecimal 字符串构造', method: 'post', tip: 'BigDecimal 推荐用字符串构造，避免 double 精度问题。' },
    { key: 'index-out-of-bounds', title: 'IndexOutOfBoundsException', desc: '数组/List/String 越界', method: 'post', tip: '操作前检查 size/length，使用安全 API。' },
    { key: 'cme', title: 'ConcurrentModificationException', desc: 'fail-fast 与正确删除元素', method: 'post', tip: 'for-each 中删除用 Iterator.remove()、removeIf 或倒序索引。' },
    { key: 'uoe', title: 'UnsupportedOperationException', desc: 'Arrays.asList / singletonList / unmodifiableList', method: 'post', tip: 'Arrays.asList 返回固定大小列表，需要可变时包一层 new ArrayList<>。' },
    { key: 'no-such-element', title: 'NoSuchElementException', desc: 'Optional.get / Iterator.next', method: 'post', tip: 'Optional 推荐 orElse/orElseGet/orElseThrow，不要直接 get。' },
    { key: 'stack-overflow', title: 'StackOverflowError', desc: '递归导致的栈溢出', method: 'post', tip: '检查递归终止条件，循环依赖初始化也会触发。' },
    { key: 'oom', title: 'OutOfMemoryError', desc: '堆/元空间/堆外内存 OOM 原理', method: 'get', tip: 'OOM 是 Error，不要 catch 后假装正常；应通过 -Xmx、dump 分析根因。' },
    { key: 'class-not-found', title: 'ClassNotFound vs NoClassDefFound', desc: '编译期缺失 vs 运行期缺失', method: 'post', tip: 'ClassNotFoundException 是 checked；NoClassDefFoundError 是 Error。' },
    { key: 'assertion', title: 'AssertionError', desc: 'assert 关键字使用注意', method: 'get', tip: 'assert 默认关闭，生产业务校验不要用 assert。' }
  ],
  advanced: [
    { key: 'multi-catch', title: 'Java 7 多 catch', desc: '同时捕获多个不相关异常', method: 'post', tip: '多 catch 中异常不能有继承关系。' },
    { key: 'rethrow', title: '更精确重抛', desc: '编译器推断实际抛出类型', method: 'post', tip: 'Java 7 后 catch(Exception e) { throw e; } 可保持原异常类型。' },
    { key: 'lambda-checked', title: 'Lambda 受检异常处理', desc: '内部 try-catch / 包装 / 自定义函数式接口', method: 'post', tip: '标准函数式接口不支持 checked exception，需要转换。' },
    { key: 'stream-exception', title: 'Stream 异常短路', desc: '中间操作异常终止流水线', method: 'post', tip: 'Stream 中异常会短路；建议在 forEach 内单独捕获或提前过滤。' },
    { key: 'suppressed', title: 'Suppressed Exception', desc: 'try-with-resources 挂起异常', method: 'post', tip: '通过 getSuppressed() 获取被挂起的 close 异常。' },
    { key: 'exception-masking', title: '异常屏蔽', desc: '抛新异常不保留 cause 会丢失原异常', method: 'post', tip: 'throw new RuntimeException("msg", e) 保留 cause。' },
    { key: 'performance', title: '异常创建性能开销', desc: 'fillInStackTrace 是主要开销', method: 'post', tip: '高吞吐场景可重写 fillInStackTrace，但会丢失堆栈。' }
  ],
  spring: [
    { key: 'business-error', title: '业务异常', desc: 'BusinessException → 全局处理器', method: 'post', tip: '@RestControllerAdvice 统一处理业务异常，返回统一错误码。' },
    { key: 'error-code', title: '业务错误码设计', desc: '错误码分段与国际化', method: 'get', tip: '错误码建议按系统+模块+序号分段，映射到国际化 message key。' },
    { key: 'validation', title: '参数校验异常', desc: '@RequestBody 校验失败', method: 'post', body: { username: 'a', email: 'not-email' }, tip: '@Valid + @RequestBody 触发 MethodArgumentNotValidException。' },
    { key: 'response-status', title: 'ResponseStatusException', desc: 'Spring 内置轻量异常', method: 'post', tip: '适合快速返回指定 HTTP 状态码与原因。' },
    { key: 'unknown-error', title: '未知异常兜底', desc: '进入 Exception.class 统一处理', method: 'post', tip: '兜底处理器记录完整日志，对外返回脱敏信息。' }
  ],
  concurrency: [
    { key: 'thread-uncaught', title: '子线程异常不抛主线程', desc: '默认子线程异常独立传播', method: 'post', tip: '主线程 try-catch 捕获不到子线程异常。' },
    { key: 'uncaught-handler', title: 'UncaughtExceptionHandler', desc: '统一捕获子线程未处理异常', method: 'post', tip: '可设置默认 handler 做日志、监控、告警。' },
    { key: 'future-get', title: 'Future.get 异常包装', desc: '任务异常包装为 ExecutionException', method: 'post', tip: '实际异常在 cause 中；InterruptedException 需恢复中断标志。' },
    { key: 'completable-exception', title: 'CompletableFuture 异常处理', desc: 'exceptionally / handle / whenComplete', method: 'post', tip: 'exceptionally 返回默认值；handle 统一处理；whenComplete 不吞异常。' },
    { key: 'async-exception', title: '@Async 异常', desc: 'AsyncUncaughtExceptionHandler 捕获（看控制台）', method: 'post', tip: '@Async 返回值 void 时异常由 handler 处理；有返回值时通过 Future 获取。' },
    { key: 'pool-swallow', title: '线程池 submit 吞异常', desc: 'submit vs execute 差异', method: 'post', tip: 'submit 后务必处理 Future.get()，否则异常静默丢失。' }
  ],
  bestpractice: [
    { key: 'swallow', title: '不要吞异常', desc: '空 catch 让问题无法定位', method: 'post', tip: 'catch 后至少记录日志，最好继续抛或返回错误。' },
    { key: 'flow-control', title: '不要用异常做流程控制', desc: '异常开销大，应用 if/for/return', method: 'post', tip: '异常设计用于异常情况，不是正常分支。' },
    { key: 'fail-fast', title: '早失败 fail-fast', desc: '入参校验前置', method: 'post', tip: '前置校验避免把错误带到下游。' },
    { key: 'logging', title: '异常日志规范', desc: '上下文 + 堆栈，避免重复打印', method: 'post', tip: 'log.error("msg, params", exception) 是标准做法。' },
    { key: 'transaction', title: '事务与异常回滚', desc: 'Spring 默认回滚规则', method: 'post', tip: '默认只回滚 RuntimeException/Error；checked exception 需配置 rollbackFor。' }
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
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>Java 异常体系<br>全场景实践</h1>
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
