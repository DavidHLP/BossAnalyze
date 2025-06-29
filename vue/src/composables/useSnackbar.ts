import { ElNotification } from 'element-plus'

interface SnackbarOptions {
  text: string
  color: string
  timeout?: number
  visible: boolean
}

import { ref } from 'vue'
const snackbar = ref<SnackbarOptions>({
  text: '',
  color: 'success',
  timeout: 3000,
  visible: false
})

export function useSnackbar() {
  const showSnackbar = (text: string, color: 'success' | 'info' | 'warning' | 'error' = 'success', timeout: number = 3000) => {
    snackbar.value = {
      text,
      color,
      timeout,
      visible: true
    }

    ElNotification({
      title: '提示',
      message: text,
      type: color,
      duration: timeout,
      position: 'top-right'
    })

    setTimeout(() => {
      hideSnackbar()
    }, timeout)
  }

  const hideSnackbar = () => {
    snackbar.value.visible = false
  }

  return {
    snackbar,
    showSnackbar,
    hideSnackbar
  }
}
