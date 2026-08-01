import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import {
  configureUnauthorizedHandler,
  createAuthenticationFailureHandler,
} from './api/http'
import { useAuthStore } from './features/auth/stores/authStore'
import router from './router'
import { pinia } from './stores'

const app = createApp(App)
app.use(pinia)

const authStore = useAuthStore(pinia)
configureUnauthorizedHandler(
  createAuthenticationFailureHandler({
    clearAuthentication: () => authStore.clearSession(),
    currentPath: () => router.currentRoute.value.path,
    isInitialized: () => authStore.initialized,
    redirectToLogin: () => router.replace('/login'),
  }),
)

app.use(router)
app.use(ElementPlus)

const mountApplication = () => app.mount('#app')
void router.isReady().then(mountApplication, mountApplication)
