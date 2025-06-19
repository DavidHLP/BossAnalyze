import useEditorStore from '@/store/modules/editor'
import { computed, nextTick, onActivated, onDeactivated, ref, watchEffect } from 'vue'
import type { Ref } from 'vue'
import { linkFlag, selectIcon } from './toolbar/hook'
import { clickedTarget } from '../../hook'
import { setClickedLinkText, setClickedLinkURL } from './toolbar/components/linkInput/hook'
import { queryDOM } from '@/utils'

export function reactiveWritable(resumeType: string) {
  const editorStore = useEditorStore()
  editorStore.initMDContent(resumeType)
  const writable = computed(() => editorStore.writable)
  return {
    writable,
  }
}

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

export function injectWritableModeAvatarEvent(
  writable: Ref<boolean>,
  setAvatar: (path: string) => void,
) {
  watchEffect(() => {
    if (!writable.value) return
    nextTick(() => {
      const node = queryDOM('.writable-edit-mode') as HTMLElement
      setTimeout(() => {
        const avatar = node.querySelector('img[alt*="个人头像"]')
        if (avatar) {
          avatar.addEventListener('click', async function () {
            const fileInput = document.createElement('input')
            fileInput.type = 'file'
            fileInput.accept = 'image/png, image/jpeg, image/jpg'
            fileInput.style.display = 'none'
            document.body.appendChild(fileInput)

            fileInput.click()

            fileInput.addEventListener('change', function () {
              const file = fileInput.files?.[0]
              if (file) {
                const reader = new FileReader()
                reader.readAsDataURL(file)
                reader.onload = function (event) {
                  setAvatar(event.target?.result as string)
                  document.body.removeChild(fileInput)
                }
              } else {
                document.body.removeChild(fileInput)
              }
            })
          })
        }

        injectWritableModeClickedReplace(node)
      })
    })
  })
}

export function injectWritableModeClickedReplace(parentNode: HTMLElement) {
  parentNode.addEventListener('click', (event: Event) => {
    const target = event.target as HTMLElement,
      className = target.className,
      tagName = target.tagName.toLocaleLowerCase()
    if (className.includes('iconfont')) {
      selectIcon.value = !selectIcon.value
      clickedTarget.value = target
    } else if (tagName === 'a') {
      linkFlag.value = !linkFlag.value
      setClickedLinkText(target)
      setClickedLinkURL(target)
      clickedTarget.value = target
    }
  })
}
