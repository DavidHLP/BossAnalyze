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
  languageSkills: string;
  professionalSkills: string;
  computerSkills: string;
  certificates: { name: string; date: string; description: string }[];
  interestTags: string[];
  selfEvaluation: string;
  customSkills: { name: string; description: string }[];
}

// 默认简历数据
export const defaultResumeData: ResumeData = {
  name: 'DavidHLP',
  age: '27岁',
  gender: '男',
  location: '上海',
  experience: '4年经验',
  phone: '15688888888',
  email: 'example@qq.com',
  avatar: '',
  jobTarget: '行政专员',
  expectedSalary: '8000/月',
  targetCity: '上海',
  availableTime: '一个月内到岗',
  education: [
    {
      startDate: '2012-09',
      endDate: '2016-07',
      school: '全民简历师范大学',
      major: '工商管理（本科）',
      gpa: 'GPA 3.86/4（专业前5%）',
      courses: '统计分析学、市场研究学、组织行为学、经济法概论、财务会计学、数据学管理、国际行为学、市场营销学、国际贸易理论、国际贸易实务、人力资源开发与管理、财务管理学、企业战略管理概论、资源管理学、资源管理与优化等。'
    }
  ],
  workExperience: [
    {
      startDate: '2018-09',
      endDate: '至今',
      company: '全民简历科技有限公司',
      position: '行政专员',
      duties: [
        '负责公司行政人事管理和日常工作，以及员工培训、假期管理等工作的统筹',
        '公司规章制度，内部办公制度，保证上报下载下高效运行，负责公司文件归档的事项进行归档',
        '部门相关事，负责公司团队状况的制作',
        '公司各人事管理制度，配合各负责人审批。'
      ]
    },
    {
      startDate: '2016-09',
      endDate: '2018-08',
      company: '上海某学网络科技有限公司',
      position: '行政专员',
      duties: [
        '负责中心服务的对接、部门管控',
        '负责公司团队部门各种会议工作，负责引导新人，积极团队工作推广',
        '负责中心行政事务，公司证照管理，负责部门工作下属管理',
        '负责招聘工作，筛选人才和负责人小组面试及测验',
        '管理公司档案工作，人事档案，劳工档案，生日活动及公司档案管理会的活动的执行',
        '负责招聘工作，筛选公司的人力资源配件材料，编写人才聘用及相关标准及资料'
      ]
    }
  ],
  languageSkills: '大学英语6级证书，英语可进行日常交流和简单文档写作',
  professionalSkills: '熟练掌握操作系统原理及网络协议的运行方式,熟练使用',
  computerSkills: '计算机二级证书，熟练运用常见办公软件，如Word、Excel、PowerPoint等',
  interestTags: ['爱阅读', '旅游', '王者荣耀'],
  selfEvaluation: '工作积极认真，细心负责，熟练运用公众自动化软件，善于在工作中提出创意，发现问题，整理流程，有较强的分析能力，易融于学习，接受新知；好学勤力强，认真负责，有良好的团队合作精神；诚实守信，吃苦耐劳，宽以待人。',
  customSkills: [
    {
      name: '项目管理',
      description: '具有多年项目管理经验，熟悉敏捷开发方法论'
    }
  ],
  certificates: [
    {
      name: '英语四级证书',
      date: '2014-06',
      description: '英语听说读写都较好，能够使用英语进行日常交流，熟练通读英文文档和资料'
    },
    {
      name: 'PMP项目管理认证',
      date: '2021-06',
      description: '全球认可的项目管理专业资质认证'
    }
  ]
};
