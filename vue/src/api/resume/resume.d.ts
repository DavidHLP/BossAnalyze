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
  location: string;
  experience: string;
  phone: string;
  email: string;
  avatar: string;
  jobTarget: string;
  expectedSalary: string;
  targetCity: string;
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
