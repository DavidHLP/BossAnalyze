# 用户分析

## 职位城市推荐

- `JobCityRecommender`

- input
```txt
inputdata = '数据分析师'
limitdata = 10
```

- result
```markdown
| company_name | count |
|--------------|-------|
| 湖州         | 30    |
```

## 职位需求分析

- `JobRequirementAnalysis`

- input
```txt
final_position_name = '数据分析师'
```

- result
```markdown
| position_name | keyword_counts_json      |
|---------------|--------------------------|
| 数据分析师    | [{"数据分析/挖掘经验":11}...] |
```

## 城市职位分析

- `JobCityRecommenderAnalysis`

- input
```txt
final_position_name = '统计员'
company_name = '拉萨'
```

- result
```markdown
| company_name | position_name | keyword_counts_json         |
|--------------|---------------|-----------------------------|
| 拉萨         | 统计员        | [{"餐补":1},{"财务结算经...} |
```