import Mock from 'mockjs'
import type { ResumeData } from './resumeData'
import { defaultResumeData } from './resumeData'

// 使用内存变量存储简历数据
let resumeDataStore: ResumeData = { ...defaultResumeData }

// 模拟获取简历数据的接口
Mock.mock('/api/resume/get', 'get', () => {
  try {
    return {
      code: 200,
      data: resumeDataStore,
      message: '获取简历数据成功'
    }
  } catch (error) {
    console.error('获取简历数据失败', error)
    return {
      code: 200,
      data: defaultResumeData,
      message: '获取简历数据失败，使用默认数据'
    }
  }
})

// 定义接口选项类型
interface MockOptions {
  body: string;
}

// 模拟保存简历数据的接口
Mock.mock('/api/resume/save', 'post', (options: MockOptions) => {
  try {
    const { body } = options
    const data = JSON.parse(body)
    // 更新内存中的数据
    resumeDataStore = { ...data }
    return {
      code: 200,
      data: true,
      message: '保存简历数据成功'
    }
  } catch (error) {
    console.error('保存简历数据失败', error)
    return {
      code: 500,
      data: false,
      message: '保存简历数据失败'
    }
  }
})

// 模拟重置简历数据的接口
Mock.mock('/api/resume/reset', 'post', () => {
  try {
    // 重置为默认数据
    resumeDataStore = { ...defaultResumeData }
    return {
      code: 200,
      data: defaultResumeData,
      message: '重置简历数据成功'
    }
  } catch (error) {
    console.error('重置简历数据失败', error)
    return {
      code: 500,
      data: defaultResumeData,
      message: '重置简历数据失败'
    }
  }
})

// 直接使用MockJS，不通过fetch调用
export const getResumeData = async (): Promise<ResumeData> => {
  return Promise.resolve(resumeDataStore);
}

export const saveResumeData = async (data: ResumeData): Promise<boolean> => {
  try {
    resumeDataStore = { ...data };
    return Promise.resolve(true);
  } catch (error) {
    console.error('保存简历数据失败', error);
    return Promise.reject(error);
  }
}

export const resetResumeData = async (): Promise<ResumeData> => {
  try {
    resumeDataStore = { ...defaultResumeData };
    return Promise.resolve(resumeDataStore);
  } catch (error) {
    console.error('重置简历数据失败', error);
    return Promise.reject(error);
  }
}
