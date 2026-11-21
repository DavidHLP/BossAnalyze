<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { deleteResumeAsset, resumeAssetAvailable, uploadResumeAsset } from '@/api/resume/assets'

const props = defineProps<{ userId?: number; enabled: boolean; assets: string[]; images: Record<string, string> }>()
const emit = defineEmits<{ uploaded: [name: string]; removed: [name: string]; insert: [name: string] }>()
const open = defineModel<boolean>('open', { required: true })
let insertOnClose: string | null = null

function selectAsset(name: string) {
  insertOnClose = name
  open.value = false
}

function returnToEditor(event: Event) {
  if (insertOnClose === null) return
  event.preventDefault()
  emit('insert', insertOnClose)
  insertOnClose = null
}
const mobile = useMediaQuery('(max-width: 767px)')
const busy = ref('')
const error = ref('')
const deletion = ref<string | null>(null)
const ready = computed(() => props.enabled && props.userId !== undefined)

async function upload(file: File | undefined) {
  if (!ready.value || props.userId === undefined || !file || busy.value) return
  busy.value = 'upload'
  error.value = ''
  try {
    const name = await uploadResumeAsset(props.userId, file)
    emit('uploaded', name)
    if (!(await resumeAssetAvailable(props.userId, name).catch(() => false))) {
      error.value = '文件已上传，暂时无法确认图片可读取。'
    }
  } catch {
    error.value = '图片上传未确认。请选择不超过 50 MB 的非空图片，重试前请核对同名素材。'
  } finally {
    busy.value = ''
  }
}

function choose(event: Event) {
  const input = event.target as HTMLInputElement
  void upload(input.files?.[0])
  input.value = ''
}

async function remove() {
  const name = deletion.value
  if (!ready.value || props.userId === undefined || !name || busy.value) return
  busy.value = 'delete'
  error.value = ''
  try {
    await deleteResumeAsset(props.userId, name)
    emit('removed', name)
    deletion.value = null
  } catch {
    error.value = '删除结果未确认，请核对素材后再操作。'
  } finally {
    busy.value = ''
  }
}
</script>

<template>
  <UiSheet :open="open" @update:open="!busy && (open = $event)">
    <UiSheetContent :side="mobile ? 'bottom' : 'right'" class="max-h-[85dvh] w-full overflow-auto sm:max-w-md" @interact-outside.prevent @escape-key-down="busy && $event.preventDefault()" @close-auto-focus="returnToEditor">
      <UiSheetHeader>
        <UiSheetTitle>图片素材管理</UiSheetTitle>
        <UiSheetDescription>上传图片并插入当前简历的光标位置。</UiSheetDescription>
      </UiSheetHeader>
      <div class="flex flex-col gap-4 px-4 pb-6">
        <UiAlert v-if="!ready">
          <UiAlertDescription>当前环境尚未启用私有图片素材。启用前需验证图片查看和地址接口的服务端所有者校验。</UiAlertDescription>
        </UiAlert>
        <template v-else>
          <UiField class="border border-dashed p-4" @dragover.prevent @drop.prevent="upload($event.dataTransfer?.files[0])">
            <UiFieldLabel for="resume-asset-file">拖放或选择图片</UiFieldLabel>
            <input id="resume-asset-file" type="file" accept="image/*" :disabled="Boolean(busy)" class="w-full text-sm file:mr-3 file:border file:bg-muted file:px-3 file:py-1.5" @change="choose" />
            <p class="text-xs text-muted-foreground">最大 50 MB。同名文件会覆盖原图，SVG 不在页面中预览。</p>
          </UiField>
          <section class="flex flex-col gap-2" aria-label="当前简历的图片素材">
            <h3 class="font-medium">当前简历的图片素材</h3>
            <p v-if="!assets.length" class="text-sm text-muted-foreground">还没有图片素材。</p>
            <ul v-else class="divide-y border">
              <li v-for="asset in assets" :key="asset" class="flex min-w-0 flex-wrap items-center gap-2 p-2">
                <img v-if="images[asset]" :src="images[asset]" alt="" class="size-10 object-cover" />
                <span class="min-w-24 flex-1 break-all text-sm">{{ asset }}</span>
                <UiButton variant="outline" size="sm" :disabled="Boolean(busy)" @click="selectAsset(asset)">插入光标</UiButton>
                <UiButton variant="ghost" size="sm" :disabled="Boolean(busy)" @click="deletion = asset">删除</UiButton>
              </li>
            </ul>
          </section>
        </template>
        <p v-if="busy === 'upload'" role="status" class="text-sm text-muted-foreground">正在上传图片…</p>
        <UiAlert v-if="error && !deletion" variant="destructive" role="alert"><UiAlertDescription>{{ error }}</UiAlertDescription></UiAlert>
      </div>
    </UiSheetContent>
  </UiSheet>
  <UiDialog :open="deletion !== null" @update:open="!$event && !busy && (deletion = null)">
    <UiDialogContent @interact-outside.prevent @escape-key-down="busy && $event.preventDefault()">
      <UiDialogHeader>
        <UiDialogTitle>删除图片</UiDialogTitle>
        <UiDialogDescription>将永久删除「{{ deletion }}」。使用此图片的历史简历也将无法显示它。</UiDialogDescription>
      </UiDialogHeader>
      <UiAlert v-if="error" variant="destructive" role="alert"><UiAlertDescription>{{ error }}</UiAlertDescription></UiAlert>
      <UiDialogFooter>
        <UiButton variant="outline" :disabled="Boolean(busy)" @click="deletion = null">取消</UiButton>
        <UiButton variant="destructive" :disabled="Boolean(busy)" @click="remove">{{ busy ? '正在删除…' : '永久删除图片' }}</UiButton>
      </UiDialogFooter>
    </UiDialogContent>
  </UiDialog>
</template>
