import { useUserStore } from '../stores/user'

/**
 * v-permission 按钮级权限指令：
 * 无权限的按钮直接从 DOM 中移除（不可见即不可点）
 * 用法：<el-button v-permission="'order:delete'">删除</el-button>
 */
export default {
  mounted(el, binding) {
    const store = useUserStore()
    const required = binding.value
    if (!required) {
      return
    }
    if (!store.has(required)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
