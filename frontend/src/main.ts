import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { ui, installComponents } from './ui-components'

import './assets/index.css'

const app = createApp(App)
installComponents(app, ui)
app.use(router)
app.mount('#app')
