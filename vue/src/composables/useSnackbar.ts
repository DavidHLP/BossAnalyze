import { ref } from 'vue'

interface SnackbarOptions {
  text: string
  color: string
  timeout?: number
  visible: boolean
}

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
