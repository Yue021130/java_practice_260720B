import { useState } from 'react'
import { submitTasks } from '../api/client'

// 压测面板：选择池 + 任务数 + 单任务耗时，提交后立刻刷新指标
export default function SubmitPanel({ poolNames, onDone }) {
  const [poolName, setPoolName] = useState('customPool')
  const [count, setCount] = useState(20)
  const [taskDurationMs, setTaskDurationMs] = useState(2000)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    setLoading(true)
    setMessage('')
    try {
      const res = await submitTasks(poolName, count, taskDurationMs)
      if (res.code === 200) {
        const { requested, submitted, rejected } = res.data
        setMessage(`请求 ${requested} 个，成功入池 ${submitted} 个，被拒绝 ${rejected} 个`)
      } else {
        setMessage(`提交失败：${res.message}`)
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
      <h3>提交压测任务</h3>
      <label>
        目标线程池
        <select value={poolName} onChange={(e) => setPoolName(e.target.value)}>
          {poolNames.map((name) => (
            <option key={name} value={name}>{name}</option>
          ))}
          {poolNames.length === 0 && <option value="customPool">customPool</option>}
        </select>
      </label>
      <label>
        任务数量
        <input
          type="number"
          min="1"
          max="1000"
          value={count}
          onChange={(e) => setCount(Number(e.target.value))}
        />
      </label>
      <label>
        单任务耗时 (ms)
        <input
          type="number"
          min="0"
          max="60000"
          step="100"
          value={taskDurationMs}
          onChange={(e) => setTaskDurationMs(Number(e.target.value))}
        />
      </label>
      <button onClick={handleSubmit} disabled={loading}>
        {loading ? '提交中...' : '提交任务'}
      </button>
      {message && <p className="panel-message">{message}</p>}
    </div>
  )
}
