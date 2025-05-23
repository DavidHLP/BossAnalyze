export interface SalaryJob {
  positionName: string;
  jobCount: number;
  minSalary: number;
  maxSalary: number;
  avgSalary: number;
  recommendedCity: string;
  recommendedCitySalary: number;
  recommendedCityJobCount: number;
}

export interface JobData {
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
  industry: string;
  financingStage: string;
  companyUrl: string;
  jobUrl: string;
  address: string;
  employeeBenefits: string[];
  jobRequirements: string[];
  [key: string]: string | number | string[] | number[]; // 添加索引签名以允许使用字符串索引
}

export interface CompanyInfo {
  companyName: string;
  positionName: string;
  cityName: string;
  salary: string;
  degree: string;
  experience: string;
  companySize: string;
  financingStage: string;
  companyUrl?: string;
  jobUrl?: string;
  address?: string;
  employeeBenefits?: string[];
  jobRequirements?: string[];
}
