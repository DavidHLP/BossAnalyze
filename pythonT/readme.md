# Python快速测试

## 模块功能说明

### 1. CityNameList.py
**主要作用**：获取所有城市名称列表，并按城市名称字母顺序排序。

**输入**：
```
{
  "数据源": "MySQL数据库boss_data.t_job_detail表",
  "连接参数": {
    "url": "jdbc:mysql://localhost:3306/boss_data",
    "user": "root",
    "password": "Alone117"
  }
}
```

**输出**：
```
{
  "类型": "DataFrame",
  "字段": ["city_name"],
  "示例": [
    {"city_name": "上海"},
    {"city_name": "北京"},
    {"city_name": "广州"},
    ...
  ]
}
```

### 2. BasicSer.py
**主要作用**：统计每个城市的工作数量，并按工作数量降序排序，输出城市名称列表。

**输入**：
```
{
  "数据源": "MySQL数据库boss_data.t_job_detail表",
  "连接参数": {
    "url": "jdbc:mysql://localhost:3306/boss_data",
    "user": "root",
    "password": "Alone117"
  }
}
```

**输出**：
```
{
  "类型": "DataFrame",
  "字段": ["city_name"],
  "排序方式": "按城市工作数量降序",
  "示例": [
    {"city_name": "北京"},
    {"city_name": "上海"},
    {"city_name": "深圳"},
    ...
  ]
}
```

### 3. SalaryHotJob.py
**主要作用**：分析薪水最高的50个职位及其推荐城市，计算最低薪资、最高薪资和平均薪资。

**输入**：
```
{
  "数据源": "MySQL数据库boss_data.t_job_detail表",
  "连接参数": {
    "url": "jdbc:mysql://localhost:3306/boss_data",
    "user": "root",
    "password": "Alone117"
  },
  "解析字段": {
    "position_name": "职位名称",
    "detail_data": "包含薪资信息的JSON字段",
    "city_name": "城市名称"
  }
}
```

**输出**：
```
{
  "类型": "DataFrame",
  "字段": [
    "position_name": "职位名称",
    "job_count": "职位数量",
    "min_salary": "最低薪资(K)",
    "max_salary": "最高薪资(K)",
    "avg_salary": "平均薪资(K)",
    "recommended_city": "推荐城市",
    "recommended_city_salary": "推荐城市平均薪资(K)",
    "recommended_city_job_count": "推荐城市职位数量"
  ],
  "排序方式": "按平均薪资降序",
  "限制": "仅显示前50个职位"
}
```

### 4. Two-dimensional-analysis-chart.py
**主要作用**：将薪资、学历、经验等数据进行转换，用于二维分析图表展示。

**输入**：
```
{
  "数据源": "MySQL数据库boss_data.t_job_detail表",
  "连接参数": {
    "url": "jdbc:mysql://localhost:3306/boss_data",
    "user": "root",
    "password": "Alone117"
  },
  "解析字段": {
    "position_id": "职位ID",
    "position_name": "职位名称",
    "detail_data": "包含详细信息的JSON字段",
    "city_name": "城市名称"
  }
}
```

**输出**：
```
{
  "类型": "DataFrame",
  "字段": [
    "position_id": "职位ID",
    "position_name": "职位名称",
    "city_name": "城市名称",
    "salary": "原始薪资字符串",
    "salary_value": "转换后的薪资数值",
    "degree": "原始学历要求字符串",
    "degree_value": "转换后的学历等级(0-6)",
    "experience": "原始经验要求字符串",
    "experience_value": "转换后的经验年限",
    "companyName": "公司名称",
    "companySize": "公司规模",
    "industry": "所属行业",
    "financingStage": "融资阶段",
    "companyUrl": "公司网址"
  ],
  "学历映射": {
    "不限": 0,
    "初中": 1,
    "高中/中专": 2,
    "大专": 3,
    "本科": 4,
    "硕士": 5,
    "博士": 6
  },
  "经验映射": {
    "应届/无需/不限": 0,
    "1年以内": 1,
    "1-3年": 2,
    "3-5年": 4,
    "5-10年": 7,
    "10年以上": 10
  }
}
```
