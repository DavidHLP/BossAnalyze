# Mock 数据服务

这个文件夹包含了模拟数据和API服务，用于前端开发和测试。

## 文件结构

- `index.ts` - 导出所有mock服务
- `resumeData.ts` - 简历数据类型定义和默认数据
- `resumeApi.ts` - 简历相关的API服务

## 使用方法

### 导入类型和模拟数据

```typescript
// 导入类型
import type { ResumeData } from '@/mock/resumeData';

// 导入默认数据
import { defaultResumeData } from '@/mock/resumeData';
```

### 使用API服务

```typescript
import { getResumeData, saveResumeData, resetResumeData } from '@/mock/resumeApi';

// 获取简历数据
const loadData = async () => {
  try {
    const data = await getResumeData();
    console.log('简历数据加载成功:', data);
  } catch (error) {
    console.error('简历数据加载失败', error);
  }
};

// 保存简历数据
const saveData = async (data: ResumeData) => {
  try {
    const success = await saveResumeData(data);
    if (success) {
      console.log('保存成功');
    } else {
      console.error('保存失败');
    }
  } catch (error) {
    console.error('保存简历数据失败', error);
  }
};

// 重置简历数据
const resetData = async () => {
  try {
    const data = await resetResumeData();
    console.log('重置成功:', data);
  } catch (error) {
    console.error('重置简历数据失败', error);
  }
};
``` 