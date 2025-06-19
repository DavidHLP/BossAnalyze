import { reactive, ref } from 'vue'

export function useUpdateInfoModel() {
  const infoModel = ref(false)
  function setInfoModel() {
    infoModel.value = !infoModel.value
  }
  return {
    infoModel,
    setInfoModel,
  }
}

export const userForm = reactive({
  uid: 0,
  nickName: '',
  username: '',
  sex: '',
  professional: '',
  graduation: '',
  school: '',
  avatar: '',
  origin: '',
})

export function useMessage() {
  const messageModal = ref(false),
    tab = ref(0)

  function toggleMessageModal() {
    messageModal.value = !messageModal.value
  }

  function msgTabChange(idx: number) {
    tab.value = idx
  }

  return {
    tab,
    messageModal,
    msgTabChange,
    toggleMessageModal,
  }
}
// 修改密码
export function useUpdatePWDModel() {
  const PWDModel = ref(false)
  function setPWDModel() {
    PWDModel.value = !PWDModel.value
  }
  return {
    PWDModel,
    setPWDModel,
  }
}
