// 单个线程池的指标卡片
export default function PoolCard({ pool, desc }) {
  const {
    poolName,
    corePoolSize,
    maxPoolSize,
    activeCount,
    poolSize,
    queueSize,
    queueCapacity,
    completedTaskCount,
    rejectedCount
  } = pool

  // 队列积压百分比；无界队列（容量极大）时进度条按 0 显示
  const isUnbounded = queueCapacity > 1_000_000
  const queuePercent = isUnbounded || queueCapacity === 0
    ? 0
    : Math.min(100, Math.round((queueSize / queueCapacity) * 100))

  return (
    <div className="pool-card">
      <div className="pool-card-header">
        <h2>{poolName}</h2>
        <span className="pool-config">
          core={corePoolSize} / max={maxPoolSize}
        </span>
      </div>
      {desc && <p className="pool-desc">{desc}</p>}

      <div className="metric-row">
        <span className="metric-label">活跃线程</span>
        <span className="metric-value highlight">{activeCount}</span>
      </div>
      <div className="metric-row">
        <span className="metric-label">池大小</span>
        <span className="metric-value">{poolSize}</span>
      </div>

      <div className="metric-row">
        <span className="metric-label">队列积压</span>
        <span className="metric-value">
          {queueSize}{isUnbounded ? '' : ` / ${queueCapacity}`}
        </span>
      </div>
      <div className="progress-track">
        <div
          className={`progress-fill ${queuePercent > 80 ? 'danger' : ''}`}
          style={{ width: `${queuePercent}%` }}
        />
      </div>

      <div className="metric-row">
        <span className="metric-label">已完成任务</span>
        <span className="metric-value">{completedTaskCount}</span>
      </div>
      <div className="metric-row">
        <span className="metric-label">被拒绝任务</span>
        <span className={`metric-value ${rejectedCount > 0 ? 'rejected' : ''}`}>
          {rejectedCount}
        </span>
      </div>
    </div>
  )
}
