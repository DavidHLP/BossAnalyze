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

export interface Resume {
  id: string;
  userId: number;
  title: string;
  content: string;
  // Git-like 字段
  currentBranch: string;
  headCommitId: string;
  branches: Branch[];
  commits: Commit[];
  tags: Tag[];
  createdAt: string; // or Date
  updatedAt: string; // or Date
}

// Git分支
export interface Branch {
  name: string;
  headCommitId: string;
  description: string;
  createdAt: string;
  isDefault: boolean;
  createdBy: string;
}

// Git提交
export interface Commit {
  commitId: string;
  parentCommitIds: string[];
  branch: string;
  title: string;
  content: string;
  commitMessage: string;
  author: string;
  commitTime: string;
  changesSummary: string;
  isMergeCommit: boolean;
  mergedFromBranch?: string;
}

// Git标签
export interface Tag {
  name: string;
  commitId: string;
  message: string;
  createdAt: string;
  createdBy: string;
  tagType: string;
}

// 请求类型
export interface CommitRequest {
  title: string;
  content: string;
  message: string;
  author?: string;
}

export interface BranchRequest {
  name: string;
  description: string;
  fromCommitId?: string;
}

export interface MergeRequest {
  sourceBranch: string;
  targetBranch: string;
  message?: string;
}

export interface TagRequest {
  name: string;
  commitId?: string;
  message: string;
}

// 兼容性类型别名
export type ResumeSnapshot = Commit;
