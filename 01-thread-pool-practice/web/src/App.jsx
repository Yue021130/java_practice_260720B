import { useCallback, useEffect, useState } from 'react'
import { getMetrics } from './api/client'
import PoolCard from './components/PoolCard'
import SubmitPanel from './components/SubmitPanel'
import ResizePanel from './components/ResizePanel'

// 每个池的一句话说明，帮助对照 README 理解
const POOL_DESC = {
  cpuPool: 'CPU 密集型：core = max = CPU 核数',
  ioPool: 'IO 密集型：core = 2×核数，max = 4×核数，大队列',
  customPool: '拒绝演示：core=2 max=4 队列=5，自定义计数拒绝策略'
}

export default function App() {
  const [pools, setPools] = useState([])
  const [error, setError] = useState('')

  // 拉取指标；轮询期间的失败只提示，不清空已有数据
  const refresh = useCallback(async () => {
    try {
      const res = await getMetrics()
      if (res.code === 200) {
        setPools(res.data)
        setError('')
      } else {
        setError(res.message || '获取指标失败')
      }
    } catch (e) {
      setError('无法连接后端，请确认 Spring Boot 已在 8081 端口启动')
    }
  }, [])

  // 每 2 秒轮询一次指标
  useEffect(() => {
    refresh()
    const timer = setInterval(refresh, 2000)
    return () => clearInterval(timer)
  }, [refresh])

  const poolNames = pools.map((p) => p.poolName)

  return (
    <div className="page">
      <header className="header">
        <h1>Java 线程池实践</h1>
        <p className="subtitle">实时监控三个线程池的运行指标，每 2 秒自动刷新</p>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <section className="pool-grid">
        {pools.map((pool) => (
          <PoolCard key={pool.poolName} pool={pool} desc={POOL_DESC[pool.poolName]} />
        ))}
      </section>

      <section className="panel-grid">
        <SubmitPanel poolNames={poolNames} onDone={refresh} />
        <ResizePanel poolNames={poolNames} onDone={refresh} />
      </section>
    </div>
  )
}
