// 简历数据类型定义
export interface Education {
  startDate: string;
  endDate: string;
  school: string;
  major: string;
  gpa: string;
  courses: string;
}

export interface WorkExperience {
  startDate: string;
  endDate: string;
  company: string;
  position: string;
  duties: string[];
}

export interface ResumeData {
  id?: string; // 简历ID，可选字段
  userId?: string; // 用户ID，可选字段
  name: string;
  age: string;
  gender: string;
  location: string[];
  experience: string;
  phone: string;
  email: string;
  avatar: string;
  jobTarget: string;
  expectedSalary: string;
  targetCity: string[];
  availableTime: string;
  education: Education[];
  workExperience: WorkExperience[];
  certificates: { name: string; date: string; description: string }[];
  interestTags: string[];
  selfEvaluation: string;
  customSkills: { name: string; description: string }[];
  sectionOrder?: string[]; // 添加用于组件排序的字段
  createdAt?: Date; // 创建时间
  updatedAt?: Date; // 更新时间
}

/**
 * 简历基础模型 (包含版本控制)
 */
export interface Resume {
  id: string
  userId: number
  title: string
  content: string
  createdAt: Date
  updatedAt: Date
  // 版本控制相关字段
  currentBranch: string
  headCommit?: string
  repositoryId?: string
  hasVersionControl: boolean
  branches: Record<string, string>
  stashStack: Array<Record<string, unknown>>
}

/**
 * 简历创建数据传输对象
 */
export type ResumeCreate = Omit<Resume, 'id' | 'userId' | 'createdAt' | 'updatedAt'>

/**
 * 简历更新数据传输对象
 */
export type ResumeUpdate = Partial<ResumeCreate>
