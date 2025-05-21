// 相似度请求参数
export interface SimilarityRequest {
  city?: string[];
  position?: string;
  resume: string;
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
