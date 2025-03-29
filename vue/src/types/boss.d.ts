export interface SalaryRange {
  range: string;
  count: number;
}

export interface CityDistribution {
  city: string;
  count: number;
}

export interface SkillRequired {
  skill: string;
  count: number;
}

export interface BossAnalyzeData {
  salaryRanges: SalaryRange[];
  cityDistribution: CityDistribution[];
  skillsRequired: SkillRequired[];
  industryDistribution?: IndustryDistribution[];
  educationDistribution?: EducationDistribution[];
}

export interface IndustryDistribution {
  industry: string;
  count: number;
}

export interface EducationDistribution {
  education: string;
  count: number;
}

export interface JobData {
  id?: string;
  position?: string;
  city?: string;
  salaryRange?: string;
  companyName?: string;
  companySize?: string;
  experienceRequirement?: string;
  educationRequirement?: string;
  timePeriod?: string;
  skills?: string[] | string;
  jobDescription?: string;
  "职位名称"?: string;
  "最高薪资K"?: number;
  "最低薪资K"?: number;
  "平均薪资K"?: number;
  "职位数量"?: number;
  "有效时间年月"?: string;
  "城市"?: string;
  "所属行业"?: string;
  "学历_大专"?: number;
  "学历_本科"?: number;
  "学历_初中及以下"?: number;
  "学历_高中"?: number;
  "学历_中专/中技"?: number;
  "学历_学历不限"?: number;
  [key: string]: any;
}

export interface BossQueryParams {
  position: string;
  timePeriod: string;
}
