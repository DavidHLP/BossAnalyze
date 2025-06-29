import { onActivated, ref } from 'vue'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

export function useFile(emit: any) {
  const fileName = ref('')

  const exportFile = (type: string) => {
    console.log('🔥 header hook: exportFile被调用，类型:', type)

    // 确保文件名不为空
    if (!fileName.value || fileName.value.trim() === '') {
      fileName.value = document.title || '简历导出'
    }

    console.log('🔥 header hook: 使用文件名:', fileName.value)
    document.title = fileName.value
    emit(`download-${type}` as any, fileName.value)
    console.log('🔥 header hook: 发出事件:', `download-${type}`)
  }

  // 导出图片功能 - 本地方案
  const exportPicture = async () => {
    console.log('🔥 开始导出图片...')

    try {
      // 查找要截图的目标元素
      const targetElement = document.querySelector('.markdown-transform-html') as HTMLElement

      if (!targetElement) {
        console.error('🚫 未找到要截图的目标元素')
        return
      }

      console.log('🔥 找到目标元素，开始截图...')

      // 配置html2canvas选项
      const canvas = await html2canvas(targetElement, {
        backgroundColor: '#ffffff', // 设置背景色
        scale: 2, // 提高分辨率
        useCORS: true, // 允许跨域图片
        allowTaint: true, // 允许跨域图片
        imageTimeout: 15000, // 图片加载超时时间
        height: targetElement.scrollHeight, // 使用滚动高度确保完整截图
        width: targetElement.scrollWidth, // 使用滚动宽度
        scrollX: 0,
        scrollY: 0,
        windowWidth: targetElement.scrollWidth,
        windowHeight: targetElement.scrollHeight,
        onclone: (clonedDoc) => {
          // 在克隆的文档中确保样式正确应用
          const clonedElement = clonedDoc.querySelector('.markdown-transform-html') as HTMLElement
          if (clonedElement) {
            clonedElement.style.transform = 'none'
            clonedElement.style.overflow = 'visible'
          }
        }
      })

      console.log('🔥 截图完成，开始生成下载...')

      // 将canvas转换为blob
      canvas.toBlob((blob) => {
        if (!blob) {
          console.error('🚫 生成图片失败')
          return
        }

        // 获取文件名
        const finalFileName = (fileName.value || document.title || '简历导出') + '.png'

        // 创建下载链接
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = finalFileName
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)

        console.log('🔥 图片导出成功:', finalFileName)
      }, 'image/png', 0.95)

    } catch (error) {
      console.error('🚫 导出图片失败:', error)
    }
  }

  // 导出PDF功能 - 本地方案
  const exportPDF = async () => {
    console.log('🔥 开始导出PDF...')

    try {
      // 查找要截图的目标元素
      const targetElement = document.querySelector('.markdown-transform-html') as HTMLElement

      if (!targetElement) {
        console.error('🚫 未找到要截图的目标元素')
        return
      }

      console.log('🔥 找到目标元素，开始截图...')

      // 配置html2canvas选项
      const canvas = await html2canvas(targetElement, {
        backgroundColor: '#ffffff', // 设置背景色
        scale: 2, // 提高分辨率
        useCORS: true, // 允许跨域图片
        allowTaint: true, // 允许跨域图片
        imageTimeout: 15000, // 图片加载超时时间
        height: targetElement.scrollHeight, // 使用滚动高度确保完整截图
        width: targetElement.scrollWidth, // 使用滚动宽度
        scrollX: 0,
        scrollY: 0,
        windowWidth: targetElement.scrollWidth,
        windowHeight: targetElement.scrollHeight,
        onclone: (clonedDoc) => {
          // 在克隆的文档中确保样式正确应用
          const clonedElement = clonedDoc.querySelector('.markdown-transform-html') as HTMLElement
          if (clonedElement) {
            clonedElement.style.transform = 'none'
            clonedElement.style.overflow = 'visible'
          }
        }
      })

      console.log('🔥 截图完成，开始生成PDF...')

      // 获取canvas尺寸
      const imgWidth = canvas.width
      const imgHeight = canvas.height

      // A4纸张尺寸 (以像素为单位，72 DPI)
      const a4Width = 595.28
      const a4Height = 841.89

      // 计算缩放比例以适应A4纸张
      const scaleX = a4Width / imgWidth
      const scaleY = a4Height / imgHeight
      const scale = Math.min(scaleX, scaleY)

      // 计算缩放后的尺寸
      const scaledWidth = imgWidth * scale
      const scaledHeight = imgHeight * scale

      // 计算居中位置
      const offsetX = (a4Width - scaledWidth) / 2
      const offsetY = (a4Height - scaledHeight) / 2

      // 创建PDF实例 (A4纸张)
      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'pt',
        format: 'a4'
      })

      // 将canvas转换为图片数据
      const imgData = canvas.toDataURL('image/png', 1.0)

      // 如果内容高度超过一页，需要分页处理
      if (scaledHeight > a4Height) {
        // 分页处理
        const totalPages = Math.ceil(scaledHeight / a4Height)

        for (let i = 0; i < totalPages; i++) {
          if (i > 0) {
            pdf.addPage()
          }

          const sourceY = (i * a4Height) / scale
          const sourceHeight = Math.min(a4Height / scale, imgHeight - sourceY)
          const targetHeight = sourceHeight * scale

          // 创建临时canvas来裁剪图片
          const tempCanvas = document.createElement('canvas')
          const tempCtx = tempCanvas.getContext('2d')
          tempCanvas.width = imgWidth
          tempCanvas.height = sourceHeight

          if (tempCtx) {
            tempCtx.drawImage(canvas, 0, sourceY, imgWidth, sourceHeight, 0, 0, imgWidth, sourceHeight)
            const pageImgData = tempCanvas.toDataURL('image/png', 1.0)
            pdf.addImage(pageImgData, 'PNG', offsetX, 0, scaledWidth, targetHeight, '', 'FAST')
          }
        }
      } else {
        // 单页处理
        pdf.addImage(imgData, 'PNG', offsetX, offsetY, scaledWidth, scaledHeight, '', 'FAST')
      }

      // 获取文件名
      const finalFileName = (fileName.value || document.title || '简历导出') + '.pdf'

      // 保存PDF文件
      pdf.save(finalFileName)

      console.log('🔥 PDF导出成功:', finalFileName)

    } catch (error) {
      console.error('🚫 导出PDF失败:', error)
    }
  }

  const importFile = (event: Event) => {
    emit('import-md', (event?.target as HTMLInputElement)?.files?.[0])
  }

  onActivated(() => {
    fileName.value = document.title || '简历导出'
  })

  return {
    fileName,
    exportFile,
    exportPicture,
    exportPDF,
    importFile,
  }
}
