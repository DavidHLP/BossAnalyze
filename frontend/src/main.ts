import { createApp, h, ref } from 'vue'
import App from './App.vue'
import AppError from './components/AppError.vue'
import { router } from './router'
import { installComponents } from './ui-components'

import './assets/index.css'

const failed = ref(false)
function showUnexpectedError() {
  failed.value = true
  document.title = '暂时无法加载页面 · BossAnalyze'
}

const app = createApp({ render: () => h(failed.value ? AppError : App) })
app.config.errorHandler = showUnexpectedError
router.onError(showUnexpectedError)
installComponents(app)
app.use(router)
app.mount('#app')
