<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { listenToSessionEvents } from '@/composables/session-events'
import { useSession } from '@/composables/useSession'
import { useTheme } from '@/composables/useTheme'

useTheme()
const session = useSession()
const router = useRouter()

let cleanup: (() => void) | undefined
onMounted(() => { cleanup = listenToSessionEvents(session, router) })
onUnmounted(() => cleanup?.())
</script>

<template>
  <RouterView />
</template>
