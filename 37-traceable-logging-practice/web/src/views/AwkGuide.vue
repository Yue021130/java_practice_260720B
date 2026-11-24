<template>
  <div>
    <div class="page-card">
      <h3>Logback 切割策略</h3>
      <ul class="desc">
        <li>按小时切割、按天归目录：<code>logs/2026-10-08/app-2026-10-08-14.log</code>（14 点的小时文件）</li>
        <li><code>error-*.log</code> 只存 ERROR 级别日志，按同样规则切割归档</li>
        <li>控制台输出不脱敏，文件输出脱敏：11 位手机号等敏感数字会被替换成 <code>***********</code></li>
      </ul>
    </div>

    <div class="page-card">
      <h3>命令速查</h3>

      <el-card v-for="cmd in commands" :key="cmd.title" class="cmd-card" shadow="never">
        <template #header>{{ cmd.title }}</template>
        <pre class="cmd">{{ cmd.code }}</pre>
        <p class="muted">{{ cmd.desc }}</p>
      </el-card>
    </div>
  </div>
</template>

<script setup>
const commands = [
  {
    title: '1. 按 traceId 全量检索（跨天跨小时）',
    code: 'grep -r "3fa85f645c8c4a7d9c2e1b0a99887766" logs/',
    desc: '在 logs/ 目录下递归搜索该 traceId，命中 info 日志、error 日志、按天目录、按小时文件。'
  },
  {
    title: '2. 按小时文件定位（缩小范围）',
    code: 'grep "traceId" logs/2026-10-08/app-2026-10-08-14.log',
    desc: '已知问题发生时间时，直接定位到对应小时文件，避免全目录扫描。'
  },
  {
    title: '3. awk 按列提取（第 4 列 = traceId）',
    code: 'awk \'$4 == "[3fa85f645c8c4a7d9c2e1b0a99887766]"\' logs/2026-10-08/app-2026-10-08-14.log',
    desc: '日志列序：1 时间、2 级别、3 [线程名]、4 [traceId]、5 logger、7+ 消息体。awk 精确匹配第 4 列，避免 traceId 出现在消息体里造成误匹配。'
  },
  {
    title: '4. 只看错误日志',
    code: 'cat logs/*/error-*.log',
    desc: 'error-current.log 按小时归档，直接聚合所有天的 ERROR 日志。'
  },
  {
    title: '5. 脱敏验证（文件脱敏、控制台不脱敏）',
    code: 'grep "***********" logs/2026-10-08/app-2026-10-08-14.log | head',
    desc: '日志里打印 11 位手机号（如 13812345678）时，文件里应是 ***********；同一行在控制台输出仍是原始号码。'
  }
]
</script>

<style scoped>
h3 { margin-bottom: 10px; }
.desc { padding-left: 20px; line-height: 2; font-size: 14px; }
.desc code { background: #f4f4f5; padding: 2px 6px; border-radius: 4px; }
.cmd-card { margin-bottom: 12px; }
.cmd-card pre { margin-bottom: 8px; }
.muted { color: #909399; font-size: 13px; line-height: 1.6; }
</style>
