// 相似度请求参数
export interface SimilarityRequest {
  city?: string[];
  position?: string;
  resume: string;
  resumeId?: string; // 用作缓存键的ID，通常是简历ID
}

// 用户相似度结果
export interface UserSimilarity {
  id: number;
  similarity: number;
  jobAnalysisData: {
    positionId: string;
    positionName: string;
    cityName: string;
    salary: string;
    salaryValue: number;
    degree: string;
    degreeValue: number;
    experience: string;
    experienceValue: number;
    companyName: string;
    companySize: string;
    financingStage: string;
    companyUrl: string;
    JobUrl: string;
    address: string;
    employeeBenefits: string[];
    jobRequirements: string[];
  };
}

// 核心需求接口参数
export interface CoreRequirementsParams {
  city?: string;
  position?: string;
}

// 异步响应接口
export interface AsyncResponse<T> {
  status: 'loading' | 'completed' | 'error';
  message: string;
  data?: T;
}
