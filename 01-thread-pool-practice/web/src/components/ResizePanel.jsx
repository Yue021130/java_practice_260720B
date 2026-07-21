import { useState } from 'react'
import { resizePool } from '../api/client'

// 动态调参面板：运行期修改 corePoolSize / maxPoolSize
export default function ResizePanel({ poolNames, onDone }) {
  const [poolName, setPoolName] = useState('ioPool')
  const [corePoolSize, setCorePoolSize] = useState(8)
  const [maxPoolSize, setMaxPoolSize] = useState(16)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const handleResize = async () => {
    setLoading(true)
    setMessage('')
    try {
      const res = await resizePool(poolName, corePoolSize, maxPoolSize)
      if (res.code === 200) {
        setMessage(`已调整 ${poolName}：core=${corePoolSize}，max=${maxPoolSize}`)
      } else {
        setMessage(`调整失败：${res.message}`)
      }
    } catch (e) {
      setMessage('请求出错，请确认后端已启动')
    } finally {
      setLoading(false)
      onDone && onDone()
    }
  }

  return (
    <div className="panel">
      <h3>动态调整参数</h3>
      <label>
        目标线程池
        <select value={poolName} onChange={(e) => setPoolName(e.target.value)}>
          {poolNames.map((name) => (
            <option key={name} value={name}>{name}</option>
          ))}
          {poolNames.length === 0 && <option value="ioPool">ioPool</option>}
        </select>
      </label>
      <label>
        核心线程数 corePoolSize
        <input
          type="number"
          min="1"
          max="500"
          value={corePoolSize}
          onChange={(e) => setCorePoolSize(Number(e.target.value))}
        />
      </label>
      <label>
        最大线程数 maxPoolSize
        <input
          type="number"
          min="1"
          max="500"
          value={maxPoolSize}
          onChange={(e) => setMaxPoolSize(Number(e.target.value))}
        />
      </label>
      <button onClick={handleResize} disabled={loading}>
        {loading ? '调整中...' : '应用调整'}
      </button>
      {message && <p className="panel-message">{message}</p>}
    </div>
  )
}
