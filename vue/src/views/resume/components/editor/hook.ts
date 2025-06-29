import { onActivated, onDeactivated, ref } from 'vue'

// 左右移动伸缩布局
export function useMoveLayout() {
  const left = ref(550)
  let flag = false

  function move(event: MouseEvent) {
    if (!flag) return
    left.value = event.clientX - 15
  }

  function down() {
    document.body.classList.add('no-select')
    flag = true
  }

  function up() {
    document.body.classList.remove('no-select')
    flag = false
  }

  onActivated(() => {
    window.addEventListener('mouseup', up)
    window.addEventListener('mousemove', move)
  })

  onDeactivated(() => {
    window.removeEventListener('mouseup', up)
    window.removeEventListener('mousemove', move)
  })
  return { left, down, top }
}
