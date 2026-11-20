import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { installComponents } from './ui-components'

import './assets/index.css'

const app = createApp(App)
installComponents(app)
app.use(router)
app.mount('#app')
