import { useUserStore } from '../stores/user'

/**
 * v-permission 按钮权限指令：
 * 绑定值为权限点字符串或字符串数组，任一满足即显示，
 * 否则在 mounted 时直接从 DOM 中移除元素。
 */
export default {
  mounted(el, binding) {
    const need = binding.value
    if (!need) return
    const codes = Array.isArray(need) ? need : [need]
    const userStore = useUserStore()
    const ok = codes.some((code) => userStore.hasPerm(code))
    if (!ok && el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
}
